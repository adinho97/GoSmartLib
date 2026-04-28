package com.example.demo.repositories;

import com.example.demo.entities.SuperAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SuperAdminRepository extends JpaRepository<SuperAdmin, Long> {
    Optional<SuperAdmin> findByEmail(String email);
}
