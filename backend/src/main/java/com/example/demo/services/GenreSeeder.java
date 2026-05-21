package com.example.demo.config;

import com.example.demo.entities.Genre;
import com.example.demo.repositories.GenreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds the database with default genres and subgenres if they don't exist.
 * This ensures the application starts with a baseline set of book categories
 * while allowing admins to manage them further via the management dashboard.
 */
@Component
public class GenreSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(GenreSeeder.class);
    private final GenreRepository genreRepository;

    public GenreSeeder(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    private static final List<String> DEFAULT_TOP_GENRES = List.of(
            "Fictie algemeen",
            "Literaire roman",
            "Spanning / thriller",
            "Detective / misdaad",
            "Fantasy",
            "Sciencefiction",
            "Dystopie",
            "Historische roman",
            "Romantiek",
            "Coming-of-age",
            "Avontuur",
            "Oorlog & conflict",
            "Horror",
            "Humor",
            "Graphic novel / strip",
            "Poëzie",
            "Didactiek",
            "Non-fictie algemeen"
    );

    private static final List<String> NON_FICTION_SUBGENRES = List.of(
            "Biografie / autobiografie",
            "Wetenschap & technologie",
            "Filosofie",
            "Maatschappij & politiek",
            "Psychologie",
            "Geschiedenis",
            "Kunst & cultuur"
    );

    private static final List<String> DIDACTIC_SUBGENRES = List.of(
            "Wiskunde", "Taal", "Geschiedenis", "Kleuteronderwijs",
            "Lager onderwijs", "Secundair onderwijs", "Volwasseneneducatie",
            "Geheugen", "Begrip", "Denkprocessen", "Samenwerking",
            "Interactie", "Dialoog", "Online leren", "E-learning platforms",
            "Educatieve apps", "Creativiteit", "Zelfexpressie", "Ervaringsgericht leren"
    );

    @Override
    @Transactional
    public void run(String... args) {
        logger.info("Checking for missing default genres...");

        for (String genreNaam : DEFAULT_TOP_GENRES) {
            Genre parent = genreRepository.findByNaamIgnoreCaseAndParentIsNull(genreNaam)
                    .orElseGet(() -> {
                        Genre g = new Genre();
                        g.setNaam(genreNaam);
                        logger.info("Seeding top-level genre: {}", genreNaam);
                        return genreRepository.save(g);
                    });

            if ("Non-fictie algemeen".equals(genreNaam)) {
                seedSubgenres(parent, NON_FICTION_SUBGENRES);
            } else if ("Didactiek".equals(genreNaam)) {
                seedSubgenres(parent, DIDACTIC_SUBGENRES);
            }
        }
    }

    private void seedSubgenres(Genre parent, List<String> subNames) {
        for (String name : subNames) {
            if (!genreRepository.existsByNaamIgnoreCaseAndParentId(name, parent.getId())) {
                Genre sub = new Genre();
                sub.setNaam(name);
                sub.setParent(parent);
                genreRepository.save(sub);
                logger.info("Seeded subgenre: {} for parent: {}", name, parent.getNaam());
            }
        }
    }
}