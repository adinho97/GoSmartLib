package com.example.demo.config;

import com.example.demo.SchoolRepository;
import com.example.demo.entities.School;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SchoolDataInitializer {

    @Bean
    CommandLineRunner seedSchools(SchoolRepository schoolRepository) {
        return args -> {
            if (schoolRepository.count() > 0) {
                return;
            }

            List<String> standaardScholen = List.of(
                    "GO! Atheneum Antwerpen",
                    "GO! Basisschool De Brug",
                    "GO! Middenschool Centrum");

            for (String naam : standaardScholen) {
                School school = new School();
                school.setNaam(naam);
                schoolRepository.save(school);
            }
        };
    }
}
