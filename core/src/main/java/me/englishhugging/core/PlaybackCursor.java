package me.englishhugging.core;

import me.englishhugging.core.settings.PlaybackMode;
import me.englishhugging.core.settings.PlaybackProgress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;

/**
 * 单词播放顺序与进度计算。
 *
 * <p>负责计算在顺序播放、完全随机或乱序不重复模式下，下一个应该播放词库里的哪个单词。
 * 本类不包含任何后台定时或多线程操作，仅负责单词播放顺序与位置计算。
 */
final class PlaybackCursor {

    /** 词库中的单词总数 */
    private final int wordCount;
    /** 当前生效的播放模式（顺序/随机/乱序） */
    private final PlaybackMode playbackMode;
    /** 是否循环播放 */
    private final boolean loopPlayback;
    /** 随机数对象 */
    private final Random random;

    /** 顺序播放模式下下一个单词的位置序号 */
    private int nextWordIndex;
    /** 乱序不重复模式下打乱的单词序号列表 */
    private List<Integer> shuffleOrder;
    /** 乱序列表对应的逗号分隔文本，只在生成或读取乱序列表时更新 */
    private String serializedShuffleOrder;
    /** 乱序不重复模式下当前播放到列表的第几个位置 */
    private int shufflePosition;
    /** 完全随机模式下已累计播放的单词次数 */
    private int randomPlayedCount;
    /** 本轮播放会话中已播放的单词数 */
    private int sessionPlayedCount = 0;

    /**
     * 初始化单词播放进度。
     *
     * @param wordCount    单词总数
     * @param playbackMode 播放模式
     * @param loopPlayback 是否循环播放
     * @param initial      历史进度数据
     * @param random       随机数对象
     */
    PlaybackCursor(int wordCount, PlaybackMode playbackMode, boolean loopPlayback, PlaybackProgress initial, Random random) {
        this.wordCount = wordCount;
        this.playbackMode = playbackMode;
        this.loopPlayback = loopPlayback;
        this.random = random;

        // 顺序播放位置：超出范围的历史进度重置为 0
        if (initial.nextWordIndex() < 0 || initial.nextWordIndex() > wordCount) {
            this.nextWordIndex = 0;
        } else {
            this.nextWordIndex = initial.nextWordIndex();
            // 如果恰巧保存的进度是最后一个词且开启了循环，自动归零
            if (loopPlayback && this.nextWordIndex == wordCount) {
                this.nextWordIndex = 0;
            }
        }

        // 只有乱序不重复模式需要创建和持有完整乱序列表。
        if (this.playbackMode == PlaybackMode.SHUFFLE_NO_REPEAT) {
            setShuffleOrder(parseShuffleOrder(initial.shuffleOrder(), wordCount));
            this.shufflePosition = Math.min(
                    Math.max(0, initial.shufflePosition()),
                    wordCount
            );
        } else {
            this.shuffleOrder = Collections.emptyList();
            this.serializedShuffleOrder = "";
            this.shufflePosition = 0;
        }
        this.randomPlayedCount = Math.max(0, initial.randomPlayedCount());
    }

    /**
     * 每次 {@code start()} 重新开始一轮会话时清零本轮播放计数。
     * 历史进度（顺序位置、乱序位置等）不受影响。
     */
    void resetSession() {
        this.sessionPlayedCount = 0;
    }

    /**
     * 取出下一个应该播放的单词位置序号并推进内部计数。
     *
     * @return 有效单词序号；-1 代表本轮已无可播放的词（不循环时播完）
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
     * 获取当前播放进度快照。
     */
    PlaybackProgress snapshot() {
        return new PlaybackProgress(
                this.nextWordIndex,
                this.serializedShuffleOrder,
                this.shufflePosition,
                this.randomPlayedCount
        );
    }

    /**
     * 计算下一个应该播放的单词位置序号。
     */
    private int nextPosition() {
        // 按播放模式分别计算下一个单词位置
        return switch (this.playbackMode) {
            case RANDOM -> {
                this.randomPlayedCount++;
                yield this.random.nextInt(this.wordCount);
            }

            case SHUFFLE_NO_REPEAT -> {
                // 乱序列表为空或长度不匹配时重新打乱生成
                if (this.shuffleOrder.size() != this.wordCount) {
                    setShuffleOrder(newShuffleOrder(this.wordCount));
                    this.shufflePosition = 0;
                }

                // 乱序列表已全部播放完毕
                if (this.shufflePosition >= this.shuffleOrder.size()) {
                    if (!this.loopPlayback) {
                        yield -1;
                    }
                    // 如果开启循环播放，重新生成乱序列表并从头开始
                    setShuffleOrder(newShuffleOrder(this.wordCount));
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

                int position = this.nextWordIndex;
                this.nextWordIndex = position + 1;

                if (this.loopPlayback) {
                    this.nextWordIndex = Math.floorMod(this.nextWordIndex, this.wordCount);
                }

                yield position;
            }
        };
    }

    /**
     * 将逗号分隔的数字字符串解析为整数列表。
     * 如果字符串为空或内容格式错误，则重新生成一组打乱的序号列表。
     */
    private List<Integer> parseShuffleOrder(String value, int wordCount) {
        if (value == null || value.trim().isEmpty()) {
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
     * 生成一组打乱顺序的单词序号列表。
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
     * 同时更新乱序列表和保存播放进度时使用的逗号分隔文本。
     */
    private void setShuffleOrder(List<Integer> order) {
        this.shuffleOrder = order;
        this.serializedShuffleOrder = serializeShuffleOrder(order);
    }

    /**
     * 将乱序列表拼接为逗号分隔的字符串，便于保存到本地配置。
     */
    private String serializeShuffleOrder(List<Integer> order) {
        StringJoiner joiner = new StringJoiner(",");
        for (Integer index : order) {
            joiner.add(String.valueOf(index));
        }
        return joiner.toString();
    }
}
