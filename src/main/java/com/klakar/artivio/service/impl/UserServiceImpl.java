package com.klakar.artivio.service.impl;

import com.klakar.artivio.dto.AuthRequest;
import com.klakar.artivio.dto.AuthResponse;
import com.klakar.artivio.dto.RegisterRequest;
import com.klakar.artivio.dto.UserDTO;
import com.klakar.artivio.entity.User;
import com.klakar.artivio.repository.UserRepository;
import com.klakar.artivio.service.JwtService;
import com.klakar.artivio.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ModelMapper modelMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        try {
            if (existsByUsername(request.getUsername())) {
                return AuthResponse.builder()
                        .message("Username already exists")
                        .build();
            }

            if (existsByEmail(request.getEmail())) {
                return AuthResponse.builder()
                        .message("Email already exists")
                        .build();
            }

            User user = User.builder()
                    .username(request.getUsername().trim().toLowerCase())
                    .email(request.getEmail().trim().toLowerCase())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .displayName(request.getDisplayName() != null ?
                            request.getDisplayName().trim() : request.getUsername())
                    .isOnline(true)
                    .lastSeen(LocalDateTime.now())
                    .isActive(true)
                    .build();

            User savedUser = userRepository.save(user);
            String jwtToken = jwtService.generateToken(savedUser);

            UserDTO userDTO = modelMapper.map(savedUser, UserDTO.class);

            log.info("User registered successfully: {}", savedUser.getUsername());

            return AuthResponse.builder()
                    .token(jwtToken)
                    .user(userDTO)
                    .message("Registration successful")
                    .build();

        } catch (Exception e) {
            log.error("Error during user registration", e);
            return AuthResponse.builder()
                    .message("Registration failed: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public AuthResponse authenticate(AuthRequest request) {
        try {
            User user = userRepository.findByUsername(request.getUsername().trim().toLowerCase())
                    .orElseThrow(() -> new RuntimeException("Invalid credentials"));

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new RuntimeException("Invalid credentials");
            }

            if (!user.getIsActive()) {
                throw new RuntimeException("Account is deactivated");
            }

            // Update online status
            user.setIsOnline(true);
            user.setLastSeen(LocalDateTime.now());
            userRepository.save(user);

            String jwtToken = jwtService.generateToken(user);
            UserDTO userDTO = modelMapper.map(user, UserDTO.class);

            log.info("User authenticated successfully: {}", user.getUsername());

            return AuthResponse.builder()
                    .token(jwtToken)
                    .user(userDTO)
                    .message("Authentication successful")
                    .build();

        } catch (Exception e) {
            log.error("Error during authentication", e);
            return AuthResponse.builder()
                    .message("Authentication failed: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public void setUserOffline(String username) {
        try {
            userRepository.findByUsername(username).ifPresent(user -> {
                user.setIsOnline(false);
                user.setLastSeen(LocalDateTime.now());
                userRepository.save(user);
                log.info("User set offline: {}", username);
            });
        } catch (Exception e) {
            log.error("Error setting user offline: {}", username, e);
        }
    }

    @Override
    public void setUserOnline(String username) {
        try {
            userRepository.findByUsername(username).ifPresent(user -> {
                user.setIsOnline(true);
                user.setLastSeen(LocalDateTime.now());
                userRepository.save(user);
                log.info("User set online: {}", username);
            });
        } catch (Exception e) {
            log.error("Error setting user online: {}", username, e);
        }
    }

    @Override
    public List<UserDTO> getOnlineUsers(String currentUsername) {
        try {
            User currentUser = findUserByUsername(currentUsername);
            return userRepository.findOnlineUsersExcept(currentUser.getId())
                    .stream()
                    .map(user -> modelMapper.map(user, UserDTO.class))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting online users", e);
            return List.of();
        }
    }

    @Override
    public List<UserDTO> getAllUsers(String currentUsername) {
        try {
            User currentUser = findUserByUsername(currentUsername);
            return userRepository.findAllActiveUsersExcept(currentUser.getId())
                    .stream()
                    .map(user -> modelMapper.map(user, UserDTO.class))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting all users", e);
            return List.of();
        }
    }

    @Override
    public UserDTO getUserById(Long id) {
        User user = findUserById(id);
        return modelMapper.map(user, UserDTO.class);
    }

    @Override
    public UserDTO getUserByUsername(String username) {
        User user = findUserByUsername(username);
        return modelMapper.map(user, UserDTO.class);
    }

    @Override
    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Override
    public User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username.trim().toLowerCase());
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email.trim().toLowerCase());
    }
}