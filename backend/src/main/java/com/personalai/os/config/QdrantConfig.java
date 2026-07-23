package com.personalai.os.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description: Qdrant向量数据库配置类，配置向量存储相关参数
 * @author: 琦
 */
@Configuration
public class QdrantConfig {

    @Value("${spring.ai.vectorstore.qdrant.host:localhost}")
    private String qdrantHost;

    @Value("${spring.ai.vectorstore.qdrant.port:6334}")
    private int qdrantPort;

    @Value("${spring.ai.embedding.ollama.options.model:nomic-embed-text}")
    private String embeddingModel;

    @Bean
    public Integer vectorSize() {
        return 768;
    }
}
