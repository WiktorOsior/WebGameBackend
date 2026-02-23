package com.webgame.backend.Controllers;

import com.webgame.backend.Dtos.RegisterDto;
import com.webgame.backend.Services.RegisterService;
import com.webgame.backend.configurations.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class RegisterController {
    private final RegisterService registerService;

    @Autowired
    private AuthenticationManager authenticationManager;

    private final RestTemplate restTemplate = new RestTemplate(); // Built-in Spring HTTP client

    @Value("${TURNSTILE_SECRET}")
    private String TURNSTILE_SECRET_KEY;

    @Value("${COOKIE_DOMAIN:localhost}")
    private String cookieDomain;

    @Value("${COOKIE_SECURE:false}")
    private boolean cookieSecure;

    public RegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterDto registerDto){

        boolean isHuman = verifyTurnstileToken(registerDto.captchaToken());
        if (!isHuman) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Security check failed. Are you a bot?"));
        }

        try {
            registerService.create(registerDto);
            return ResponseEntity.ok(Map.of("message", "Created Successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Username is already taken"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An unexpected error occurred"));
        }
    }

    private boolean verifyTurnstileToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        String url = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("secret", TURNSTILE_SECRET_KEY);
        map.add("response", token);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();
            return body != null && Boolean.TRUE.equals(body.get("success"));
        } catch (Exception e) {
            System.err.println("Turnstile Verification Failed: " + e.getMessage());
            return false;
        }
    }


    @PostMapping("/perform_login")
    public ResponseEntity<Object> login(@RequestBody RegisterDto registerDto, HttpServletRequest request, HttpServletResponse response) {
        System.out.print("Login attempt for: " + registerDto.username());

        boolean isHuman = verifyTurnstileToken(registerDto.captchaToken());
        if (!isHuman) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new LoginResponse("Security check failed. Are you a bot?"));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(registerDto.username(), registerDto.password())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String token = JwtUtil.generateToken(registerDto.username());

            Cookie cookie = new Cookie("AUTH-TOKEN", token);
            cookie.setHttpOnly(false);
            cookie.setSecure(cookieSecure);
            cookie.setPath("/");


            if (!"localhost".equals(cookieDomain)) {
                cookie.setDomain(cookieDomain);
            }

            response.addCookie(cookie);

            System.out.print("Login Successfully");
            return ResponseEntity.ok().body(new LoginResponse("Login Successfully"));

        } catch (AuthenticationException e) {
            System.out.print("Login Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse("Login Failed: Incorrect username or password"));
        }
    }
}
