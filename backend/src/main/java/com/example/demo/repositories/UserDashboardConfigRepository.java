package com.example.demo.repositories;

import com.example.demo.entities.UserDashboardConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserDashboardConfigRepository extends JpaRepository<UserDashboardConfig, Long> {
    Optional<UserDashboardConfig> findByUserSub(String userSub);
}
