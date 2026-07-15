package me.englishhugging.core.settings;

/**
 * 单个词库的播放进度快照。
 *
 * <p>把顺序索引、乱序序列与消费位置、随机播放计数四项状态收敛为一个不可变值对象，
 * 供 {@code WordScheduler} 回调、{@link AppSettings} 缓存与 {@link SettingsMapper} 持久化共用，
 * 避免同一组字段在回调签名和读写代码中反复展开。
 *
 * <p>紧凑构造器负责归一化：负数计数归零、null 序列串归空并去首尾空白。
 *
 * @param nextWordIndex     顺序模式下，下一个被播放的单词下标
 * @param shuffleOrder      乱序模式下的伪随机序列（逗号分隔的下标串）
 * @param shufflePosition   乱序模式下当前消费到的位置
 * @param randomPlayedCount 完全随机模式下累计播放的单词数量
 */
public record PlaybackProgress(int nextWordIndex, String shuffleOrder, int shufflePosition, int randomPlayedCount) {

    /** 全新词库的零进度。 */
    public static final PlaybackProgress EMPTY = new PlaybackProgress(0, "", 0, 0);

    public PlaybackProgress {
        nextWordIndex = Math.max(0, nextWordIndex);
        shuffleOrder = shuffleOrder == null ? "" : shuffleOrder.trim();
        shufflePosition = Math.max(0, shufflePosition);
        randomPlayedCount = Math.max(0, randomPlayedCount);
    }
}
