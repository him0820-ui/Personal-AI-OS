package com.personalai.os.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

/**
 * @description: Jackson配置类，配置JSON序列化和反序列化策略
 * @author: 琦
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return objectMapper;
    }

    @Bean
    public RestClient.Builder restClientBuilder(ObjectMapper objectMapper) {
        return RestClient.builder()
                .messageConverters(converters -> {
                    converters.removeIf(c -> c.getClass().getName().contains("Jackson"));
                    converters.add(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper));
                });
    }
}
