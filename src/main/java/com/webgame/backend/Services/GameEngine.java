package com.webgame.backend.Services;

import com.webgame.backend.Dtos.Message;
import com.webgame.backend.Repositories.RegisterInterface;
import com.webgame.backend.databases.BetInfo;
import com.webgame.backend.databases.Game;
import com.webgame.backend.databases.User;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GameEngine implements Runnable {
    private final Game game;
    private final GameService gameService;
    private final SessionManager sessionManager;
    private final RegisterInterface userRepository;
    private boolean running = true;

    public GameEngine(Game game, GameService gameService, SessionManager sessionManager, RegisterInterface userRepository) {
        this.game = game;
        this.gameService = gameService;
        this.sessionManager = sessionManager;
        this.userRepository = userRepository;
    }

    @Override
    public void run() {
        while (running) {
            try {
                game.setPhase("BETTING");
                game.getPlayers().clear();
                sessionManager.broadcastToGame(game, new Message("phase-change", "BETTING", 0, 0));

                Thread.sleep(25000);

                if (game.getConnectedUsers().isEmpty()) {
                    shutDown();
                    break;
                }

                game.setPhase("RACING"); // Blocks new bets
                Map<String, Double> horseTimes = generateTimes();
                Message raceMsg = new Message("phase-change", "RACING", 0, 0);
                raceMsg.setMap(horseTimes);
                sessionManager.broadcastToGame(game, raceMsg);

                double maxTime = Collections.max(horseTimes.values());
                Thread.sleep((long) (maxTime * 1000) + 1000);

                game.setPhase("WINNER");
                int winnerHorse = calculateWinner(horseTimes);
                processBets(winnerHorse);

                sessionManager.broadcastToGame(game, new Message("winner", String.valueOf(winnerHorse), 0, 0));

                Thread.sleep(5000);

                if (game.getConnectedUsers().isEmpty()) {
                    shutDown();
                    break;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    private void shutDown() {
        gameService.removeGame(game.getGame_id());
        running = false;
    }

    private Map<String, Double> generateTimes() {
        Map<String, Double> times = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            double time = 3.0 + (Math.random() * 7.0);
            time = Math.round(time * 100.0) / 100.0;
            times.put(String.valueOf(i), time);
        }
        return times;
    }

    private int calculateWinner(Map<String, Double> horseTimes) {
        int winner = 1;
        double bestTime = Double.MAX_VALUE;
        for (Map.Entry<String, Double> entry : horseTimes.entrySet()) {
            if (entry.getValue() < bestTime) {
                bestTime = entry.getValue();
                winner = Integer.parseInt(entry.getKey());
            }
        }
        return winner;
    }

    private void processBets(int winnerHorse) {
        for (Map.Entry<String, BetInfo> entry : game.getPlayers().entrySet()) {
            String username = entry.getKey();
            BetInfo bet = entry.getValue();

            if (bet.getHorse() == winnerHorse) {
                User u = userRepository.findByUsername(username);
                if (u != null) {
                    int winnings = bet.getAmount() * 2;
                    u.setPoints(u.getPoints() + winnings);
                    userRepository.save(u);

                    sessionManager.sendToUser(username, new Message("balance", "", u.getPoints(), 0));
                }
            }
        }
    }
}