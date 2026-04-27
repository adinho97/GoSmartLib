package com.example.demo.config;

import com.example.demo.repositories.SchoolRepository;
import com.example.demo.entities.School;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchoolDataInitializer {

    @Bean
    CommandLineRunner seedSchools(SchoolRepository schoolRepository) {
        return args -> {
            saveOrUpdateSchool(schoolRepository, 1L, "GO! Atheneum Antwerpen",
                    "Franklin Rooseveltplaats 11, 2060 Antwerpen", 51.21985309919904, 4.418119109992717);

            saveOrUpdateSchool(schoolRepository, 2L, "GO! Basisschool De Brug",
                    "Dahliastraat 20, 2060 Antwerpen", 51.226805577293455, 4.419975666700077);

            saveOrUpdateSchool(schoolRepository, 3L, "GO! Middenschool Centrum",
                    "Lange Beeldekensstraat 264, 2060 Antwerpen", 51.2219517621366, 4.434647424681566);
        };
    }

    private void saveOrUpdateSchool(SchoolRepository repo, Long id, String naam, String adres, Double lat, Double lon) {
        School school = repo.findById(id).orElse(new School());
        school.setNaam(naam);
        school.setAdres(adres);
        school.setLatitude(lat);
        school.setLongitude(lon);
        repo.save(school);
    }
}
