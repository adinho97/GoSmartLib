package com.example.demo.services;

import com.example.demo.config.AuthService;
import com.example.demo.config.SmartschoolMessageRequest;
import com.example.demo.config.SmartschoolMessageService;
import com.example.demo.config.SmartschoolProperties;
import com.example.demo.entities.Loan;
import com.example.demo.repositories.LoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class LoanReminderService {

    private static final Logger logger = LoggerFactory.getLogger(LoanReminderService.class);

    private final LoanRepository loanRepository;
    private final SmartschoolMessageService smartschoolMessageService;
    private final AuthService authService;
    private final SmartschoolProperties smartschoolProperties;

    public LoanReminderService(LoanRepository loanRepository,
        SmartschoolMessageService smartschoolMessageService, AuthService authService,
        SmartschoolProperties smartschoolProperties) {
        this.loanRepository = loanRepository;
        this.smartschoolMessageService = smartschoolMessageService;
        this.authService = authService;
        this.smartschoolProperties = smartschoolProperties;
    }

    @Scheduled(cron = "0 0 13 * * ?", zone = "Europe/Brussels") // Runs every day at 13:00
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

    @Scheduled(cron = "0 0 13 * * ?", zone = "Europe/Brussels") // Runs every day at 13:00
    public void sendOverdueNotifications() {
        LocalDate today = LocalDate.now();
        logger.info("Starting automated overdue book check for date: {}", today);

        List<Loan> overdueLoans = loanRepository.findByDueDateBeforeAndReturnedAtIsNull(today);
        logger.info("Found {} overdue loans", overdueLoans.size());
        if (overdueLoans.isEmpty()) {
            logger.info("No overdue loans found.");
            return;
        }

        Flux.fromIterable(overdueLoans)
                .flatMap(this::processOverdueNotification)
                .subscribe(
                        success -> logger.debug("Overdue notification processed successfully."),
                        error -> logger.error("Error in overdue notification job batch", error),
                        () -> logger.info("Finished processing all overdue notifications"));
    }

    private Mono<String> processLoanReminder(Loan loan) {
        return authService.getUserInfoBySub(loan.getUserSub())
                .flatMap(userInfo -> {
                    SmartschoolMessageRequest request = new SmartschoolMessageRequest();
                    String platform = (userInfo.getPlatform() != null) ? userInfo.getPlatform()
                            : smartschoolProperties.getApiBaseUrl();

                    request.setPlatformUrl(platform);
                    request.setSubject("Herinnering: inleveren bibliotheekboek morgen");
                    request.setBody(buildReminderHtml(
                            userInfo.getName() != null ? userInfo.getName() : "Lezer",
                            loan.getCopy().getBook().getTitel(),
                            loan.getDueDate() != null ? loan.getDueDate().toString() : "onbekend"));

                    return smartschoolMessageService.sendMessage(userInfo.getAccessToken(), request);
                })
                .doOnSuccess(res -> logger.info("Sent reminder to {} for {}", loan.getUserSub(),
                        loan.getCopy().getBook().getTitel()))
                .doOnError(err -> logger.error("Failed reminder for {}: {}", loan.getUserSub(), err.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    private Mono<String> processOverdueNotification(Loan loan) {
        return authService.getUserInfoBySub(loan.getUserSub())
                .flatMap(userInfo -> {
                    SmartschoolMessageRequest request = new SmartschoolMessageRequest();
                    String platform = (userInfo.getPlatform() != null) ? userInfo.getPlatform()
                            : smartschoolProperties.getApiBaseUrl();

                    long daysLate = ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now());

                    request.setPlatformUrl(platform);
                    request.setSubject(String.format("Herinnering: boek %d dag(en) te laat", daysLate));
                    request.setBody(buildOverdueHtml(
                            userInfo.getName() != null ? userInfo.getName() : "Lezer",
                            loan.getCopy().getBook().getTitel(),
                            loan.getDueDate() != null ? loan.getDueDate().toString() : "onbekend",
                            daysLate));

                    return smartschoolMessageService.sendMessage(userInfo.getAccessToken(), request);
                })
                .doOnSuccess(res -> logger.info("Sent overdue notification to {} for {}", loan.getUserSub(),
                        loan.getCopy().getBook().getTitel()))
                .doOnError(err -> logger.error("Failed overdue notification for {}: {}", loan.getUserSub(), err.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    // ── HTML builders ──────────────────────────────────────────────────────────

    private String buildReminderHtml(String name, String title, String dueDate) {
        return String.format("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; overflow: hidden;">
                  <div style="background-color: #1a3a5c; padding: 24px 32px;">
                    <h1 style="margin: 0; color: #ffffff; font-size: 20px; font-weight: normal; letter-spacing: 0.5px;">
                       Bibliotheek — Herinnering
                    </h1>
                  </div>
                  <div style="padding: 28px 32px;">
                    <p style="margin: 0 0 16px; font-size: 15px; color: #333;">Beste <strong>%s</strong></p>
                    <p style="margin: 0 0 24px; font-size: 15px; color: #333;">
                      Dit is een vriendelijke herinnering dat onderstaand boek <strong>morgen</strong> teruggebracht moet worden.
                    </p>
                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 24px; font-family: Arial, sans-serif;">
                      <thead>
                        <tr style="background-color: #1a3a5c; color: #fff;">
                          <th style="padding: 10px 12px; text-align: left; font-size: 13px;">Titel</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr style="background-color: #f9f9f9;">
                          <td style="padding: 8px 12px; font-size: 14px; color: #222;">%s</td>
                        </tr>
                      </tbody>
                    </table>
                    <div style="background-color: #fff8e1; border-left: 4px solid #f0a500; padding: 14px 18px; border-radius: 3px; margin-bottom: 24px;">
                      <p style="margin: 0; font-size: 14px; color: #7a5c00;"><strong>Terugbrengdatum:</strong> %s</p>
                      <p style="margin: 6px 0 0; font-size: 13px; color: #9a7a20;">Gelieve het boek morgen terug te brengen naar de bibliotheek.</p>
                    </div>
                    <p style="margin: 0; font-size: 14px; color: #555;">Met vriendelijke groeten<br><strong>De bibliotheek</strong></p>
                  </div>
                  <div style="background-color: #f5f5f5; padding: 14px 32px; border-top: 1px solid #e0e0e0;">
                    <p style="margin: 0; font-size: 12px; color: #999; text-align: center;">Dit is een automatisch gegenereerd bericht — gelieve niet te antwoorden.</p>
                  </div>
                </div>
                """, escapeHtml(name), escapeHtml(title), dueDate);
    }

    private String buildOverdueHtml(String name, String title, String dueDate, long daysLate) {
        return String.format("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; overflow: hidden;">
                  <div style="background-color: #8b1a1a; padding: 24px 32px;">
                    <h1 style="margin: 0; color: #ffffff; font-size: 20px; font-weight: normal; letter-spacing: 0.5px;">
                      Bibliotheek — Boek te laat
                    </h1>
                  </div>
                  <div style="padding: 28px 32px;">
                    <p style="margin: 0 0 16px; font-size: 15px; color: #333;">Beste <strong>%s</strong></p>
                    <p style="margin: 0 0 24px; font-size: 15px; color: #333;">
                      Onderstaand boek had <strong>%d dag(en) geleden</strong> teruggebracht moeten worden.
                      Gelieve het zo snel mogelijk terug te brengen naar de bibliotheek.
                    </p>
                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 24px; font-family: Arial, sans-serif;">
                      <thead>
                        <tr style="background-color: #8b1a1a; color: #fff;">
                          <th style="padding: 10px 12px; text-align: left; font-size: 13px;">Titel</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr style="background-color: #f9f9f9;">
                          <td style="padding: 8px 12px; font-size: 14px; color: #222;">%s</td>
                        </tr>
                      </tbody>
                    </table>
                    <div style="background-color: #fff0f0; border-left: 4px solid #c0392b; padding: 14px 18px; border-radius: 3px; margin-bottom: 24px;">
                      <p style="margin: 0; font-size: 14px; color: #7a0000;"><strong>Had teruggebracht moeten zijn op:</strong> %s</p>
                      <p style="margin: 6px 0 0; font-size: 13px; color: #a00000;">Breng het boek zo snel mogelijk terug om verdere vertraging te vermijden.</p>
                    </div>
                    <p style="margin: 0; font-size: 14px; color: #555;">Met vriendelijke groeten<br><strong>De bibliotheek</strong></p>
                  </div>
                  <div style="background-color: #f5f5f5; padding: 14px 32px; border-top: 1px solid #e0e0e0;">
                    <p style="margin: 0; font-size: 12px; color: #999; text-align: center;">Dit is een automatisch gegenereerd bericht — gelieve niet te antwoorden.</p>
                  </div>
                </div>
                """, escapeHtml(name), daysLate, escapeHtml(title), dueDate);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}