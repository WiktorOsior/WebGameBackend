package com.webgame.backend.Services;

import com.webgame.backend.Repositories.RegisterInterface;
import com.webgame.backend.databases.Game;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class GameService {
    private final List<Game> games = new CopyOnWriteArrayList<>();
    private final AtomicInteger number_of_games = new AtomicInteger(0);
    private static final int MAX_ACTIVE_GAMES = 100;

    private final SessionManager sessionManager;
    private final RegisterInterface userRepository;

    public GameService(SessionManager sessionManager, RegisterInterface userRepository) {
        this.sessionManager = sessionManager;
        this.userRepository = userRepository;
    }

    public List<Game> getGames() { return games; }

    public int addGame() {
        if (games.size() >= MAX_ACTIVE_GAMES) {
            throw new IllegalStateException("Server is at maximum capacity"); // Catches in controller and returns 503
        }
        int newId = number_of_games.incrementAndGet();
        Game newGame = new Game(newId, 0, "BETTING");
        games.add(newGame);

        GameEngine engine = new GameEngine(newGame, this, sessionManager, userRepository);
        new Thread(engine).start();

        return newId;
    }

    public boolean joinGame(String username, int gameId) {
        for (Game game : games) {
            if (game.getGame_id() == gameId) {
                game.getConnectedUsers().add(username);
                return true;
            }
        }
        return false;
    }

    public void removeGame(int gameId) {
        games.removeIf(g -> g.getGame_id() == gameId);
    }

    public void removeUserFromAllGames(String username) {
        for (Game game : games) {
            game.getConnectedUsers().remove(username);
        }
    }

    public Integer getGameIdForUser(String username) {
        for (Game game : games) {
            if (game.getConnectedUsers().contains(username)) {
                return game.getGame_id();
            }
        }
        return null;
    }
}