package com.example.scaffoldmcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ScaffoldMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScaffoldMcpApplication.class, args);
    }

    @Bean
    ToolCallbackProvider scaffoldToolCallbacks(ScaffoldTools scaffoldTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(scaffoldTools)
                .build();
    }
}
