package com.example.demo.repositories;

import com.example.demo.entities.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByAppUserId(Long appUserId);

    List<UserRole> findBySchoolIdAndRole(Long schoolId, String role);
}
