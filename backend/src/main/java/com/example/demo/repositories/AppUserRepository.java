package com.example.demo.repositories;

import com.example.demo.entities.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findBySub(String sub);

    Optional<AppUser> findByAccessToken(String accessToken);

    List<AppUser> findByRole(String role);

    List<AppUser> findByRoleAndPlatform(String role, String platform);

    long countBySchool_Id(Long schoolId);

    List<AppUser> findBySchool_Id(Long schoolId);
}