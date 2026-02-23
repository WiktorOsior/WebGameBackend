package com.webgame.backend.Dtos;

public record RegisterDto(
        String username,
        String password,
        String captchaToken) {

}
