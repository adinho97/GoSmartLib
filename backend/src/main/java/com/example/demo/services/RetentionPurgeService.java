package com.example.demo.services;

import com.example.demo.entities.AppUser;
import com.example.demo.entities.Leeslijst;
import com.example.demo.entities.Loan;
import com.example.demo.entities.Review;
import com.example.demo.entities.Wishlist;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.LeeslijstRepository;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.repositories.ReviewRepository;
import com.example.demo.repositories.UserDashboardConfigRepository;
import com.example.demo.repositories.UserExperienceRepository;
import com.example.demo.repositories.UserPreferenceRepository;
import com.example.demo.repositories.WishlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RetentionPurgeService {

    private static final Logger logger = LoggerFactory.getLogger(RetentionPurgeService.class);
    private static final int RETENTION_DAYS = 90;

    private final AppUserRepository appUserRepository;
    private final WishlistRepository wishlistRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserExperienceRepository userExperienceRepository;
    private final UserDashboardConfigRepository userDashboardConfigRepository;
    private final LeeslijstRepository leeslijstRepository;
    private final LoanRepository loanRepository;
    private final ReviewRepository reviewRepository;
    private final RetentionPurgeService self;

    public RetentionPurgeService(AppUserRepository appUserRepository,
            WishlistRepository wishlistRepository,
            UserPreferenceRepository userPreferenceRepository,
            UserExperienceRepository userExperienceRepository,
            UserDashboardConfigRepository userDashboardConfigRepository,
            LeeslijstRepository leeslijstRepository,
            LoanRepository loanRepository,
            ReviewRepository reviewRepository,
            @Lazy RetentionPurgeService self) {
        this.appUserRepository = appUserRepository;
        this.wishlistRepository = wishlistRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.userExperienceRepository = userExperienceRepository;
        this.userDashboardConfigRepository = userDashboardConfigRepository;
        this.leeslijstRepository = leeslijstRepository;
        this.loanRepository = loanRepository;
        this.reviewRepository = reviewRepository;
        this.self = self;
    }

    @Scheduled(cron = "0 0 2 * * ?", zone = "Europe/Brussels")
    public void runDailyPurge() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);
        List<AppUser> due = appUserRepository
                .findByDepartedAtBeforeAndDataPurgedAtIsNull(threshold);
        if (due.isEmpty()) {
            logger.info("Retention purge: no users past {}-day window", RETENTION_DAYS);
            return;
        }
        logger.info("Retention purge: {} user(s) past {}-day window", due.size(), RETENTION_DAYS);

        int purged = 0;
        int failed = 0;
        for (AppUser user : due) {
            try {
                self.purgeUser(user.getId());
                purged++;
            } catch (org.springframework.dao.DataAccessException e) {
                failed++;
                logger.error("Retention purge DB error for user id={}: {}", user.getId(), e.getMessage(), e);
            } catch (RuntimeException e) {
                failed++;
                logger.error("Retention purge failed for user id={} ({}): {}",
                        user.getId(), e.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
        logger.info("Retention purge done: purged={}, failed={}", purged, failed);
    }

    @Transactional
    public void purgeUser(Long userId) {
        AppUser user = appUserRepository.findById(userId).orElse(null);
        if (user == null || user.getDataPurgedAt() != null) {
            return;
        }
        String originalSub = user.getSub();

        List<Wishlist> wishlists = wishlistRepository.findByUser(user);
        if (!wishlists.isEmpty()) {
            wishlistRepository.deleteAll(wishlists);
        }

        int loansAnonymized = 0;
        if (StringUtils.hasText(originalSub)) {
            userPreferenceRepository.deleteAll(
                    userPreferenceRepository.findByUserSub(originalSub));

            userExperienceRepository.findByUserSub(originalSub)
                    .ifPresent(userExperienceRepository::delete);

            userDashboardConfigRepository.findByUserSub(originalSub)
                    .ifPresent(userDashboardConfigRepository::delete);

            List<Loan> loans = loanRepository.findByUserSub(originalSub);
            String anonSub = "anon-" + UUID.randomUUID();
            for (Loan loan : loans) {
                loan.setUserSub(anonSub);
            }
            if (!loans.isEmpty()) {
                loanRepository.saveAll(loans);
            }
            loansAnonymized = loans.size();
        }

        List<Leeslijst> assignedTo = leeslijstRepository.findByAssignedUsers(userId);
        for (Leeslijst list : assignedTo) {
            list.getAssignedUsers().removeIf(u -> userId.equals(u.getId()));
        }
        if (!assignedTo.isEmpty()) {
            leeslijstRepository.saveAll(assignedTo);
        }

        List<Leeslijst> authoredBy = leeslijstRepository.findByCreatedBy_Id(userId);
        for (Leeslijst list : authoredBy) {
            list.setCreatedByName(null);
        }
        if (!authoredBy.isEmpty()) {
            leeslijstRepository.saveAll(authoredBy);
        }

        List<Review> reviews = reviewRepository
                .findByReviewerUserSubOrReviewerUserId(originalSub, userId);
        for (Review review : reviews) {
            review.setAnonymous(true);
            review.setReviewerUserSub(null);
            review.setReviewerUserId(null);
        }
        if (!reviews.isEmpty()) {
            reviewRepository.saveAll(reviews);
        }

        user.setSub("purged-" + UUID.randomUUID());
        user.setAccessToken(null);
        user.setSmartschoolRefreshToken(null);
        user.setKlas(null);
        user.setActive(false);
        user.setDataPurgedAt(LocalDateTime.now());
        appUserRepository.save(user);

        logger.info("Purged user id={} (originalSub anonymized): "
                + "wishlists={}, loans-anonymized={}, reviews-anonymized={}, "
                + "leeslisten-unassigned={}, leeslisten-authored-namecleared={}",
                userId,
                wishlists.size(),
                loansAnonymized,
                reviews.size(),
                assignedTo.size(),
                authoredBy.size());
    }
}
