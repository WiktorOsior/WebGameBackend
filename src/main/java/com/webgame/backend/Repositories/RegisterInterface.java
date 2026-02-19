package com.webgame.backend.Repositories;

import com.webgame.backend.databases.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RegisterInterface extends JpaRepository<User, UUID> {
    User findByUsername(String username);
}
