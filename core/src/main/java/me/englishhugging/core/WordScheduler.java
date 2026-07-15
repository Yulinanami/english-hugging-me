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
 * 单词调度的核心多线程引擎。
 *
 * <p>这个类负责管理整个背单词的生命周期与定时任务。它在后台常驻一个定时线程池，
 * 根据用户配置（顺序、随机、间隔时间、填空模式等），源源不断地向外部接口发射
 * {@link WordEntry} 或被处理过的填空字符串。
 *
 * <p>内部职责已按状态机拆分：
 * “下一个播放哪个下标”由 {@link PlaybackCursor} 负责，
 * “挖空逐步揭字”由 {@link FillBlankSession} 负责，
 * 本类只保留定时线程、暂停恢复与两段式（锁内运算、锁外派发）的事件分发。
 *
 * <p>所有的状态修改和播放逻辑都使用了 {@code synchronized} 加锁，确保即使 UI 线程频繁修改设置，
 * 引擎内部也不会出现状态撕裂或竞态条件。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 1. 构建环境与监听器
 * WordSchedulerConfig config = WordSchedulerConfig.fromAppSettings(appSettings);
 * WordScheduler scheduler = new WordScheduler(words, config, new Listener() { ... }, null);
 *
 * // 2. 启动引擎
 * scheduler.start();
 *
 * // 3. 用户在 UI 上修改了间隔时间，动态打入引擎
 * scheduler.updateIntervalSeconds(5);
 *
 * // 4. 退出程序前释放线程池
 * scheduler.close();
 * </code></pre>
 */
public final class WordScheduler implements AutoCloseable {

    /**
     * UI 消费者监听接口。
     * 当调度器决定播放某个词汇或填空时，会通过这些回调方法通知宿主（如悬浮窗服务）。
     */
    public interface Listener {
        /**
         * 正常播放一个完整的词汇。
         *
         * @param wordEntry 将被展示的词汇对象
         */
        void onWord(WordEntry wordEntry);

        /**
         * 填空考核模式下，播放一个带有下划线的残缺单词。
         *
         * @param displayWord     当前要显示的字符串（如 "a_p_e"）
         * @param originalEntry   这个词的原本实体（用于提取翻译等后备信息）
         * @param hidePhrases     当前状态是否强制隐藏例句
         * @param hideTranslation 当前状态是否强制隐藏翻译
         */
        void onFillBlankWord(String displayWord, WordEntry originalEntry, boolean hidePhrases, boolean hideTranslation);

        /**
         * 当不开启循环播放且所有词汇都已被播放完毕时触发。
         */
        void onPlaybackFinished();
    }

    /**
     * 持久化进度监听接口。
     * 专门用于通知外部存储系统，引擎的内部计数器和伪随机序列已发生变动，需要落盘保存。
     */
    public interface ProgressListener {
        void onProgress(PlaybackProgress progress);
    }

    // --- 核心依赖与组件 ---

    /** 原始的过滤后词库源数据，一旦初始化不可更改 */
    private final List<WordEntry> words;
    private final Listener listener;
    private final ProgressListener progressListener;
    private final FillBlankGenerator fillBlankGenerator = new FillBlankGenerator();

    /** 播放顺序状态机（顺序/随机/乱序的进度推进全在这里面） */
    private final PlaybackCursor cursor;

    // --- 调度器运行时配置 ---

    private int intervalSeconds;

    // --- 并发与线程控制 ---

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> future;
    private boolean paused;

    // --- 填空考核模式 ---

    private boolean fillBlankMode;
    private int fillBlankIntervalSeconds;
    private boolean fillBlankHidePhrases;
    private boolean fillBlankShowTranslation;

    /** 当前进行中的填空会话；null 表示不在填空阶段 */
    private FillBlankSession fillBlankSession;

    /**
     * 构造并初始化调度器引擎。
     *
     * @param words            原始词库数据，不可为空
     * @param config           包含了所有调度规则的配置参数打包对象
     * @param listener         接受播放事件的回调
     * @param progressListener 接受进度保存事件的回调（可为空）
     */
    public WordScheduler(
            List<WordEntry> words,
            WordSchedulerConfig config,
            Listener listener,
            ProgressListener progressListener
    ) {
        if (words == null || words.isEmpty()) {
            throw new IllegalArgumentException("words 列表绝对不能为 null 或为空");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener 监听器不能为 null，否则无法消费调度结果");
        }

        // 1. 根据前缀过滤出候选词库
        this.words = filterWordsByPrefix(words, config.startingPrefix());

        this.listener = listener;
        this.progressListener = progressListener;

        // 2. 初始化播放游标
        PlaybackMode playbackMode;
        if (config.playbackMode() != null) {
            playbackMode = config.playbackMode();
        } else {
            playbackMode = PlaybackMode.SEQUENTIAL;
        }

        // 只有按前缀播放时才允许不循环；全量播放强制循环
        boolean hasPrefix = config.startingPrefix() != null && !config.startingPrefix().trim().isEmpty();
        boolean loopPlayback = !hasPrefix || config.loopPlayback();

        this.cursor = new PlaybackCursor(this.words.size(), playbackMode, loopPlayback, config.progress(), new Random());

        this.intervalSeconds = Math.max(2, config.intervalSeconds());

        // 3. 初始化填空考核相关参数
        this.fillBlankMode = config.fillBlankMode();
        this.fillBlankIntervalSeconds = Math.max(1, config.fillBlankIntervalSeconds());
        this.fillBlankHidePhrases = config.fillBlankHidePhrases();
        this.fillBlankShowTranslation = config.fillBlankShowTranslation();
    }

