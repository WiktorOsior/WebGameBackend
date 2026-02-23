package com.webgame.backend.configurations;

import com.webgame.backend.Controllers.WebSocketHandler;
import org.springframework.beans.factory.annotation.Value; // <-- 1. Import this!
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler webSocketHandler;

    @Value("${FRONTEND_URL:http://localhost:5173}")
    private String frontendUrl;

    public WebSocketConfig(WebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/websocket")
                .setAllowedOrigins(frontendUrl);
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();

        container.setMaxTextMessageBufferSize(2048);
        container.setMaxBinaryMessageBufferSize(2048);
        container.setMaxSessionIdleTimeout(900000L);

        return container;
    }
}