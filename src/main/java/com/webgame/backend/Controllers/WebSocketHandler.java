package com.webgame.backend.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webgame.backend.Dtos.Message;
import com.webgame.backend.Services.GameService;
import com.webgame.backend.Services.SessionManager;
import com.webgame.backend.Repositories.RegisterInterface;
import com.webgame.backend.configurations.JwtUtil;
import com.webgame.backend.databases.BetInfo;
import com.webgame.backend.databases.Game;
import com.webgame.backend.databases.User;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GameService gameService;
    private final SessionManager sessionManager;
    private final RegisterInterface userRepository;

    public WebSocketHandler(GameService gameService, SessionManager sessionManager, RegisterInterface userRepository) {
        this.gameService = gameService;
        this.sessionManager = sessionManager;
        this.userRepository = userRepository;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        spamTracker.remove(session.getId());
        String username = sessionManager.getUsernameBySession(session);
        if (username != null) {
            sessionManager.removeSession(username);
            gameService.removeUserFromAllGames(username);
        }
    }
    private final ConcurrentHashMap<String, Long> spamTracker = new ConcurrentHashMap<>();
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
        long now = System.currentTimeMillis();
        long lastTime = spamTracker.getOrDefault(session.getId(), 0L);

        if (now - lastTime < 200) {
            try {
                System.out.println(" Kicking spammer: " + session.getId());
                session.close(new CloseStatus(4000, "Spam detected"));
            } catch (Exception ignored) {}
            return;
        }
        spamTracker.put(session.getId(), now);
        try {
            Message incoming = objectMapper.readValue(textMessage.getPayload(), Message.class);
            String authToken = incoming.getAuthToken();

            Claims claims = JwtUtil.parseToken(authToken);
            if (claims == null) return;

            String username = claims.getSubject();

            if ("get-game-id".equalsIgnoreCase(incoming.getType())) {
                sessionManager.addSession(username, session);

                User u = userRepository.findByUsername(username);
                if (u != null) {
                    sessionManager.sendToUser(username, new Message("balance", "", u.getPoints(), 0));
                }

                for (Game game : gameService.getGames()) {
                    if (game.getConnectedUsers().contains(username)) {
                        sessionManager.sendToUser(username, new Message("game-id", username, game.getGame_id(), 0));
                        break;
                    }
                }
            }
            else if ("bet".equalsIgnoreCase(incoming.getType())) {
                int gameId = Integer.parseInt(incoming.getContent());
                Game game = null;
                for (Game g : gameService.getGames()) {
                    if (g.getGame_id() == gameId) {
                        game = g;
                        break;
                    }
                }

                if (game != null && game.getPhase().equals("BETTING") && game.getConnectedUsers().contains(username)) {
                    int betAmount = incoming.getValue();
                    int horse = incoming.getHorse();

                    if (betAmount > 0) {
                        User u = userRepository.findByUsername(username);
                        if (u != null && u.getPoints() >= betAmount) {
                            u.setPoints(u.getPoints() - betAmount);
                            userRepository.save(u);

                            game.getPlayers().put(username, new BetInfo(horse, betAmount));

                            sessionManager.sendToUser(username, new Message("balance", "", u.getPoints(), 0));

                            sessionManager.broadcastToGame(game, new Message("bet-placed", username, betAmount, horse));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("WS Error: " + e.getMessage());
        }
    }
}