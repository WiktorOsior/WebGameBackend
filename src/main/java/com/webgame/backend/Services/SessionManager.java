package com.webgame.backend.Services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webgame.backend.Dtos.Message;
import com.webgame.backend.databases.Game;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {
    private final ConcurrentHashMap<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void addSession(String username, WebSocketSession session) {
        userSessions.put(username, session);
    }

    public void removeSession(String username) {
        userSessions.remove(username);
    }

    public String getUsernameBySession(WebSocketSession session) {
        for (Map.Entry<String, WebSocketSession> entry : userSessions.entrySet()) {
            if (entry.getValue().equals(session)) return entry.getKey();
        }
        return null;
    }

    public void broadcastToGame(Game game, Message msg) {
        try {
            TextMessage tm = new TextMessage(objectMapper.writeValueAsString(msg));
            for (String user : game.getConnectedUsers()) {
                WebSocketSession s = userSessions.get(user);
                if (s != null && s.isOpen()) s.sendMessage(tm);
            }
        } catch (Exception e) {
            System.err.println("Broadcast Error: " + e.getMessage());
        }
    }

    public void sendToUser(String username, Message msg) {
        try {
            WebSocketSession s = userSessions.get(username);
            if (s != null && s.isOpen()) {
                s.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
            }
        } catch (Exception e) {
            System.err.println("Send Error: " + e.getMessage());
        }
    }
}