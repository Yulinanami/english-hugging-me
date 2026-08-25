package me.englishhugging.core.settings;

/**
 * 词库播放进度记录。
 *
 * <p>记录当前词库在顺序、乱序及随机模式下的播放进度。
 *
 * @param nextWordIndex     顺序模式下下一个要播放的单词序号
 * @param shuffleOrder      乱序模式下打乱的单词序号列表（逗号分隔）
 * @param shufflePosition   乱序模式下当前播放到的列表位置
 * @param randomPlayedCount 完全随机模式下已累计播放的单词数量
 */
public record PlaybackProgress(int nextWordIndex, String shuffleOrder, int shufflePosition, int randomPlayedCount) {

    /** 初始空进度常量。 */
    public static final PlaybackProgress EMPTY = new PlaybackProgress(0, "", 0, 0);

    public PlaybackProgress {
        nextWordIndex = Math.max(0, nextWordIndex);
        shuffleOrder = shuffleOrder == null ? "" : shuffleOrder.trim();
        shufflePosition = Math.max(0, shufflePosition);
        randomPlayedCount = Math.max(0, randomPlayedCount);
    }
}
