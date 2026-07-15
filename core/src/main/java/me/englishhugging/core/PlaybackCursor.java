package me.englishhugging.core;

import me.englishhugging.core.settings.PlaybackMode;
import me.englishhugging.core.settings.PlaybackProgress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 播放进度游标：封装顺序 / 完全随机 / 乱序不重复三种模式下
 * “下一个播放哪个下标”的全部算法与计数状态。
 *
 * <p>职责边界：只做纯状态推进，不含线程、定时与回调逻辑，
 * 因此可以脱离定时器直接进行毫秒级单元测试。
 *
 * <p>非线程安全——由 {@link WordScheduler} 在自身锁内调用。
 */
final class PlaybackCursor {

    private final int wordCount;
    private final PlaybackMode playbackMode;
    private final boolean loopPlayback;
    private final Random random;

    private int nextWordIndex;
    private List<Integer> shuffleOrder;
    private int shufflePosition;
    private int randomPlayedCount;
    private int sessionPlayedCount = 0;

    /**
     * @param wordCount    词库（过滤后）的总词数
     * @param playbackMode 播放模式，不可为 null
     * @param loopPlayback 生效的循环开关（无前缀播放时上游会强制为 true）
     * @param initial      上次会话遗留的进度快照
     * @param random       随机源
     */
    PlaybackCursor(int wordCount, PlaybackMode playbackMode, boolean loopPlayback, PlaybackProgress initial, Random random) {
        this.wordCount = wordCount;
        this.playbackMode = playbackMode;
        this.loopPlayback = loopPlayback;
        this.random = random;

        // 顺序播放索引：越界的历史进度作废归零
        if (initial.nextWordIndex() < 0 || initial.nextWordIndex() > wordCount) {
            this.nextWordIndex = 0;
        } else {
            this.nextWordIndex = initial.nextWordIndex();
            // 如果恰巧保存的进度是最后一个词且开启了循环，自动归零
            if (loopPlayback && this.nextWordIndex == wordCount) {
                this.nextWordIndex = 0;
            }
        }

        // 乱序播放状态
        this.shuffleOrder = parseShuffleOrder(initial.shuffleOrder(), wordCount);
        this.shufflePosition = Math.min(Math.max(0, initial.shufflePosition()), wordCount);
        this.randomPlayedCount = Math.max(0, initial.randomPlayedCount());
    }

    /**
     * 每次 {@code start()} 重新开始一轮会话时清零本轮播放计数。
     * 历史进度（顺序索引、乱序位置等）不受影响。
     */
    void resetSession() {
        this.sessionPlayedCount = 0;
    }

    /**
     * 取出下一个应该播放的词库下标并推进内部计数。
     *
     * @return 有效下标；-1 代表本轮已无可播放的词（不循环时播完）
     */
    int next() {
        // 完全随机模式没有天然的“播完”概念，用本轮会话计数判定
        boolean isRandomFinished = !this.loopPlayback
                && this.playbackMode == PlaybackMode.RANDOM
                && this.sessionPlayedCount >= this.wordCount;
        if (isRandomFinished) {
            return -1;
        }

        int position = nextPosition();
        if (position != -1) {
            this.sessionPlayedCount++;
        }
        return position;
    }

    /**
     * 把当前所有计数器打包成可持久化的进度快照。
     */
    PlaybackProgress snapshot() {
        return new PlaybackProgress(
                this.nextWordIndex,
                serializeShuffleOrder(this.shuffleOrder),
                this.shufflePosition,
                this.randomPlayedCount
        );
    }

    /**
     * 算法核心：计算物理数据集合中的哪一个下标应该被下一次取出。
     */
    private int nextPosition() {
        // 穷尽匹配 PlaybackMode，勿加 default：新增播放模式时漏写分支应直接编译失败
        return switch (this.playbackMode) {
            case RANDOM -> {
                this.randomPlayedCount++;
                yield this.random.nextInt(this.wordCount);
            }

            case SHUFFLE_NO_REPEAT -> {
                // 乱序池为空或尺寸不对则重建
                if (this.shuffleOrder.size() != this.wordCount) {
                    this.shuffleOrder = newShuffleOrder(this.wordCount);
                    this.shufflePosition = 0;
                }

                // 当前这批乱序列表消费殆尽了
                if (this.shufflePosition >= this.shuffleOrder.size()) {
                    if (!this.loopPlayback) {
                        yield -1;
                    }
                    // 如果允许循环，那么就新洗一副牌，从头抽
                    this.shuffleOrder = newShuffleOrder(this.wordCount);
                    this.shufflePosition = 0;
                }

                int targetIndex = this.shuffleOrder.get(this.shufflePosition);
                this.shufflePosition++;
                yield targetIndex;
            }

            case SEQUENTIAL -> {
                if (!this.loopPlayback && this.nextWordIndex >= this.wordCount) {
                    yield -1;
                }

                int position = Math.floorMod(this.nextWordIndex, this.wordCount);
                this.nextWordIndex = position + 1;

                if (this.loopPlayback) {
                    this.nextWordIndex = Math.floorMod(this.nextWordIndex, this.wordCount);
                }

                yield position;
            }
        };
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
}
