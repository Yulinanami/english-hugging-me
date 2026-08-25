package me.englishhugging.core;

import me.englishhugging.core.display.FillBlankGenerator;
import me.englishhugging.core.model.WordEntry;
import me.englishhugging.core.settings.PlaybackMode;
import me.englishhugging.core.settings.PlaybackProgress;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 单词后台定时播放控制。
 *
 * <p>这个类负责管理背单词的定时播放任务。它在后台常驻一个定时线程，
 * 根据用户配置（顺序、随机、间隔时间、填空模式等），定时向界面回调通知要展示的单词。
 *
 * <p>内部职责已清晰拆分：
 * “下一个播放哪个单词”由 {@link PlaybackCursor} 负责，
 * “挖空逐步揭字”由 {@link FillBlankSession} 负责，
 * 本类负责管理定时线程、暂停与恢复，并在每次切换时通过回调通知界面。
 *
 * <p>所有的状态修改和播放操作都使用了 {@code synchronized} 加锁，确保即使 UI 线程频繁修改设置，
 * 内部也不会出现并发冲突或状态混乱。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 1. 构建配置与回调
 * WordSchedulerConfig config = WordSchedulerConfig.fromAppSettings(appSettings);
 * WordScheduler scheduler = new WordScheduler(words, config, new Listener() { ... }, null);
 *
 * // 2. 启动定时播放
 * scheduler.start();
 *
 * // 3. 动态更新间隔时间
 * scheduler.updateIntervalSeconds(5);
 *
 * // 4. 退出程序前释放资源
 * scheduler.close();
 * </code></pre>
 */
public final class WordScheduler implements AutoCloseable {

    /**
     * 单词播放事件回调接口。
     * 当需要展示单词或推进填空时，会通过这些回调方法通知悬浮窗界面。
     */
    public interface Listener {
        /**
         * 正常播放一个完整的词汇。
         *
         * @param wordEntry 将被展示的词汇对象
         */
        void onWord(WordEntry wordEntry);

        /**
         * 填空复习时的单帧推进通知。
         *
         * @param displayWord     当前帧展示的单词字符串（如 "a_p_e"）
         * @param originalEntry   原始单词条目
         * @param hidePhrases     当前帧是否隐藏例句
         * @param hideTranslation 当前帧是否隐藏释义
         */
        void onFillBlankWord(String displayWord, WordEntry originalEntry, boolean hidePhrases, boolean hideTranslation);

        /**
         * 当不开启循环播放且所有词汇都已被播放完毕时触发。
         */
        void onPlaybackFinished();
    }

    /**
     * 背诵进度更新回调接口。
     * 当背词进度发生变化时通知外部保存到本地。
     */
    public interface ProgressListener {
        /**
         * 收到最新的进度快照。
         *
         * @param progress 最新的进度对象
         */
        void onProgress(PlaybackProgress progress);
    }

    // --- 核心字段 ---

    /** 过滤后的单词列表 */
    private final List<WordEntry> words;
    /** 单词播放事件回调 */
    private final Listener listener;
    /** 进度保存事件回调 */
    private final ProgressListener progressListener;
    /** 单词挖空生成工具 */
    private final FillBlankGenerator fillBlankGenerator = new FillBlankGenerator();

    /** 播放进度计算对象（负责顺序/随机/乱序的位置计算） */
    private final PlaybackCursor cursor;

    // --- 播放运行时配置 ---

    /** 单词切换间隔（秒） */
    private int intervalSeconds;

    // --- 后台定时任务 ---

    /** 后台定时任务线程池 */
    private ScheduledExecutorService executor;
    /** 当前运行中的定时任务 */
    private ScheduledFuture<?> future;
    /** 标记当前是否处于暂停状态 */
    private boolean paused;

    // --- 填空模式设置 ---

    /** 是否开启填空模式 */
    private boolean fillBlankMode;
    /** 填空提示恢复间隔（秒） */
    private int fillBlankIntervalSeconds;
    /** 填空时是否隐藏短语 */
    private boolean fillBlankHidePhrases;
    /** 填空时是否显示中文释义 */
    private boolean fillBlankShowTranslation;

    /** 当前进行中的填空过程；null 表示不在填空阶段 */
    private FillBlankSession fillBlankSession;

