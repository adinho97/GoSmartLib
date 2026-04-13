package com.example.demo.repositories;

import com.example.demo.entities.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    List<UserPreference> findByUserSub(String userSub);

    Optional<UserPreference> findByUserSubAndPreferenceKey(String userSub, String preferenceKey);

    void deleteByUserSubAndPreferenceKey(String userSub, String preferenceKey);
}
