package me.englishhugging.core;

import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.settings.PlaybackMode;
import me.englishhugging.core.settings.PlaybackProgress;

/**
 * 单词调度引擎的不可变配置容器。
 *
 * <p>此 record 的作用是消除 {@link WordScheduler} 构造函数中泛滥的长参数列表（Parameter Explosion），
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
 *
 * @param intervalSeconds          自动播放下一词的间隔时间
 * @param playbackMode             顺序、随机或乱序播放模式
 * @param progress                 上次会话遗留的播放进度快照
 * @param startingPrefix           单词字母过滤前缀
 * @param loopPlayback             播放到底部后是否自动循环
 * @param fillBlankMode            是否开启填空考核模式
 * @param fillBlankIntervalSeconds 填空时每个提示字符的揭示时间间隔
 * @param fillBlankHidePhrases     填空时是否隐藏短语和例句
 * @param fillBlankShowTranslation 填空时是否显示中文释义
 */
public record WordSchedulerConfig(
        int intervalSeconds,
        PlaybackMode playbackMode,
        PlaybackProgress progress,
        String startingPrefix,
        boolean loopPlayback,
        boolean fillBlankMode,
        int fillBlankIntervalSeconds,
        boolean fillBlankHidePhrases,
        boolean fillBlankShowTranslation
) {

    public WordSchedulerConfig {
        if (progress == null) {
            progress = PlaybackProgress.EMPTY;
        }
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
                settings.getPlaybackProgress(),
                settings.getStartingPrefix(),
                settings.isLoopPlayback(),
                settings.isFillBlankMode(),
                settings.getFillBlankIntervalSeconds(),
                settings.isFillBlankHidePhrases(),
                settings.isFillBlankShowTranslation()
        );
    }
}
