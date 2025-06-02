package com.example.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * RAG Service Spring Boot Application
 * 
 * 基于Spring AI、Ollama和Elasticsearch的RAG服务应用
 * 
 * @author RAG Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.example.rag.repository")
@EnableElasticsearchRepositories(basePackages = "com.example.rag.repository")
@EnableAsync
public class RagServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagServiceApplication.class, args);
    }
} 