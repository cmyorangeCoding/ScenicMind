package com.cmyhyq.ai.rag;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentSplitter {

    // 每个文本块最大长度（字符数）
    private static final int CHUNK_SIZE = 500;

    // 块之间重叠长度（保证上下文不丢失）
    private static final int OVERLAP = 100;

    /**
     * 智能语义切片（推荐 RAG 使用）
     * 优点：不会把句子从中间切断，语义完整，检索效果更好
     */
    public List<String> splitSmart(String text) {
        List<String> chunks = new ArrayList<>();

        // 1. 空值安全处理
        if (text == null || text.isBlank()) {
            return chunks;
        }

        // 2. 文本清洗：去掉多余空格、换行
        text = text.trim().replaceAll("\\s+", " ");

        // 3. 按句子分割（中文+英文句号、感叹号、问号、分号）
        // (?<=...) 是正则“零宽断言”，表示：在这些符号后面切割，但保留符号本身
        String[] sentences = text.split("(?<=[。！？；.!?])");

        // 4. 开始拼接句子，直到达到 CHUNK_SIZE
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            // 如果加入当前句子会超过最大长度，就先保存当前块
            if (currentChunk.length() + sentence.length() > CHUNK_SIZE) {
                if (!currentChunk.isEmpty()) {
                    // 把当前块加入结果
                    chunks.add(currentChunk.toString().trim());

                    // 重叠逻辑：保留最后 100 字符作为下一块的开头
                    int keepStart = Math.max(0, currentChunk.length() - OVERLAP);
                    String overlapPart = currentChunk.substring(keepStart);

                    // 重置，只保留重叠部分
                    currentChunk = new StringBuilder(overlapPart);
                }
            }

            // 把句子加入当前块
            currentChunk.append(sentence);
        }

        // 5. 把最后一段不足长度的内容加进去
        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * 固定长度切片
     * 缺点：可能会把一句话从中间切断
     */
    public List<String> split(String text) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        text = text.trim().replaceAll("\\s+", " ");
        int length = text.length();
        int step = CHUNK_SIZE - OVERLAP;

        if (length <= CHUNK_SIZE) {
            chunks.add(text);
            return chunks;
        }

        int start = 0;
        while (start < length) {
            int end = Math.min(start + CHUNK_SIZE, length);
            chunks.add(text.substring(start, end));

            if (end == length) break;

            start += step;

            // 最后一段不足时，对齐到末尾
            if (start + CHUNK_SIZE > length) {
                start = length - CHUNK_SIZE;
            }
        }

        return chunks;
    }
}