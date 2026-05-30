package me.englishhugging.core;

import me.englishhugging.core.display.FillBlankGenerator;
import me.englishhugging.core.model.WordEntry;
import me.englishhugging.core.settings.PlaybackMode;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
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
        void onProgress(int nextWordIndex, String shuffleOrder, int shufflePosition, int randomPlayedCount);
    }

    // --- 核心依赖与组件 ---
    
    /** 原始的过滤后词库源数据，一旦初始化不可更改 */
    private final List<WordEntry> words;
    private final Listener listener;
    private final ProgressListener progressListener;
    private final Random random = new Random();
    private final FillBlankGenerator fillBlankGenerator = new FillBlankGenerator();
    
    // --- 调度器运行时配置 ---
    
    private PlaybackMode playbackMode;
    private int intervalSeconds;
    private boolean loopPlayback;
    
    // --- 播放进度状态 ---
    
    private int nextWordIndex;
    private List<Integer> shuffleOrder;
    private int shufflePosition;
    private int randomPlayedCount;
    private int sessionPlayedCount = 0;
    
    // --- 并发与线程控制 ---
    
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> future;
    private boolean paused;
    
    // --- 填空考核模式状态机 ---
    
    private boolean fillBlankMode;
    private int fillBlankIntervalSeconds;
    private boolean fillBlankHidePhrases;
    private boolean fillBlankShowTranslation;
    private boolean inFillBlankPhase = false;
    private boolean initialBlankShown = false;
    private WordEntry fillBlankOriginalEntry;
    private String fillBlankCurrentWord;
    private List<Integer> fillBlankRemainingBlanks;

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
        this.words = filterWordsByPrefix(words, config.getStartingPrefix());

        this.listener = listener;
        this.progressListener = progressListener;
        
        // 2. 初始化核心参数
        if (config.getPlaybackMode() != null) {
            this.playbackMode = config.getPlaybackMode();
        } else {
            this.playbackMode = PlaybackMode.SEQUENTIAL;
        }

        initProgressCounters(config);
        
        this.intervalSeconds = Math.max(2, config.getIntervalSeconds());
        
        boolean hasPrefix = config.getStartingPrefix() != null && !config.getStartingPrefix().trim().isEmpty();
        if (hasPrefix) {
            this.loopPlayback = config.isLoopPlayback();
        } else {
            this.loopPlayback = true;
        }

        // 3. 初始化填空考核相关参数
        this.fillBlankMode = config.isFillBlankMode();
        this.fillBlankIntervalSeconds = Math.max(1, config.getFillBlankIntervalSeconds());
        this.fillBlankHidePhrases = config.isFillBlankHidePhrases();
        this.fillBlankShowTranslation = config.isFillBlankShowTranslation();
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
            if (w.getWord().toLowerCase().startsWith(targetPrefix)) {
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
     * 辅助方法：初始化四个核心进度计数器（顺序、乱序等）。
     */
    private void initProgressCounters(WordSchedulerConfig config) {
        // 顺序播放索引
        if (config.getNextWordIndex() < 0 || config.getNextWordIndex() > this.words.size()) {
            this.nextWordIndex = 0;
        } else {
            this.nextWordIndex = config.getNextWordIndex();
            // 如果恰巧保存的进度是最后一个词且开启了循环，自动归零
            if (config.isLoopPlayback() && this.nextWordIndex == this.words.size()) {
                this.nextWordIndex = 0;
            }
        }

        // 乱序播放状态
        this.shuffleOrder = parseShuffleOrder(config.getShuffleOrder(), this.words.size());
        
        int safeShufflePosition = Math.max(0, config.getShufflePosition());
        this.shufflePosition = Math.min(safeShufflePosition, this.words.size());
        
        this.randomPlayedCount = Math.max(0, config.getRandomPlayedCount());
    }

    /**
     * 启动或重新启动引擎。
     * 这将分配一个新的后台独立线程，并立即派发第一枚单词。
     */
    public synchronized void start() {
        stop();
        
        this.paused = false;
        this.inFillBlankPhase = false;
        
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "word-scheduler-worker");
            thread.setDaemon(true);
            return thread;
        });
        
        this.sessionPlayedCount = 0;
        
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
        if (!enabled && this.inFillBlankPhase) {
            this.inFillBlankPhase = false;
            this.fillBlankOriginalEntry = null;
            this.fillBlankCurrentWord = null;
            this.fillBlankRemainingBlanks = null;
            
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
        this.inFillBlankPhase = false;
        
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
     * 这个方法极其复杂，它首先通过状态机（正常模式 vs 填空模式）推导出需要派发的数据，
     * 然后调度下一次心跳，最后在释放锁之后安全地通知外部的 UI。
     *
     * <p>由于复杂度的关系，我将通过提取辅助方法的方式（{@code handleFillBlankPhase} 和 {@code handleNormalPhase}）对其进行解耦。
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

            // 如果当前在填空状态机中，则交给专属方法推进
            if (this.inFillBlankPhase) {
                boolean filledSomething = handleFillBlankPhase();
                // 只要状态机还在活跃返回，就向外输出填空内容
                if (filledSomething) {
                    blankedWordToEmit = this.fillNormalWordIfPossible();
                    originalBlankEntry = this.fillBlankOriginalEntry;
                }
            }

            // 如果不在填空模式，或者填空刚好在这个 tick 结束了，则进入正常的取词循环
            if (!this.inFillBlankPhase && blankedWordToEmit == null) {
                WordEntry newWord = handleNormalPhase();
                
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
     * 内部辅助方法：处理填空模式的心跳推进。
     * 
     * @return true 表示仍在填空进行中，外部应该展示填空字符串；false 表示填空已经彻底结束（被填满了）
     */
    private boolean handleFillBlankPhase() {
        if (!this.initialBlankShown) {
            // 第一步：展示第一次刚被挖出很多洞的样子（此时一个空都没填）
            this.initialBlankShown = true;
            scheduleNext(this.fillBlankIntervalSeconds);
            return true;
        } 
        
        boolean hasBlanksLeft = this.fillBlankRemainingBlanks != null && !this.fillBlankRemainingBlanks.isEmpty();
        if (hasBlanksLeft) {
            // 第二步：慢慢地一个一个把空填补回去
            this.fillBlankCurrentWord = this.fillBlankGenerator.fillOneBlank(
                    this.fillBlankCurrentWord, 
                    this.fillBlankOriginalEntry.getWord(), 
                    this.fillBlankRemainingBlanks
            );
            scheduleNext(this.fillBlankIntervalSeconds);
            return true;
        } 
        
        // 第三步：所有的空位都填完了，重置填空状态机标识，为下一次心跳准备
        this.inFillBlankPhase = false;
        this.fillBlankOriginalEntry = null;
        this.fillBlankCurrentWord = null;
        this.fillBlankRemainingBlanks = null;
        return false;
    }
    
    /**
     * 防止局部变量不可见问题的内部提取物。
     */
    private String fillNormalWordIfPossible() {
        return this.fillBlankCurrentWord;
    }

    /**
     * 内部辅助方法：处理正常取词阶段。
     * 
     * @return 取出的下一个实体；如果结束了则返回 null
     */
    private WordEntry handleNormalPhase() {
        // 前置校验：判断在某些极端模式下是否已经播放完所有的单词而需要宣告结束
        boolean isRandomFinished = (!this.loopPlayback) && (this.playbackMode == PlaybackMode.RANDOM) && (this.sessionPlayedCount >= this.words.size());
        if (isRandomFinished) {
            return null;
        }

        int position = nextPosition();
        if (position == -1) {
            return null; // 根据乱序或顺序算法返回宣告结束
        }

        this.sessionPlayedCount++;
        WordEntry wordToEmit = this.words.get(position);

        // 如果用户开启了全局的填空考核，且这个单词本身的长度值得被挖空（长度大于1）
        boolean canBeBlanked = this.fillBlankMode && wordToEmit.getWord() != null && wordToEmit.getWord().length() > 1;
        if (canBeBlanked) {
            // 组装填空所需要的一切材料，并启动它的专属状态机
            this.fillBlankOriginalEntry = wordToEmit;
            FillBlankGenerator.BlankResult result = this.fillBlankGenerator.generateBlanked(wordToEmit.getWord());
            
            this.fillBlankCurrentWord = result.getBlankedWord();
            this.fillBlankRemainingBlanks = new ArrayList<>(result.getBlankPositions());
            
            this.inFillBlankPhase = true;
            this.initialBlankShown = false;
            
            // 下一次心跳安排在主间隔时间之后
            scheduleNext(this.intervalSeconds);
        } else {
            // 普通单词，不参与填空状态机，仅仅安排正常倒计时
            scheduleNext(this.intervalSeconds);
        }
        
        return wordToEmit;
    }

    /**
     * 算法核心：计算物理数据集合中的哪一个下标应该被下一次取出。
     * 
     * @return 有效的下标，返回 -1 代表没有剩余可用的词汇了
     */
    private synchronized int nextPosition() {
        if (this.playbackMode == PlaybackMode.RANDOM) {
            this.randomPlayedCount++;
            return this.random.nextInt(this.words.size());
        }

        if (this.playbackMode == PlaybackMode.SHUFFLE_NO_REPEAT) {
            // 乱序池为空或尺寸不对则重建
            if (this.shuffleOrder.size() != this.words.size()) {
                this.shuffleOrder = newShuffleOrder(this.words.size());
                this.shufflePosition = 0;
            }
            
            // 当前这批乱序列表消费殆尽了
            if (this.shufflePosition >= this.shuffleOrder.size()) {
                if (!this.loopPlayback) {
                    return -1;
                }
                // 如果允许循环，那么就新洗一副牌，从头抽
                this.shuffleOrder = newShuffleOrder(this.words.size());
                this.shufflePosition = 0;
            }
            
            int targetIndex = this.shuffleOrder.get(this.shufflePosition);
            this.shufflePosition++;
            return targetIndex;
        }

        // 默认的 SEQUENTIAL 顺序模式
        if (!this.loopPlayback && this.nextWordIndex >= this.words.size()) {
            return -1;
        }
        
        int position = Math.floorMod(this.nextWordIndex, this.words.size());
        this.nextWordIndex = position + 1;
        
        if (this.loopPlayback) {
            this.nextWordIndex = Math.floorMod(this.nextWordIndex, this.words.size());
        }
        
        return position;
    }

    /**
     * 将当前的所有计数器打包交由配置引擎落地。
     */
    private void publishProgress() {
        if (this.progressListener != null) {
            String orderString = serializeShuffleOrder(this.shuffleOrder);
            this.progressListener.onProgress(this.nextWordIndex, orderString, this.shufflePosition, this.randomPlayedCount);
        }
    }

    /**
     * 将字符串化的乱序数组重新反序列化为可操作的 List。
     * 进行强容错，只要发现异常的乱数序列，立马丢弃重塑。
     */
    private List<Integer> parseShuffleOrder(String value, int wordCount) {
        if (value == null || value.trim().length() == 0) {
            return newShuffleOrder(wordCount);
        }

        String[] parts = value.split(",");
        if (parts.length != wordCount) {
            return newShuffleOrder(wordCount);
        }

        List<Integer> parsed = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        
        try {
            for (String part : parts) {
                int index = Integer.parseInt(part.trim());
                if (index < 0 || index >= wordCount || !seen.add(index)) {
                    return newShuffleOrder(wordCount);
                }
                parsed.add(index);
            }
        } catch (RuntimeException ignored) {
            return newShuffleOrder(wordCount);
        }
        return parsed;
    }

    /**
     * 生成一副全新的打乱乱序数组。
     */
    private List<Integer> newShuffleOrder(int wordCount) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < wordCount; i++) {
            order.add(i);
        }
        Collections.shuffle(order, this.random);
        return order;
    }

    /**
     * 将打乱的数组序列化为逗号分割的普通字符串。
     */
    private String serializeShuffleOrder(List<Integer> order) {
        StringBuilder builder = new StringBuilder();
        for (Integer index : order) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(index);
        }
        return builder.toString();
    }

    @Override
    public void close() {
        stop();
    }
}
