package com.klakar.artivio.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;
    private Boolean isOnline;
    private LocalDateTime lastSeen;
    private LocalDateTime createdAt;
}