    /**
     * 辅助方法：过滤具有指定字母前缀的单词集合。
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
            // 如果一个都没匹配上，则退化使用整个原始词库
            return originalWords;
        } else {
            return filtered;
        }
    }

    /**
     * 启动或重新启动引擎。
     * 这将分配一个新的后台独立线程，并立即派发第一枚单词。
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

        // 发射第一弹，并在内部自循环安排未来的调度
        emitNext();
    }

    /**
     * 暂停调度器。
     * 当前正在展示的单词会一直停留在屏幕上，计时器被挂起。
     */
    public synchronized void pause() {
        if (this.future != null) {
            this.future.cancel(false);
            this.future = null;
        }
        this.paused = true;
    }

    /**
     * 恢复被暂停的调度器。立即发射下一个词以弥补等待。
     */
    public synchronized void resume() {
        if (!this.paused || this.executor == null) {
            return;
        }
        this.paused = false;
        emitNext();
    }

    public synchronized boolean isPaused() {
        return this.paused;
    }

    /**
     * 动态热更新主时间间隔。
     * 新的时间间隔会在下一次【正常单词】更迭时自动生效。
     *
     * @param newIntervalSeconds 间隔（秒）
     */
    public synchronized void updateIntervalSeconds(int newIntervalSeconds) {
        int newInterval = Math.max(2, newIntervalSeconds);
        if (this.intervalSeconds == newInterval) {
            return;
        }
        this.intervalSeconds = newInterval;
    }

    /**
     * 动态热更新填空考核模式。
     * 如果关闭时正在进行填空，引擎会立即强制斩断填空流程并跳转到下一个单词。
     */
    public synchronized void updateFillBlankSettings(boolean enabled, int interval, boolean hidePhrases, boolean showTranslation) {
        this.fillBlankMode = enabled;
        this.fillBlankIntervalSeconds = Math.max(1, interval);
        this.fillBlankHidePhrases = hidePhrases;
        this.fillBlankShowTranslation = showTranslation;

        // 如果用户要求关闭填空模式，而当前正处于某个单词的逐步提示中，则必须阻断并清理
        if (!enabled && this.fillBlankSession != null) {
            this.fillBlankSession = null;

            // 立即砍掉未来的填空延迟任务，马上排期下一个完整单词（0秒延时）
            if (this.future != null) {
                this.future.cancel(false);
            }
            scheduleNext(0);
        }
    }

    /**
     * 彻底停止引擎，销毁底层的定时器线程。
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
     * 辅助方法：向执行器提交未来的某个发射任务。
     *
     * @param delaySeconds 延迟秒数
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
     * 引擎的核心“心脏”脉冲方法。
     * 先在锁内推进状态机（填空会话优先，其次正常取词）并安排下一次心跳，
     * 再在锁外把结果派发给 UI，防止 UI 线程阻塞导致死锁。
     */
    private void emitNext() {
        WordEntry wordToEmit = null;
        String blankedWordToEmit = null;
        WordEntry originalBlankEntry = null;
        boolean isFinished = false;

        // 【阶段 1】：核心状态机运算区（必须加锁）
        synchronized (this) {
            if (this.paused || this.executor == null) {
                return; // 被暂停或被销毁，心脏停跳
            }

            // 填空会话进行中：先推进一帧
            if (this.fillBlankSession != null) {
                String frame = this.fillBlankSession.nextFrame();
                if (frame != null) {
                    blankedWordToEmit = frame;
                    originalBlankEntry = this.fillBlankSession.entry();
                    scheduleNext(this.fillBlankIntervalSeconds);
                } else {
                    // 所有空位揭完，会话结束，同一次心跳内落回正常取词
                    this.fillBlankSession = null;
                }
            }

            // 不在填空会话中（或刚刚结束）：正常取词
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

        // 【阶段 2】：向外派发事件区（必须在锁外执行，防止 UI 线程阻塞导致死锁）
        if (isFinished) {
            this.listener.onPlaybackFinished();
        } else if (blankedWordToEmit != null) {
            // 派发填空单词
            boolean hideTranslation = !this.fillBlankShowTranslation;
            this.listener.onFillBlankWord(blankedWordToEmit, originalBlankEntry, this.fillBlankHidePhrases, hideTranslation);
        } else if (wordToEmit != null) {
            // 派发正常完整单词，并汇报进度
            this.listener.onWord(wordToEmit);
            publishProgress();
        }
    }

    /**
     * 正常取词：向游标要下一个下标，必要时为该词开启填空会话，并安排下一次心跳。
     *
     * @return 取出的下一个实体；如果播放结束则返回 null
     */
    private WordEntry takeNextWord() {
        int position = this.cursor.next();
        if (position == -1) {
            return null;
        }

        WordEntry wordToEmit = this.words.get(position);

        // 如果用户开启了全局的填空考核，且这个单词本身的长度值得被挖空（长度大于1）
        boolean canBeBlanked = this.fillBlankMode && wordToEmit.word() != null && wordToEmit.word().length() > 1;
        if (canBeBlanked) {
            this.fillBlankSession = new FillBlankSession(wordToEmit, this.fillBlankGenerator);
        }

        scheduleNext(this.intervalSeconds);
        return wordToEmit;
    }

    /**
     * 将游标当前的所有计数器打包交由外部落地。
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
