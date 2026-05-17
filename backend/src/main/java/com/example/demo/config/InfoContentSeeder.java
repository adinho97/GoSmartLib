package com.example.demo.config;

import com.example.demo.entities.InfoContent;
import com.example.demo.entities.InfoContent.Sectie;
import com.example.demo.repositories.InfoContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InfoContentSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(InfoContentSeeder.class);

    private final InfoContentRepository infoContentRepository;

    public InfoContentSeeder(InfoContentRepository infoContentRepository) {
        this.infoContentRepository = infoContentRepository;
    }

    private static final List<String> DEFAULT_LOAN_STEPS = List.of(
        "Zoek een boek via Boekencatalogus en open de detailpagina.",
        "Controleer of het boek beschikbaar is in de bibliotheek.",
        "Vind het boek in de bibliotheek en ga naar de bib-verantwoordelijke om het te ontlenen.",
        "Het boek verschijnt daarna bij Geleende boeken in je lijsten.",
        "Lever op tijd in om sancties te voorkomen."
    );

    private record FeatureSeed(String title, String description) {}

    private static final List<FeatureSeed> DEFAULT_FEATURES = List.of(
        new FeatureSeed("Dashboard",
            "persoonlijke aanbevelingen en snelle toegang tot je profielblokken."),
        new FeatureSeed("Boekencatalogus",
            "zoeken, filteren en boekdetails bekijken."),
        new FeatureSeed("Mijn lijsten",
            "verlanglijsten, ontleenhistoriek, klasleeslijsten en geleende boeken."),
        new FeatureSeed("Klassement",
            "bekijk de top lezers in jouw klas en school.")
    );

    @Override
    public void run(String... args) {
        seedStapDefaults();
        seedFeatureDefaults();
    }

    private void seedStapDefaults() {
        if (infoContentRepository.existsBySchoolIsNullAndSectie(Sectie.STAP)) {
            return;
        }
        for (int i = 0; i < DEFAULT_LOAN_STEPS.size(); i++) {
            InfoContent item = new InfoContent();
            item.setSchool(null);
            item.setSectie(Sectie.STAP);
            item.setInhoud(DEFAULT_LOAN_STEPS.get(i));
            item.setSortOrder(i);
            infoContentRepository.save(item);
        }
        logger.info("Seeded {} global STAP defaults.", DEFAULT_LOAN_STEPS.size());
    }

    private void seedFeatureDefaults() {
        if (infoContentRepository.existsBySchoolIsNullAndSectie(Sectie.FEATURE)) {
            return;
        }
        for (int i = 0; i < DEFAULT_FEATURES.size(); i++) {
            FeatureSeed seed = DEFAULT_FEATURES.get(i);
            InfoContent item = new InfoContent();
            item.setSchool(null);
            item.setSectie(Sectie.FEATURE);
            item.setTitel(seed.title());
            item.setInhoud(seed.description());
            item.setSortOrder(i);
            infoContentRepository.save(item);
        }
        logger.info("Seeded {} global FEATURE defaults.", DEFAULT_FEATURES.size());
    }
}
