package com.example.demo;

import com.example.demo.entities.AppUser;
import com.example.demo.entities.Klas;
import com.example.demo.entities.Leeslijst;
import com.example.demo.entities.Loan;
import com.example.demo.entities.Review;
import com.example.demo.entities.UserDashboardConfig;
import com.example.demo.entities.UserExperience;
import com.example.demo.entities.UserPreference;
import com.example.demo.entities.Wishlist;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.LeeslijstRepository;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.repositories.ReviewRepository;
import com.example.demo.repositories.UserDashboardConfigRepository;
import com.example.demo.repositories.UserExperienceRepository;
import com.example.demo.repositories.UserPreferenceRepository;
import com.example.demo.repositories.WishlistRepository;
import com.example.demo.services.RetentionPurgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
@DisplayName("RetentionPurgeService Tests")
class RetentionPurgeServiceTest {

    private AppUserRepository appUserRepository;
    private WishlistRepository wishlistRepository;
    private UserPreferenceRepository userPreferenceRepository;
    private UserExperienceRepository userExperienceRepository;
    private UserDashboardConfigRepository userDashboardConfigRepository;
    private LeeslijstRepository leeslijstRepository;
    private LoanRepository loanRepository;
    private ReviewRepository reviewRepository;
    private RetentionPurgeService selfProxy;

    private RetentionPurgeService service;

    @BeforeEach
    void setUp() {
        appUserRepository = mock(AppUserRepository.class);
        wishlistRepository = mock(WishlistRepository.class);
        userPreferenceRepository = mock(UserPreferenceRepository.class);
        userExperienceRepository = mock(UserExperienceRepository.class);
        userDashboardConfigRepository = mock(UserDashboardConfigRepository.class);
        leeslijstRepository = mock(LeeslijstRepository.class);
        loanRepository = mock(LoanRepository.class);
        reviewRepository = mock(ReviewRepository.class);
        selfProxy = mock(RetentionPurgeService.class);

        service = new RetentionPurgeService(appUserRepository, wishlistRepository,
                userPreferenceRepository, userExperienceRepository,
                userDashboardConfigRepository, leeslijstRepository,
                loanRepository, reviewRepository, selfProxy);

        // Route self.purgeUser(id) calls (from runDailyPurge) to the real method,
        // mirroring the Spring proxy semantics in unit tests.
        doAnswer(inv -> {
            service.purgeUser(inv.getArgument(0));
            return null;
        }).when(selfProxy).purgeUser(anyLong());
    }

    // ------------------------ purgeUser: PII + state ------------------------

