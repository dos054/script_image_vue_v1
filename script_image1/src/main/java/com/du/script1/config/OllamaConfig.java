package com.du.script1.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
public class OllamaConfig {

    @Value("${ollama.host}")
    private String ollamaHost;

    @Bean
    public WebClient ollamaWebClient() {
        log.info("Ollama WebClient 초기화: {}", ollamaHost);
        return WebClient.builder()
            .baseUrl(ollamaHost)
            .build();
    }
}
