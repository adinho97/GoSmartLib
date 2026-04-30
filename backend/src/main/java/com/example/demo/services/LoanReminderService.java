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
import java.time.temporal.ChronoUnit;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class LoanReminderService {

    private static final Logger logger = LoggerFactory.getLogger(LoanReminderService.class);

    private static final String LOGO_BASE64 = "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";

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

    @Scheduled(cron = "0 0 9 * * ?", zone = "Europe/Brussels") // Runs every day at 9 AM
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

    @Scheduled(cron = "0 0 10 * * ?", zone = "Europe/Brussels") // Runs every day at 10 AM
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
                <div style="font-family: Georgia, serif; max-width: 600px; margin: 0 auto; background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; overflow: hidden;">
                  <div style="background-color: #1a3a5c; padding: 16px 32px;">
                    <img src="data:image/png;base64,%s" alt="GO! Antwerpen" style="height: 48px; width: auto; display: block;" />
                  </div>
                  <div style="padding: 28px 32px;">
                    <p style="margin: 0 0 16px; font-size: 15px; color: #333;">Beste <strong>%s</strong>,</p>
                    <p style="margin: 0 0 24px; font-size: 15px; color: #333;">
                      Dit is een vriendelijke herinnering dat onderstaand boek <strong>morgen</strong> teruggebracht moet worden.
                    </p>
                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 24px; font-family: Georgia, serif;">
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
                    <p style="margin: 0; font-size: 14px; color: #555;">Met vriendelijke groeten,<br><strong>De bibliotheek</strong></p>
                  </div>
                  <div style="background-color: #f5f5f5; padding: 14px 32px; border-top: 1px solid #e0e0e0;">
                    <p style="margin: 0; font-size: 12px; color: #999; text-align: center;">Dit is een automatisch gegenereerd bericht — gelieve niet te antwoorden.</p>
                  </div>
                </div>
                """, LOGO_BASE64, escapeHtml(name), escapeHtml(title), dueDate);
    }

    private String buildOverdueHtml(String name, String title, String dueDate, long daysLate) {
        return String.format("""
                <div style="font-family: Georgia, serif; max-width: 600px; margin: 0 auto; background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; overflow: hidden;">
                  <div style="background-color: #8b1a1a; padding: 16px 32px;">
                    <img src="data:image/png;base64,%s" alt="GO! Antwerpen" style="height: 48px; width: auto; display: block;" />
                  </div>
                  <div style="padding: 28px 32px;">
                    <p style="margin: 0 0 16px; font-size: 15px; color: #333;">Beste <strong>%s</strong>,</p>
                    <p style="margin: 0 0 24px; font-size: 15px; color: #333;">
                      Onderstaand boek had <strong>%d dag(en) geleden</strong> teruggebracht moeten worden.
                      Gelieve het zo snel mogelijk terug te brengen naar de bibliotheek.
                    </p>
                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 24px; font-family: Georgia, serif;">
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
                    <p style="margin: 0; font-size: 14px; color: #555;">Met vriendelijke groeten,<br><strong>De bibliotheek</strong></p>
                  </div>
                  <div style="background-color: #f5f5f5; padding: 14px 32px; border-top: 1px solid #e0e0e0;">
                    <p style="margin: 0; font-size: 12px; color: #999; text-align: center;">Dit is een automatisch gegenereerd bericht — gelieve niet te antwoorden.</p>
                  </div>
                </div>
                """, LOGO_BASE64, escapeHtml(name), daysLate, escapeHtml(title), dueDate);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
