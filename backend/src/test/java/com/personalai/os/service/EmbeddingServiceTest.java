package com.personalai.os.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class EmbeddingServiceTest {

    @Autowired
    private EmbeddingService embeddingService;

    @Test
    void testGetEmbedding() {
        String text = "学习Spring Boot开发";
        float[] vector = embeddingService.getEmbedding(text);
        System.out.println("向量维度：" + vector.length);
        assert vector.length > 0 : "向量维度不能为0";
        System.out.println("向量前5个值：" + vector[0] + ", " + vector[1] + ", " + vector[2] + ", " + vector[3] + ", " + vector[4]);
    }

    @Test
    void testSimilarTextCosine() {
        String t1 = "学习Spring Boot";
        String t2 = "学习Spring框架";
        float score = embeddingService.cosineSimilarity(
                embeddingService.getEmbedding(t1),
                embeddingService.getEmbedding(t2)
        );
        System.out.println("相近文本相似度（学习Spring Boot vs 学习Spring框架）：" + score);
        assert score > 0.75 : "相近文本相似度应该大于0.75，实际为" + score;
        System.out.println("isSimilar结果（阈值0.80）：" + embeddingService.isSimilar(t1, t2, 0.80f));
    }

    @Test
    void testUnSimilarTextCosine() {
        String t1 = "学习Spring Boot";
        String t2 = "今天去爬山游玩";
        float score = embeddingService.cosineSimilarity(
                embeddingService.getEmbedding(t1),
                embeddingService.getEmbedding(t2)
        );
        System.out.println("无关文本相似度（学习Spring Boot vs 今天去爬山游玩）：" + score);
        assert score < 0.75 : "无关文本相似度应该小于0.75，实际为" + score;
    }

    @Test
    void testSameText() {
        String t1 = "Java向量数据库实践";
        String t2 = "Java向量数据库实践";
        float score = embeddingService.cosineSimilarity(
                embeddingService.getEmbedding(t1),
                embeddingService.getEmbedding(t2)
        );
        System.out.println("相同文本相似度：" + score);
        assert score > 0.95 : "相同文本相似度应该大于0.95，实际为" + score;
    }

    @Test
    void testResumeOptimizationSimilarity() {
        String t1 = "完成简历优化";
        String t2 = "优化我的简历";
        float score = embeddingService.cosineSimilarity(
                embeddingService.getEmbedding(t1),
                embeddingService.getEmbedding(t2)
        );
        System.out.println("简历优化相似度（完成简历优化 vs 优化我的简历）：" + score);
        assert score > 0.6 : "简历优化相关文本相似度应该大于0.6，实际为" + score;
    }

    @Test
    void testDifferentHobbies() {
        String t1 = "我喜欢打篮球";
        String t2 = "我正在学习Python";
        float score = embeddingService.cosineSimilarity(
                embeddingService.getEmbedding(t1),
                embeddingService.getEmbedding(t2)
        );
        System.out.println("不同爱好相似度（我喜欢打篮球 vs 我正在学习Python）：" + score);
        assert score < 0.8 : "不同爱好相似度应该小于0.8，实际为" + score;
    }

    @Test
    void testNameChangeSimilarity() {
        String t1 = "我叫唐琦";
        String t2 = "我改名叫唐琦";
        float score = embeddingService.cosineSimilarity(
                embeddingService.getEmbedding(t1),
                embeddingService.getEmbedding(t2)
        );
        System.out.println("姓名变更相似度（我叫唐琦 vs 我改名叫唐琦）：" + score);
        assert score > 0.8 : "姓名变更相关文本相似度应该大于0.8，实际为" + score;
    }
}