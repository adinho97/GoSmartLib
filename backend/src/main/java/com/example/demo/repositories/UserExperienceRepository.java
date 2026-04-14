package com.example.demo.repositories;

import com.example.demo.entities.UserExperience;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserExperienceRepository extends JpaRepository<UserExperience, Long> {
    Optional<UserExperience> findByUserSub(String userSub);
}