    /**
     * 构造并初始化单词播放控制对象。
     *
     * @param words            词库数据列表
     * @param config           播放配置对象
     * @param listener         单词播放事件回调
     * @param progressListener 进度更新回调（可为 null）
     */
    public WordScheduler(
            List<WordEntry> words,
            WordSchedulerConfig config,
            Listener listener,
            ProgressListener progressListener
    ) {
        if (words == null || words.isEmpty()) {
            throw new IllegalArgumentException("words 列表不能为 null 或为空");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener 不能为空");
        }

        // 1. 根据首字母前缀过滤出符合条件的单词
        this.words = filterWordsByPrefix(words, config.startingPrefix());

        this.listener = listener;
        this.progressListener = progressListener;

        // 2. 初始化播放进度计算对象
        PlaybackMode playbackMode;
        if (config.playbackMode() != null) {
            playbackMode = config.playbackMode();
        } else {
            playbackMode = PlaybackMode.SEQUENTIAL;
        }

        // 只有按前缀播放时才允许不循环；播放全部词库时默认循环播放
        boolean hasPrefix = config.startingPrefix() != null && !config.startingPrefix().trim().isEmpty();
        boolean loopPlayback = !hasPrefix || config.loopPlayback();

        this.cursor = new PlaybackCursor(this.words.size(), playbackMode, loopPlayback, config.progress(), new Random());

        this.intervalSeconds = Math.max(2, config.intervalSeconds());

        // 3. 初始化填空相关设置
        this.fillBlankMode = config.fillBlankMode();
        this.fillBlankIntervalSeconds = Math.max(1, config.fillBlankIntervalSeconds());
        this.fillBlankHidePhrases = config.fillBlankHidePhrases();
        this.fillBlankShowTranslation = config.fillBlankShowTranslation();
    }

    /**
     * 根据首字母前缀筛选单词。如果没有任何匹配项，则使用原词库。
     */
    private List<WordEntry> filterWordsByPrefix(List<WordEntry> originalWords, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return originalWords;
        }

        List<WordEntry> filtered = new ArrayList<>();
        String targetPrefix = prefix.toLowerCase();

        for (WordEntry w : originalWords) {
            if (w.word().toLowerCase().startsWith(targetPrefix)) {
                filtered.add(w);
            }
        }

