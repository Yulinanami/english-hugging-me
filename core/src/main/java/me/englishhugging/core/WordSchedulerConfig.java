package me.englishhugging.core;

import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.settings.PlaybackMode;

/**
 * 单词调度引擎的不可变配置容器。
 *
 * <p>此类的作用是消除 {@link WordScheduler} 构造函数中泛滥的长参数列表（Parameter Explosion），
 * 将所有运行时调度相关的规则参数打包成一个高内聚的对象。
 * 它是完全不可变的，保证了并发调度时的绝对线程安全。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 从 AppSettings 转换为 Scheduler 可用的配置快照
 * WordSchedulerConfig config = WordSchedulerConfig.fromAppSettings(currentSettings);
 * 
 * // 传入调度器引擎
 * WordScheduler scheduler = new WordScheduler(words, config, listener, progressListener);
 * </code></pre>
 */
public final class WordSchedulerConfig {

    private final int intervalSeconds;
    private final PlaybackMode playbackMode;
    private final int nextWordIndex;
    private final String shuffleOrder;
    private final int shufflePosition;
    private final int randomPlayedCount;
    private final String startingPrefix;
    private final boolean loopPlayback;
    private final boolean fillBlankMode;
    private final int fillBlankIntervalSeconds;
    private final boolean fillBlankHidePhrases;
    private final boolean fillBlankShowTranslation;

    /**
     * 构造完整的调度配置实例。
     * 由于参数众多，在实际业务中通常推荐使用 {@link #fromAppSettings(AppSettings)} 工厂方法构建。
     *
     * @param intervalSeconds          自动播放下一词的间隔时间
     * @param playbackMode             顺序、随机或乱序播放模式
     * @param nextWordIndex            顺序模式下的下一词下标
     * @param shuffleOrder             乱序模式下的序列化状态串
     * @param shufflePosition          乱序模式下的当前消费位点
     * @param randomPlayedCount        完全随机模式下的历史总播放量
     * @param startingPrefix           单词字母过滤前缀
     * @param loopPlayback             播放到底部后是否自动循环
     * @param fillBlankMode            是否开启填空考核模式
     * @param fillBlankIntervalSeconds 填空时每个提示字符的揭示时间间隔
     * @param fillBlankHidePhrases     填空时是否隐藏短语和例句
     * @param fillBlankShowTranslation 填空时是否显示中文释义
     */
    public WordSchedulerConfig(
            int intervalSeconds, 
            PlaybackMode playbackMode, 
            int nextWordIndex,
            String shuffleOrder, 
            int shufflePosition, 
            int randomPlayedCount,
            String startingPrefix, 
            boolean loopPlayback, 
            boolean fillBlankMode,
            int fillBlankIntervalSeconds, 
            boolean fillBlankHidePhrases, 
            boolean fillBlankShowTranslation
    ) {
        this.intervalSeconds = intervalSeconds;
        this.playbackMode = playbackMode;
        this.nextWordIndex = nextWordIndex;
        this.shuffleOrder = shuffleOrder;
        this.shufflePosition = shufflePosition;
        this.randomPlayedCount = randomPlayedCount;
        this.startingPrefix = startingPrefix;
        this.loopPlayback = loopPlayback;
        this.fillBlankMode = fillBlankMode;
        this.fillBlankIntervalSeconds = fillBlankIntervalSeconds;
        this.fillBlankHidePhrases = fillBlankHidePhrases;
        this.fillBlankShowTranslation = fillBlankShowTranslation;
    }

    /**
     * 工厂方法：从全局内存设置对象中直接提取出与调度相关的状态参数并封箱。
     *
     * @param settings 全局应用设置
     * @return 提取并构建好的配置对象
     */
    public static WordSchedulerConfig fromAppSettings(AppSettings settings) {
        return new WordSchedulerConfig(
                settings.getIntervalSeconds(),
                settings.getPlaybackMode(),
                settings.getNextWordIndex(),
                settings.getShuffleOrder(),
                settings.getShufflePosition(),
                settings.getRandomPlayedCount(),
                settings.getStartingPrefix(),
                settings.isLoopPlayback(),
                settings.isFillBlankMode(),
                settings.getFillBlankIntervalSeconds(),
                settings.isFillBlankHidePhrases(),
                settings.isFillBlankShowTranslation()
        );
    }

    public int getIntervalSeconds() { 
        return this.intervalSeconds; 
    }
    
    public PlaybackMode getPlaybackMode() { 
        return this.playbackMode; 
    }
    
    public int getNextWordIndex() { 
        return this.nextWordIndex; 
    }
    
    public String getShuffleOrder() { 
        return this.shuffleOrder; 
    }
    
    public int getShufflePosition() { 
        return this.shufflePosition; 
    }
    
    public int getRandomPlayedCount() { 
        return this.randomPlayedCount; 
    }
    
    public String getStartingPrefix() { 
        return this.startingPrefix; 
    }
    
    public boolean isLoopPlayback() { 
        return this.loopPlayback; 
    }
    
    public boolean isFillBlankMode() { 
        return this.fillBlankMode; 
    }
    
    public int getFillBlankIntervalSeconds() { 
        return this.fillBlankIntervalSeconds; 
    }
    
    public boolean isFillBlankHidePhrases() { 
        return this.fillBlankHidePhrases; 
    }
    
    public boolean isFillBlankShowTranslation() { 
        return this.fillBlankShowTranslation; 
    }
}
