package com.example.demo.config;

import com.example.demo.services.InviteService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Gebruik: java -jar app.jar --generate-invite aphogeschool.smartschool.be -? school id is aphogeschool hier bv

/*
# Voor andereschool:
java -jar app.jar --generate-invite andereschool.smartschool.be
# → Printt ander token + link
*/

@Component
public class InviteGenerator implements CommandLineRunner {

    private final InviteService inviteService;

    public InviteGenerator(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Check of --generate-invite flag aanwezig is
        if (args.length >= 2 && "--generate-invite".equals(args[0])) {
            String schoolId = args[1];
            String token = inviteService.generateInvite(schoolId);
            System.out.println("\n========================================");
            System.out.println("✓ Invite token gegenereerd voor: " + schoolId);
            System.out.println("Token: " + token);
            System.out.println("Link: https://app.be/setup/invite/" + token);
            System.out.println("Geldig voor: 7 dagen");
            System.out.println("========================================\n");
        }
    }
}
