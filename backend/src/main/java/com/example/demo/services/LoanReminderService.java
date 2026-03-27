package com.example.demo.services;

import com.example.demo.config.AuthService;
import com.example.demo.config.SmartschoolMessageRequest;
import com.example.demo.config.SmartschoolMessageService;
import com.example.demo.config.SmartschoolProperties;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Loan;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.LoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class LoanReminderService {

    private static final Logger logger = LoggerFactory.getLogger(LoanReminderService.class);

    private final LoanRepository loanRepository;
    private final AppUserRepository appUserRepository;
    private final SmartschoolMessageService smartschoolMessageService;
    private final AuthService authService;
    private final SmartschoolProperties smartschoolProperties;

    public LoanReminderService(LoanRepository loanRepository, AppUserRepository appUserRepository,
            SmartschoolMessageService smartschoolMessageService, AuthService authService,
            SmartschoolProperties smartschoolProperties) {
        this.loanRepository = loanRepository;
        this.appUserRepository = appUserRepository;
        this.smartschoolMessageService = smartschoolMessageService;
        this.authService = authService;
        this.smartschoolProperties = smartschoolProperties;
    }

    @Scheduled(cron = "0 0 9 * * ?") // Runs every day at 9 AM
    public void sendLoanReminders() {
        logger.info("Running loan reminder job.");
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Loan> loans = loanRepository.findByDueDateAndReturnedAtIsNull(tomorrow);

        for (Loan loan : loans) {
            appUserRepository.findBySub(loan.getUserSub())
                    .ifPresent(user -> sendReminderToUser(user, loan));
        }
    }

    private void sendReminderToUser(AppUser user, Loan loan) {
        if (user.getSmartschoolRefreshToken() == null) {
            logger.warn("User {} has no refresh token. Cannot send automated reminder.", user.getSub());
            return;
        }

        authService.refreshAccessToken(user.getSmartschoolRefreshToken())
                .flatMap(authService::getUserInfo)
                .flatMap(userInfo -> {
                    SmartschoolMessageRequest messageRequest = new SmartschoolMessageRequest();
                    // Ensure the platform URL is set for the message API
                    messageRequest.setPlatformUrl(smartschoolProperties.getApiBaseUrl());
                    messageRequest.setSubject("Herinnering: Inleveren bibliotheekboek");

                    String body = String.format(
                            "Beste %s,\n\nDit is een automatische herinnering dat het boek '%s' morgen ingeleverd moet worden.\n\nMet vriendelijke groeten,\nDe bibliotheek.",
                            userInfo.getGivenName(),
                            loan.getCopy().getBook().getTitel());
                    messageRequest.setBody(body);

                    return smartschoolMessageService.sendMessage(userInfo.getAccessToken(), messageRequest);
                })
                .doOnSuccess(response -> logger.info("Successfully sent reminder for user sub {}", user.getSub()))
                .doOnError(error -> logger.error("Failed to send reminder for user sub {}", user.getSub(), error))
                .subscribe();
    }
}