        if (filtered.isEmpty()) {
            return originalWords;
        }
        return filtered;
    }

    /**
     * 启动或重新启动定时播放。
     *
     * <p>创建后台定时线程，并立即播放第一个单词。
     */
    public synchronized void start() {
        stop();

        this.paused = false;
        this.fillBlankSession = null;

        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "word-scheduler-worker");
            thread.setDaemon(true);
            return thread;
        });

        this.cursor.resetSession();

        // 立即触发首次单词播放
        emitNext();
    }

    /**
     * 暂停播放。
     *
     * <p>当前正在展示的单词会一直停留在屏幕上，后台播放暂停。
     */
    public synchronized void pause() {
        if (this.future != null) {
            this.future.cancel(false);
            this.future = null;
        }
        this.paused = true;
    }

    /**
     * 恢复被暂停的播放。
     */
    public synchronized void resume() {
        if (!this.paused || this.executor == null) {
            return;
        }
        this.paused = false;
        emitNext();
    }

    /**
     * 查询当前是否处于暂停状态。
     *
     * @return 如果处于暂停状态返回 true
     */
    public synchronized boolean isPaused() {
        return this.paused;
    }

    /**
     * 动态更新单词切换间隔时间。
     *
     * <p>新的时间间隔会在下一次切换单词时生效。
     *
     * @param newIntervalSeconds 新的间隔时间（秒）
     */
    public synchronized void updateIntervalSeconds(int newIntervalSeconds) {
        int newInterval = Math.max(2, newIntervalSeconds);
        if (this.intervalSeconds == newInterval) {
            return;
        }
        this.intervalSeconds = newInterval;
    }

    /**
     * 动态更新填空测试设置。
     *
     * @param enabled         是否开启填空模式
     * @param interval        填空字母揭开间隔（秒）
     * @param hidePhrases     填空时是否隐藏短语例句
     * @param showTranslation 填空时是否显示中文释义
     */
    public synchronized void updateFillBlankSettings(boolean enabled, int interval, boolean hidePhrases, boolean showTranslation) {
        this.fillBlankMode = enabled;
        this.fillBlankIntervalSeconds = Math.max(1, interval);
        this.fillBlankHidePhrases = hidePhrases;
        this.fillBlankShowTranslation = showTranslation;

        // 如果关闭了填空模式且当前正在填空中，则直接切换到下一个完整单词
        if (!enabled && this.fillBlankSession != null) {
            this.fillBlankSession = null;

            if (this.future != null) {
                this.future.cancel(false);
            }
            scheduleNext(0);
        }
    }

    /**
     * 停止播放并释放后台定时任务线程池。
     */
    public synchronized void stop() {
        this.paused = false;
        this.fillBlankSession = null;

        if (this.future != null) {
            this.future.cancel(true);
            this.future = null;
        }

        if (this.executor != null) {
            this.executor.shutdownNow();
            this.executor = null;
        }
    }

    /**
     * 安排下一次单词切换或填空提示任务。
     *
     * @param delaySeconds 延迟执行的秒数
     */
    private synchronized void scheduleNext(long delaySeconds) {
        if (this.paused || this.executor == null) {
            return;
        }

        if (this.future != null) {
            this.future.cancel(false);
        }

        this.future = this.executor.schedule(this::emitNext, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * 执行单词切换与填空提示。
     * 优先推进正在进行的填空进度；填空完成后取出下一个单词，并通知界面更新。
     */
    private void emitNext() {
        WordEntry wordToEmit = null;
        String blankedWordToEmit = null;
        WordEntry originalBlankEntry = null;
        boolean isFinished = false;

        // 1. 状态判断与取词（加锁确保线程安全）
        synchronized (this) {
            if (this.paused || this.executor == null) {
                return; // 被暂停或已停止，直接返回
            }

            // 填空复习进行中：先推进一步
            if (this.fillBlankSession != null) {
                String frame = this.fillBlankSession.nextFrame();
                if (frame != null) {
                    blankedWordToEmit = frame;
                    originalBlankEntry = this.fillBlankSession.entry();
                    scheduleNext(this.fillBlankIntervalSeconds);
                } else {
                    // 所有空位揭完，填空结束，继续取下一个正常单词
                    this.fillBlankSession = null;
                }
            }

            // 不在填空复习中（或刚刚结束）：正常取词
            if (this.fillBlankSession == null && blankedWordToEmit == null) {
                WordEntry newWord = takeNextWord();

                if (newWord == null) {
                    // 没有可播放的单词了（例如不循环播放且到头了）
                    isFinished = true;
                } else {
                    wordToEmit = newWord;
                }
            }

            if (isFinished) {
                stop();
            }
        }

        // 2. 触发回调通知界面（锁外执行，防止界面响应慢导致互相等待卡死）
        if (isFinished) {
            this.listener.onPlaybackFinished();
        } else if (blankedWordToEmit != null) {
            // 通知界面展示填空单词
            boolean hideTranslation = !this.fillBlankShowTranslation;
            this.listener.onFillBlankWord(blankedWordToEmit, originalBlankEntry, this.fillBlankHidePhrases, hideTranslation);
        } else if (wordToEmit != null) {
            // 通知界面展示正常完整单词，并汇报进度
            this.listener.onWord(wordToEmit);
            publishProgress();
        }
    }

    /**
     * 获取下一个要播放的单词，并根据设置安排下一次播放任务。
     *
     * @return 下一个单词条目；如果本轮播放结束则返回 null
     */
    private WordEntry takeNextWord() {
        int position = this.cursor.next();
        if (position == -1) {
            return null;
        }

        WordEntry wordToEmit = this.words.get(position);

        // 如果开启了填空模式且单词长度大于 1，则开始填空复习
        boolean canBeBlanked = this.fillBlankMode && wordToEmit.word() != null && wordToEmit.word().length() > 1;
        if (canBeBlanked) {
            this.fillBlankSession = new FillBlankSession(wordToEmit, this.fillBlankGenerator);
        }

        scheduleNext(this.intervalSeconds);
        return wordToEmit;
    }

    /**
     * 将当前背诵进度通知外部回调保存。
     */
    private void publishProgress() {
        if (this.progressListener != null) {
            this.progressListener.onProgress(this.cursor.snapshot());
        }
    }

    @Override
    public void close() {
        stop();
    }
}
