package com.example.demo;

import com.example.demo.entities.Boek;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoekRepository extends JpaRepository<Boek, Long> {
}
