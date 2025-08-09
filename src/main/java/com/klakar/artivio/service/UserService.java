package com.klakar.artivio.service;

import com.klakar.artivio.dto.AuthRequest;
import com.klakar.artivio.dto.AuthResponse;
import com.klakar.artivio.dto.RegisterRequest;
import com.klakar.artivio.dto.UserDTO;
import com.klakar.artivio.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface UserService extends UserDetailsService {
    AuthResponse register(RegisterRequest request);
    AuthResponse authenticate(AuthRequest request);
    void setUserOffline(String username);
    void setUserOnline(String username);
    List<UserDTO> getOnlineUsers(String currentUsername);
    List<UserDTO> getAllUsers(String currentUsername);
    UserDTO getUserById(Long id);
    UserDTO getUserByUsername(String username);
    User findUserByUsername(String username);
    User findUserById(Long id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}