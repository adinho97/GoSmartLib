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
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        logger.info("Starting automated loan reminder check for date: {}", tomorrow);

        List<Loan> loans = loanRepository.findByDueDateAndReturnedAtIsNull(tomorrow);
        logger.info("Found {} loans due for reminder on {}", loans.size(), tomorrow);
        if (loans.isEmpty()) {
            logger.info("No loans due on {}. No reminders sent.", tomorrow);
            return;
        }

        Flux.fromIterable(loans)
                .flatMap(this::processLoanReminder)
                .subscribe(
                        success -> logger.debug("Reminder processed successfully."),
                        error -> logger.error("Error in reminder job batch", error),
                        () -> logger.info("Finished processing all reminders for {}", tomorrow));
    }

    private Mono<String> processLoanReminder(Loan loan) {
        return authService.getUserInfoBySub(loan.getUserSub())
                .flatMap(userInfo -> {
                    SmartschoolMessageRequest request = new SmartschoolMessageRequest();
                    String platform = (userInfo.getPlatform() != null) ? userInfo.getPlatform()
                            : smartschoolProperties.getApiBaseUrl();

                    request.setPlatformUrl(platform);
                    request.setSubject("Herinnering: Inleveren bibliotheekboek");
                    request.setBody(String.format(
                            "Beste %s,\n\nHet boek '%s' moet morgen ingeleverd worden.\n\nMet vriendelijke groeten,\nDe bibliotheek.",
                            userInfo.getName() != null ? userInfo.getName() : "Lezer",
                            loan.getCopy().getBook().getTitel()));

                    return smartschoolMessageService.sendMessage(userInfo.getAccessToken(), request);
                })
                .doOnSuccess(res -> logger.info("Sent reminder to {} for {}", loan.getUserSub(),
                        loan.getCopy().getBook().getTitel()))
                .doOnError(err -> logger.error("Failed reminder for {}: {}", loan.getUserSub(), err.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }
}