    @Test
    @DisplayName("purgeUser: anonymizes sub, nulls tokens, klas, sets active=false and dataPurgedAt")
    void purgeUser_anonymizesAppUserFields() {
        AppUser user = departedUser(100L, "ORIGINAL_SUB");
        user.setAccessToken("access");
        user.setSmartschoolRefreshToken("refresh");
        user.setKlas(new Klas());
        user.setActive(true);

        when(appUserRepository.findById(100L)).thenReturn(Optional.of(user));
        when(wishlistRepository.findByUser(user)).thenReturn(List.of());
        when(userPreferenceRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(List.of());
        when(userExperienceRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(Optional.empty());
        when(userDashboardConfigRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(Optional.empty());
        when(loanRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(List.of());
        when(leeslijstRepository.findByAssignedUsers(100L)).thenReturn(List.of());
        when(leeslijstRepository.findByCreatedBy_Id(100L)).thenReturn(List.of());
        when(reviewRepository.findByReviewerUserSubOrReviewerUserId("ORIGINAL_SUB", 100L))
                .thenReturn(List.of());

        service.purgeUser(100L);

        assertTrue(user.getSub().startsWith("purged-"),
                "sub must be replaced with purged-<uuid>");
        assertFalse("ORIGINAL_SUB".equals(user.getSub()),
                "original sub must not be readable post-purge");
        assertNull(user.getAccessToken());
        assertNull(user.getSmartschoolRefreshToken());
        assertNull(user.getKlas());
        assertFalse(user.isActive());
        assertNotNull(user.getDataPurgedAt(), "dataPurgedAt must be set");
        verify(appUserRepository).save(user);
    }

    @Test
    @DisplayName("purgeUser: already-purged user is a no-op")
    void purgeUser_alreadyPurgedShortCircuits() {
        AppUser user = departedUser(100L, "ORIGINAL_SUB");
        user.setDataPurgedAt(LocalDateTime.now().minusDays(1));

        when(appUserRepository.findById(100L)).thenReturn(Optional.of(user));

        service.purgeUser(100L);

        verify(appUserRepository, never()).save(any());
        verify(wishlistRepository, never()).deleteAll(any());
        verify(loanRepository, never()).saveAll(any());
        verify(reviewRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("purgeUser: missing user is a no-op")
    void purgeUser_missingUserIsNoOp() {
        when(appUserRepository.findById(999L)).thenReturn(Optional.empty());

        service.purgeUser(999L);

        verify(appUserRepository, never()).save(any());
    }

    // ------------------------ purgeUser: cascading data ------------------------

    @Test
    @DisplayName("purgeUser: wishlists are deleted, preferences/experience/dashboard removed by sub")
    void purgeUser_personalDataDeleted() {
        AppUser user = departedUser(100L, "ORIGINAL_SUB");
        Wishlist w1 = new Wishlist();
        Wishlist w2 = new Wishlist();
        UserPreference pref = new UserPreference("ORIGINAL_SUB", "k", true);
        UserExperience xp = new UserExperience();
        UserDashboardConfig cfg = new UserDashboardConfig();

        when(appUserRepository.findById(100L)).thenReturn(Optional.of(user));
        when(wishlistRepository.findByUser(user)).thenReturn(List.of(w1, w2));
        when(userPreferenceRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(List.of(pref));
        when(userExperienceRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(Optional.of(xp));
        when(userDashboardConfigRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(Optional.of(cfg));
        when(loanRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(List.of());
        when(leeslijstRepository.findByAssignedUsers(100L)).thenReturn(List.of());
        when(leeslijstRepository.findByCreatedBy_Id(100L)).thenReturn(List.of());
        when(reviewRepository.findByReviewerUserSubOrReviewerUserId("ORIGINAL_SUB", 100L))
                .thenReturn(List.of());

        service.purgeUser(100L);

        verify(wishlistRepository).deleteAll(List.of(w1, w2));
        verify(userPreferenceRepository).deleteAll(List.of(pref));
        verify(userExperienceRepository).delete(xp);
        verify(userDashboardConfigRepository).delete(cfg);
    }

    @Test
    @DisplayName("purgeUser: loans get one shared anon-<uuid> sub (preserves per-user grouping)")
    void purgeUser_loansShareOneAnonSub() {
        AppUser user = departedUser(100L, "ORIGINAL_SUB");
        Loan l1 = new Loan();
        l1.setUserSub("ORIGINAL_SUB");
        Loan l2 = new Loan();
        l2.setUserSub("ORIGINAL_SUB");
        Loan l3 = new Loan();
        l3.setUserSub("ORIGINAL_SUB");

        when(appUserRepository.findById(100L)).thenReturn(Optional.of(user));
        when(wishlistRepository.findByUser(user)).thenReturn(List.of());
        when(userPreferenceRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(List.of());
        when(userExperienceRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(Optional.empty());
        when(userDashboardConfigRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(Optional.empty());
        when(loanRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(new ArrayList<>(List.of(l1, l2, l3)));
        when(leeslijstRepository.findByAssignedUsers(100L)).thenReturn(List.of());
        when(leeslijstRepository.findByCreatedBy_Id(100L)).thenReturn(List.of());
        when(reviewRepository.findByReviewerUserSubOrReviewerUserId("ORIGINAL_SUB", 100L))
                .thenReturn(List.of());

        service.purgeUser(100L);

        String shared = l1.getUserSub();
        assertTrue(shared.startsWith("anon-"), "loans must be rewritten to anon-<uuid>");
        assertFalse("ORIGINAL_SUB".equals(shared));
        assertEquals(shared, l2.getUserSub(), "all loans for this user share one anon sub");
        assertEquals(shared, l3.getUserSub());
        verify(loanRepository).saveAll(List.of(l1, l2, l3));
    }

    @Test
    @DisplayName("purgeUser: reviews are anonymized (anonymous=true, reviewer fields nulled)")
    void purgeUser_reviewsAnonymized() {
        AppUser user = departedUser(100L, "ORIGINAL_SUB");
        Review r = new Review();
        r.setId(7L);
        r.setReviewerUserSub("ORIGINAL_SUB");
        r.setReviewerUserId(100L);
        r.setAnonymous(false);

        when(appUserRepository.findById(100L)).thenReturn(Optional.of(user));
        when(wishlistRepository.findByUser(user)).thenReturn(List.of());
        when(userPreferenceRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(List.of());
        when(userExperienceRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(Optional.empty());
        when(userDashboardConfigRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(Optional.empty());
        when(loanRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(List.of());
        when(leeslijstRepository.findByAssignedUsers(100L)).thenReturn(List.of());
        when(leeslijstRepository.findByCreatedBy_Id(100L)).thenReturn(List.of());
        when(reviewRepository.findByReviewerUserSubOrReviewerUserId("ORIGINAL_SUB", 100L))
                .thenReturn(new ArrayList<>(List.of(r)));

        service.purgeUser(100L);

        assertTrue(r.getAnonymous(), "review.anonymous must be set to true");
        assertNull(r.getReviewerUserSub());
        assertNull(r.getReviewerUserId());
        verify(reviewRepository).saveAll(List.of(r));
    }

    @Test
    @DisplayName("purgeUser: user removed from assignedUsers of leeslijsten they were assigned to")
    void purgeUser_leeslijstAssignedUsersTrimmed() {
        AppUser user = departedUser(100L, "ORIGINAL_SUB");

        AppUser other = new AppUser();
        other.setId(200L);

        Leeslijst list = new Leeslijst();
        Set<AppUser> assigned = new HashSet<>();
        assigned.add(user);
        assigned.add(other);
        list.setAssignedUsers(assigned);

        when(appUserRepository.findById(100L)).thenReturn(Optional.of(user));
        when(wishlistRepository.findByUser(user)).thenReturn(List.of());
        when(userPreferenceRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(List.of());
        when(userExperienceRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(Optional.empty());
        when(userDashboardConfigRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(Optional.empty());
        when(loanRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(List.of());
        when(leeslijstRepository.findByAssignedUsers(100L)).thenReturn(new ArrayList<>(List.of(list)));
        when(leeslijstRepository.findByCreatedBy_Id(100L)).thenReturn(List.of());
        when(reviewRepository.findByReviewerUserSubOrReviewerUserId("ORIGINAL_SUB", 100L))
                .thenReturn(List.of());

        service.purgeUser(100L);

        assertEquals(1, list.getAssignedUsers().size(),
                "purged user must be removed from assignedUsers set");
        assertTrue(list.getAssignedUsers().contains(other),
                "other assigned users must remain");
        verify(leeslijstRepository).saveAll(List.of(list));
    }

    @Test
    @DisplayName("purgeUser: createdByName cleared on leeslijsten authored by the purged user")
    void purgeUser_authoredLeeslijstNameCleared() {
        AppUser user = departedUser(100L, "ORIGINAL_SUB");
        Leeslijst authored = new Leeslijst();
        authored.setCreatedByName("Jan De Vries");

        when(appUserRepository.findById(100L)).thenReturn(Optional.of(user));
        when(wishlistRepository.findByUser(user)).thenReturn(List.of());
        when(userPreferenceRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(List.of());
        when(userExperienceRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(Optional.empty());
        when(userDashboardConfigRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(Optional.empty());
        when(loanRepository.findByUserSub("ORIGINAL_SUB")).thenReturn(List.of());
        when(leeslijstRepository.findByAssignedUsers(100L)).thenReturn(List.of());
        when(leeslijstRepository.findByCreatedBy_Id(100L)).thenReturn(new ArrayList<>(List.of(authored)));
        when(reviewRepository.findByReviewerUserSubOrReviewerUserId("ORIGINAL_SUB", 100L))
                .thenReturn(List.of());

        service.purgeUser(100L);

        assertNull(authored.getCreatedByName(),
                "authored leeslijst must have createdByName nulled");
        verify(leeslijstRepository).saveAll(List.of(authored));
    }

    @Test
    @DisplayName("purgeUser: user with no sub still purges (skips sub-keyed cleanups gracefully)")
    void purgeUser_blankSubSkipsSubKeyedDeletes() {
        AppUser user = departedUser(100L, null);

        when(appUserRepository.findById(100L)).thenReturn(Optional.of(user));
        when(wishlistRepository.findByUser(user)).thenReturn(List.of());
        when(leeslijstRepository.findByAssignedUsers(100L)).thenReturn(List.of());
        when(leeslijstRepository.findByCreatedBy_Id(100L)).thenReturn(List.of());
        when(reviewRepository.findByReviewerUserSubOrReviewerUserId(null, 100L))
                .thenReturn(List.of());

        service.purgeUser(100L);

        verify(userPreferenceRepository, never()).findByUserSub(any());
        verify(loanRepository, never()).findByUserSub(any());
        verify(appUserRepository).save(user);
        assertNotNull(user.getDataPurgedAt());
    }

    // ------------------------ runDailyPurge: orchestration ------------------------

    @Test
    @DisplayName("runDailyPurge: selects users with departedAt older than 90 days and dataPurgedAt null")
    void runDailyPurge_picksDueUsers() {
        AppUser due = departedUser(100L, "DUE_SUB");
        AppUser otherDue = departedUser(101L, "DUE_SUB_2");

        // The service computes the threshold itself; here we just stand-in any list.
        when(appUserRepository.findByDepartedAtBeforeAndDataPurgedAtIsNull(any()))
                .thenReturn(List.of(due, otherDue));
        stubEmptyForUser(due);
        stubEmptyForUser(otherDue);

        service.runDailyPurge();

        verify(selfProxy).purgeUser(100L);
        verify(selfProxy).purgeUser(101L);
    }

    @Test
    @DisplayName("runDailyPurge: passes a threshold ~90 days in the past")
    void runDailyPurge_thresholdIsNinetyDays() {
        when(appUserRepository.findByDepartedAtBeforeAndDataPurgedAtIsNull(any()))
                .thenReturn(List.of());

        LocalDateTime before = LocalDateTime.now().minusDays(90);
        service.runDailyPurge();
        LocalDateTime after = LocalDateTime.now().minusDays(90);

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(appUserRepository).findByDepartedAtBeforeAndDataPurgedAtIsNull(captor.capture());
        LocalDateTime threshold = captor.getValue();

        assertTrue(!threshold.isBefore(before) && !threshold.isAfter(after),
                "threshold must be approx now()-90d (was " + threshold + ")");
    }

    @Test
    @DisplayName("runDailyPurge: per-user exception does not abort the batch")
    void runDailyPurge_failureOnOneUserDoesNotBlockOthers() {
        AppUser badUser = departedUser(100L, "BAD_SUB");
        AppUser goodUser = departedUser(101L, "GOOD_SUB");

        when(appUserRepository.findByDepartedAtBeforeAndDataPurgedAtIsNull(any()))
                .thenReturn(List.of(badUser, goodUser));

        // Override selfProxy: throw for badUser, route goodUser to the real method.
        doAnswer(inv -> {
            Long id = inv.getArgument(0);
            if (id.equals(100L)) {
                throw new RuntimeException("boom");
            }
            service.purgeUser(id);
            return null;
        }).when(selfProxy).purgeUser(anyLong());

        stubEmptyForUser(goodUser);

        service.runDailyPurge();

        // goodUser still got purged despite badUser blowing up.
        assertNotNull(goodUser.getDataPurgedAt(),
                "good user must still be purged after a sibling failure");
        verify(selfProxy).purgeUser(100L);
        verify(selfProxy).purgeUser(101L);
    }

    @Test
    @DisplayName("runDailyPurge: empty due list is a no-op")
    void runDailyPurge_noDueUsersIsNoOp() {
        when(appUserRepository.findByDepartedAtBeforeAndDataPurgedAtIsNull(any()))
                .thenReturn(List.of());

        service.runDailyPurge();

        verify(selfProxy, never()).purgeUser(anyLong());
    }

    // ------------------------ helpers ------------------------

    private AppUser departedUser(Long id, String sub) {
        AppUser u = new AppUser();
        u.setId(id);
        u.setSub(sub);
        u.setDepartedAt(LocalDateTime.now().minusDays(120));
        return u;
    }

    private void stubEmptyForUser(AppUser u) {
        when(appUserRepository.findById(u.getId())).thenReturn(Optional.of(u));
        when(wishlistRepository.findByUser(u)).thenReturn(List.of());
        String sub = u.getSub();
        if (sub != null && !sub.isBlank()) {
            when(userPreferenceRepository.findByUserSub(sub)).thenReturn(List.of());
            when(userExperienceRepository.findByUserSub(sub)).thenReturn(Optional.empty());
            when(userDashboardConfigRepository.findByUserSub(sub)).thenReturn(Optional.empty());
            when(loanRepository.findByUserSub(sub)).thenReturn(List.of());
        }
        when(leeslijstRepository.findByAssignedUsers(u.getId())).thenReturn(List.of());
        when(leeslijstRepository.findByCreatedBy_Id(u.getId())).thenReturn(List.of());
        when(reviewRepository.findByReviewerUserSubOrReviewerUserId(sub, u.getId()))
                .thenReturn(List.of());
    }
}
