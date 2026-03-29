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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

        logger.info("Found {} loans due tomorrow.", loans.size());

        Flux.fromIterable(loans)
                .flatMap(loan -> {
                    if (loan.getUserSub() == null)
                        return Flux.empty();
                    return Mono.justOrEmpty(appUserRepository.findBySub(loan.getUserSub()))
                            .flatMapMany(user -> sendReminderToUser(user, loan));
                })
                .doOnComplete(() -> logger.info("Finished processing reminders."))
                .subscribe();
    }

    private Flux<String> sendReminderToUser(AppUser user, Loan loan) {
        if (user.getSmartschoolRefreshToken() == null || loan.getCopy() == null || loan.getCopy().getBook() == null) {
            logger.warn("User {} has no refresh token. Cannot send automated reminder.", user.getSub());
            return Flux.empty();
        }

        return authService.getUserInfoBySub(user.getSub())
                .flatMap(userInfo -> {
                    SmartschoolMessageRequest messageRequest = new SmartschoolMessageRequest();
                    messageRequest.setPlatformUrl(smartschoolProperties.getApiBaseUrl());
                    messageRequest.setSubject("Herinnering: Inleveren bibliotheekboek");

                    String bookTitle = loan.getCopy().getBook().getTitel();
                    String body = String.format(
                            "Beste %s,\n\nDit is een automatische herinnering dat het boek '%s' morgen ingeleverd moet worden.\n\nMet vriendelijke groeten,\nDe bibliotheek.",
                            userInfo.getGivenName() != null ? userInfo.getGivenName() : "Lezer",
                            bookTitle);
                    messageRequest.setBody(body);

                    return smartschoolMessageService.sendMessage(userInfo.getAccessToken(), messageRequest);
                })
                .doOnSuccess(response -> logger.info("Successfully sent reminder for user sub {}", user.getSub()))
                .doOnError(error -> logger.error("Failed to send reminder for user sub {}", user.getSub(), error))
                .flux();
    }
}
