package com.example.demo.repositories;

import com.example.demo.entities.Invite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InviteRepository extends JpaRepository<Invite, Long> {

    Optional<Invite> findByToken(String token);

    Optional<Invite> findByTokenAndUsedFalse(String token);

    Optional<Invite> findBySchoolId(String schoolId);
}
