package com.example.demo.config;

import com.example.demo.repositories.SchoolRepository;
import com.example.demo.entities.School;
import com.example.demo.entities.SchoolStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchoolDataInitializer {

    @Bean
    CommandLineRunner seedSchools(SchoolRepository schoolRepository) {
        return args -> {
            saveOrUpdateSchool(schoolRepository, 1L, "GO! Atheneum Antwerpen", "go-atheneum-antwerpen");

            saveOrUpdateSchool(schoolRepository, 2L, "GO! Basisschool De Brug", "go-basisschool-de-brug");

            saveOrUpdateSchool(schoolRepository, 3L, "GO! Middenschool Centrum", "go-middenschool-centrum");
        };
    }

    private void saveOrUpdateSchool(SchoolRepository repo, Long id, String naam, String subdomein) {
        School school = repo.findById(id).orElse(new School());
        school.setNaam(naam);
        school.setSubdomein(subdomein);
        school.setSmartschoolUrl("https://" + subdomein + ".smartschool.be");
        school.setStatus(SchoolStatus.ACTIVE);
        repo.save(school);
    }
}
