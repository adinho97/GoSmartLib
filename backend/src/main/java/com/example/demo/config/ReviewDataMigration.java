package com.example.demo.config;

import com.example.demo.entities.AppUser;
import com.example.demo.entities.Review;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Data migration to populate reviewerUserSub and reviewerUserId for existing reviews.
 * This ensures that old reviews created before these fields were properly tracked
 * can still be managed (edited/deleted) by their creators.
 */
@Component
public class ReviewDataMigration implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ReviewDataMigration.class);

    private final ReviewRepository reviewRepository;
    private final AppUserRepository appUserRepository;

    public ReviewDataMigration(ReviewRepository reviewRepository,
                               AppUserRepository appUserRepository) {
        this.reviewRepository = reviewRepository;
        this.appUserRepository = appUserRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting review data migration...");
        
        // Find all reviews with missing reviewerUserSub or reviewerUserId
        List<Review> reviews = reviewRepository.findAll();
        int migrated = 0;
        int skipped = 0;
        
        for (Review review : reviews) {
            // Skip if already has both fields populated
            if (review.getReviewerUserSub() != null && review.getReviewerUserId() != null && !review.getAnonymous()) {
                skipped++;
                continue;
            }
            
            // Skip anonymous reviews with reviewerUserSub (they should only have sub, not ID)
            if (Boolean.TRUE.equals(review.getAnonymous())) {
                if (review.getReviewerUserSub() != null) {
                    skipped++;
                    continue;
                }
                // Anonymous reviews without sub - try to get any user as fallback (shouldn't happen)
                skipped++;
                continue;
            }
            
            // For non-anonymous reviews, try to find the user
            // Since we don't have the original user info, we'll at least ensure reviewerUserSub is set
            if (review.getReviewerUserSub() == null) {
                logger.debug("Review {} is missing reviewerUserSub. Cannot auto-migrate without original user context.", review.getId());
                skipped++;
                continue;
            }
            
            // Try to resolve the user ID from the sub
            if (review.getReviewerUserId() == null && review.getReviewerUserSub() != null) {
                try {
                    String sub = review.getReviewerUserSub();
                    Optional<AppUser> userOpt = appUserRepository.findBySub(sub);
                    if (userOpt.isPresent()) {
                        review.setReviewerUserId(userOpt.get().getId());
                        reviewRepository.save(review);
                        migrated++;
                        logger.debug("Migrated review {} - set reviewerUserId to {}", review.getId(), userOpt.get().getId());
                    } else {
                        logger.debug("Could not find user with sub {} for review {}", sub, review.getId());
                        skipped++;
                    }
                } catch (Exception e) {
                    logger.error("Error migrating review {}", review.getId(), e);
                    skipped++;
                }
            } else {
                skipped++;
            }
        }
        
        logger.info("Review data migration completed. Migrated: {}, Skipped: {}, Total: {}", 
                    migrated, skipped, reviews.size());
    }
}
