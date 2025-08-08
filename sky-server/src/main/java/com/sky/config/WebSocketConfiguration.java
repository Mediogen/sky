package com.sky.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket配置类
 * 该类用于
 */

@Configuration
@Slf4j
public class WebSocketConfiguration {
    @Bean
    public ServerEndpointExporter serverEndpointExporter(){
        log.info("开始注册WebSocket服务端点...");
        // ServerEndpointExporter会自动注册使用@ServerEndpoint注解声明的WebSocket端点。
        // 需要注意的是，ServerEndpointExporter只在Spring Boot应用中起作用
        // 如果是传统的Spring应用，需要手动注册WebSocket端点。
        return new ServerEndpointExporter();
    }
}

