package com.cmyhyq;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;

@SpringBootTest
public class VectorStoreTest {

    @Autowired
    private VectorStore vectorStore;

    @Test
    void testInsertAndSearch() {
        // 插入一条测试文档
        Document doc = new Document("龙门石窟的开放时间是每天8:00-18:00");
        vectorStore.add(List.of(doc));

        // 相似度检索
        List<Document> results = vectorStore.similaritySearch("龙门石窟几点开门");
        System.out.println("检索结果：" + results.get(0).getText());
    }
}