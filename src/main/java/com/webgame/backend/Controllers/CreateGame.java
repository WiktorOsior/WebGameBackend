package com.webgame.backend.Controllers;

import com.webgame.backend.databases.Game;
import com.webgame.backend.Services.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CreateGame {

    private final GameService gameService;

    public CreateGame(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/create")
    public ResponseEntity<?> CreateGame(Authentication authentication) {
        String username = authentication.getName();

        Integer existingGameId = gameService.getGameIdForUser(username);
        if (existingGameId != null) {
            return ResponseEntity.status(409).body("User is already in game " + existingGameId);
        }

        int gameId = gameService.addGame();
        return JoinGame(authentication, gameId);
    }

    @GetMapping("/list")
    public ResponseEntity<?> getList() {
        List<Game> games = gameService.getGames();
        return games.isEmpty() ? ResponseEntity.ok("There are no games") : ResponseEntity.ok(games);
    }

    @GetMapping("/join/{game_id}")
    public ResponseEntity<?> JoinGame(Authentication authentication, @PathVariable int game_id) {
        String username = authentication.getName();

        Integer existingGameId = gameService.getGameIdForUser(username);

        if (existingGameId != null && existingGameId != game_id) {
            return ResponseEntity.status(409).body("User is already in game " + existingGameId);
        }

        boolean joined = gameService.joinGame(username, game_id);
        if (joined) {
            return ResponseEntity.status(200).body("Game found and user joined");
        } else {
            return ResponseEntity.status(404).body("Game not found");
        }
    }
}