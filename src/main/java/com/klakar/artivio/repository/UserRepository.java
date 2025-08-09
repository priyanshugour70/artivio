package com.klakar.artivio.repository;

import com.klakar.artivio.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.isOnline = true AND u.id != :userId AND u.isActive = true")
    List<User> findOnlineUsersExcept(@Param("userId") Long userId);

    @Query("SELECT u FROM User u WHERE u.id != :userId AND u.isActive = true ORDER BY u.isOnline DESC, u.lastSeen DESC")
    List<User> findAllActiveUsersExcept(@Param("userId") Long userId);

    @Query("SELECT u FROM User u WHERE u.isActive = true ORDER BY u.createdAt DESC")
    List<User> findAllActiveUsers();
}