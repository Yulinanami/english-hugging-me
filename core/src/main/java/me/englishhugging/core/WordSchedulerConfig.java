package me.englishhugging.core;

import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.settings.PlaybackMode;
import me.englishhugging.core.settings.PlaybackProgress;

/**
 * 单词播放运行配置。
 *
 * <p>将所有播放相关的规则参数打包成一个配置对象，传递给 {@link WordScheduler} 使用。
 *
 * @param intervalSeconds          自动播放下一词的间隔时间（秒）
 * @param playbackMode             顺序、随机或乱序播放模式
 * @param progress                 上次保存的播放进度
 * @param startingPrefix           单词字母过滤前缀
 * @param loopPlayback             播放到底部后是否自动循环
 * @param fillBlankMode            是否开启填空复习模式
 * @param fillBlankIntervalSeconds 填空提示恢复间隔（秒）
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
     * 从应用设置中读取与播放相关的参数并生成配置对象。
     *
     * @param settings 全局应用设置
     * @return 生成的播放配置对象
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
