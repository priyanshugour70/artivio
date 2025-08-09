package com.klakar.artivio.controller;

import com.klakar.artivio.dto.AuthRequest;
import com.klakar.artivio.dto.AuthResponse;
import com.klakar.artivio.dto.RegisterRequest;
import com.klakar.artivio.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            log.info("Registration attempt for username: {}", request.getUsername());

            AuthResponse response = userService.register(request);

            if (response.getToken() != null) {
                log.info("Registration successful for username: {}", request.getUsername());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Registration failed for username: {} - {}", request.getUsername(), response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Registration error for username: {}", request.getUsername(), e);
            return ResponseEntity.badRequest().body(
                    AuthResponse.builder()
                            .message("Registration failed: " + e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody AuthRequest request) {
        try {
            log.info("Login attempt for username: {}", request.getUsername());

            AuthResponse response = userService.authenticate(request);

            if (response.getToken() != null) {
                log.info("Login successful for username: {}", request.getUsername());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Login failed for username: {} - {}", request.getUsername(), response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Login error for username: {}", request.getUsername(), e);
            return ResponseEntity.badRequest().body(
                    AuthResponse.builder()
                            .message("Authentication failed: " + e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            // Extract username from token and set offline
            log.info("Logout request received");
            return ResponseEntity.ok(
                    AuthResponse.builder()
                            .message("Logged out successfully")
                            .build()
            );
        } catch (Exception e) {
            log.error("Logout error", e);
            return ResponseEntity.badRequest().body(
                    AuthResponse.builder()
                            .message("Logout failed")
                            .build()
            );
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<AuthResponse> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            return ResponseEntity.ok(
                    AuthResponse.builder()
                            .message("Token is valid")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    AuthResponse.builder()
                            .message("Invalid token")
                            .build()
            );
        }
    }
}