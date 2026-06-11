package com.example.demo.services;

import com.example.demo.repositories.ClassReadingListItemRepository;
import com.example.demo.dto.admin.school.CreateSchoolRequest;
import com.example.demo.dto.admin.school.CreateSchoolResponse;
import com.example.demo.dto.admin.school.KlasListItem;
import com.example.demo.dto.admin.school.SchoolDashboardItemResponse;
import com.example.demo.dto.admin.school.SchoolDetailResponse;
import com.example.demo.dto.admin.school.UpdateSchoolInfoRequest;
import com.example.demo.dto.admin.user.AdminUserListItem;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.School;
import com.example.demo.entities.SchoolSettings;
import com.example.demo.entities.SchoolStatus;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.KlasRepository;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.repositories.SchoolRepository;
import com.example.demo.repositories.SchoolSettingsRepository;
import com.example.demo.repositories.WishlistRepository;
import java.util.Objects;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolAdminService {

    private final SchoolRepository schoolRepository;
    private final KlasRepository klasRepository;
    private final AppUserRepository appUserRepository;
    private final SchoolAdminValidationService schoolAdminValidationService;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final WishlistRepository wishlistRepository;
    private final ClassReadingListItemRepository classReadingListItemRepository;
    private final SchoolSettingsRepository schoolSettingsRepository;

    public SchoolAdminService(SchoolRepository schoolRepository,
            KlasRepository klasRepository,
            AppUserRepository appUserRepository,
            SchoolAdminValidationService schoolAdminValidationService,
            BookRepository bookRepository,
            LoanRepository loanRepository,
            WishlistRepository wishlistRepository,
            ClassReadingListItemRepository classReadingListItemRepository,
            SchoolSettingsRepository schoolSettingsRepository) {
        this.schoolRepository = schoolRepository;
        this.klasRepository = klasRepository;
        this.appUserRepository = appUserRepository;
        this.schoolAdminValidationService = schoolAdminValidationService;
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
        this.wishlistRepository = wishlistRepository;
        this.classReadingListItemRepository = classReadingListItemRepository;
        this.schoolSettingsRepository = schoolSettingsRepository;
    }

    @Transactional
    public CreateSchoolResponse addSchool(CreateSchoolRequest request) {
        String normalizedSubdomain = schoolAdminValidationService.normalizeSubdomain(request.getSubdomain());
        schoolAdminValidationService.assertSubdomainAvailable(normalizedSubdomain);

        School school = new School();
        school.setSubdomein(normalizedSubdomain);
        school.setSmartschoolUrl(schoolAdminValidationService.buildSmartschoolUrl(normalizedSubdomain));
        school.setNaam(request.getNaam() == null ? null : request.getNaam().trim());
        school.setAdres(request.getAdres() == null ? null : request.getAdres().trim());
        school.setLatitude(request.getLatitude());
        school.setLongitude(request.getLongitude());
        school.setStatus(SchoolStatus.PENDING);

        School saved = schoolRepository.save(school);
        ensureSettingsExist(saved);
        return toCreateResponse(saved);
    }

    private void ensureSettingsExist(School school) {
        if (!schoolSettingsRepository.existsById(school.getId())) {
            SchoolSettings settings = new SchoolSettings();
            settings.setSchool(school);
            schoolSettingsRepository.save(settings);
        }
    }

    @Transactional(readOnly = true)
    public List<SchoolDashboardItemResponse> getAllSchools() {
        return schoolRepository.findAll().stream()
                .map(this::toDashboardItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public SchoolDetailResponse getSchoolDetail(Long id) {
        Long resolvedId = Objects.requireNonNull(id, "schoolId is required");
        School school = schoolRepository.findById(resolvedId)
                .orElseThrow(() -> new ApiException("School niet gevonden", HttpStatus.NOT_FOUND, "SCHOOL_NOT_FOUND"));
        return toDetailResponse(school);
    }

    @Transactional
    public SchoolDetailResponse updateSchoolInfo(Long id, UpdateSchoolInfoRequest request) {
        Long resolvedId = Objects.requireNonNull(id, "schoolId is required");
        School school = schoolRepository.findById(resolvedId)
                .orElseThrow(() -> new ApiException("School niet gevonden", HttpStatus.NOT_FOUND, "SCHOOL_NOT_FOUND"));

        String naam = request.getNaam();
        school.setNaam(naam == null || naam.isBlank() ? null : naam.trim());

        String adres = request.getAdres();
        school.setAdres(adres == null || adres.isBlank() ? null : adres.trim());

        school.setLatitude(request.getLatitude());
        school.setLongitude(request.getLongitude());

        return toDetailResponse(schoolRepository.save(school));
    }

    @Transactional
    public SchoolDetailResponse updateSchoolStatus(Long schoolId, SchoolStatus status) {
        schoolAdminValidationService.validateUpdatableStatus(status);

        Long resolvedSchoolId = Objects.requireNonNull(schoolId, "schoolId is required");
        School school = schoolRepository.findById(resolvedSchoolId)
                .orElseThrow(() -> new ApiException("School niet gevonden", HttpStatus.NOT_FOUND, "SCHOOL_NOT_FOUND"));

        school.setStatus(status);
        return toDetailResponse(schoolRepository.save(school));
    }

    @Transactional(readOnly = true)
    public List<AdminUserListItem> getSchoolUsers(Long schoolId) {
        Long resolvedSchoolId = Objects.requireNonNull(schoolId, "schoolId is required");
        schoolRepository.findById(resolvedSchoolId)
                .orElseThrow(() -> new ApiException("School niet gevonden", HttpStatus.NOT_FOUND, "SCHOOL_NOT_FOUND"));
        return appUserRepository.findBySchool_Id(resolvedSchoolId).stream()
                .map(this::toUserListItem)
                .toList();
    }

    @Transactional
    public AdminUserListItem toggleUserActive(Long schoolId, Long userId) {
        Long resolvedSchoolId = Objects.requireNonNull(schoolId, "schoolId is required");
        Long resolvedUserId = Objects.requireNonNull(userId, "userId is required");
        AppUser user = appUserRepository.findById(resolvedUserId)
                .orElseThrow(() -> new ApiException("Gebruiker niet gevonden", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        if (user.getSchool() == null || !user.getSchool().getId().equals(resolvedSchoolId)) {
            throw new ApiException("Gebruiker behoort niet tot deze school", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }
        user.setActive(!user.isActive());
        return toUserListItem(appUserRepository.save(user));
    }

    @Transactional
    public AdminUserListItem setUserRole(Long schoolId, Long userId, String newRole) {
        Long resolvedSchoolId = Objects.requireNonNull(schoolId, "schoolId is required");
        Long resolvedUserId = Objects.requireNonNull(userId, "userId is required");
        AppUser user = appUserRepository.findById(resolvedUserId)
                .orElseThrow(() -> new ApiException("Gebruiker niet gevonden", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        if (user.getSchool() == null || !user.getSchool().getId().equals(resolvedSchoolId)) {
            throw new ApiException("Gebruiker behoort niet tot deze school", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }
        if (!"leerkracht".equals(newRole) && !"bibbeheerder".equals(newRole)) {
            throw new ApiException("Rol moet 'leerkracht' of 'bibbeheerder' zijn", HttpStatus.BAD_REQUEST,
                    "INVALID_ROLE");
        }
        if (!"leerkracht".equals(user.getRole()) && !"bibbeheerder".equals(user.getRole())) {
            throw new ApiException("Alleen leerkrachten en bibbeheerders kunnen van rol wisselen",
                    HttpStatus.BAD_REQUEST, "INVALID_ROLE_TRANSITION");
        }
        user.setRole(newRole);
        return toUserListItem(appUserRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<KlasListItem> getSchoolKlassen(Long schoolId) {
        Long resolvedSchoolId = Objects.requireNonNull(schoolId, "schoolId is required");
        schoolRepository.findById(resolvedSchoolId)
                .orElseThrow(() -> new ApiException("School niet gevonden", HttpStatus.NOT_FOUND, "SCHOOL_NOT_FOUND"));
        return klasRepository.findBySchool_Id(resolvedSchoolId).stream()
                .map(k -> {
                    KlasListItem item = new KlasListItem();
                    item.setId(k.getId());
                    item.setGroupId(k.getGroupId());
                    item.setNaam(k.getNaam());
                    return item;
                })
                .toList();
    }

    @Transactional
    public void deleteSchool(Long schoolId) {
        Long resolvedSchoolId = Objects.requireNonNull(schoolId, "schoolId is required");
        School school = schoolRepository.findById(resolvedSchoolId)
                .orElseThrow(() -> new ApiException("School niet gevonden", HttpStatus.NOT_FOUND, "SCHOOL_NOT_FOUND"));

        // Delete all related data in proper order to avoid foreign key violations
        // Get all books for this school first
        var allBooks = bookRepository.findAll();
        var booksInSchool = allBooks.stream()
                .filter(b -> b.getSchool() != null && b.getSchool().getId().equals(resolvedSchoolId))
                .toList();

        // 1. Delete class reading list items for this school
        var classReadingLists = classReadingListItemRepository.findBySchoolId(resolvedSchoolId);
        classReadingListItemRepository.deleteAll(classReadingLists);

        // 2. Delete wishlists for books in this school
        var allWishlists = wishlistRepository.findAll();
        var wishlistsToDelete = allWishlists.stream()
                .filter(w -> booksInSchool.stream().anyMatch(b -> b.getId().equals(w.getBook().getId())))
                .toList();
        wishlistRepository.deleteAll(wishlistsToDelete);

        // 3. Delete loans for books in this school
        var allLoans = loanRepository.findAll();
        var loansToDelete = allLoans.stream()
                .filter(l -> booksInSchool.stream().anyMatch(b -> b.getId().equals(l.getCopy().getBook().getId())))
                .toList();
        loanRepository.deleteAll(loansToDelete);

        // 4. Delete books in this school (this will cascade to book copies)
        bookRepository.deleteAll(booksInSchool);

        // 5. Delete classes in this school
        var klassen = klasRepository.findBySchool_Id(resolvedSchoolId);
        klasRepository.deleteAll(klassen);

        // 6. Delete users in this school
        var users = appUserRepository.findBySchool_Id(resolvedSchoolId);
        appUserRepository.deleteAll(users);

        // 7. Finally delete the school itself
        schoolRepository.delete(school);
    }

    private CreateSchoolResponse toCreateResponse(School school) {
        CreateSchoolResponse response = new CreateSchoolResponse();
        response.setId(school.getId());
        response.setSubdomain(school.getSubdomein());
        response.setSmartschoolUrl(school.getSmartschoolUrl());
        response.setStatus(school.getStatus());
        response.setCreatedAt(school.getAangemaaktOp());
        return response;
    }

    private SchoolDetailResponse toDetailResponse(School school) {
        SchoolDetailResponse r = new SchoolDetailResponse();
        r.setId(school.getId());
        r.setSubdomain(school.getSubdomein());
        r.setSmartschoolUrl(school.getSmartschoolUrl());
        r.setNaam(school.getNaam());
        r.setAdres(school.getAdres());
        r.setLatitude(school.getLatitude());
        r.setLongitude(school.getLongitude());
        r.setStatus(school.getStatus());
        r.setAangemaaktOp(school.getAangemaaktOp());
        Long schoolId = Objects.requireNonNull(school.getId(), "School id is required");
        r.setUserCount(appUserRepository.countBySchool_Id(schoolId));
        r.setKlasCount(klasRepository.countBySchool_Id(schoolId));
        r.setBookCount(bookRepository.countBySchool_Id(schoolId));
        r.setActiveLoansCount(loanRepository.countByCopy_Book_School_IdAndReturnedAtIsNull(schoolId));
        r.setWishlistCount(wishlistRepository.countByBook_School_Id(schoolId));
        r.setClassReadingListCount(classReadingListItemRepository.countBySchoolId(schoolId));
        return r;
    }

    private SchoolDashboardItemResponse toDashboardItem(School school) {
        SchoolDashboardItemResponse item = new SchoolDashboardItemResponse();
        Long schoolId = Objects.requireNonNull(school.getId(), "School id is required");
        item.setId(schoolId);
        item.setNaam(school.getNaam());
        item.setSubdomain(school.getSubdomein());
        item.setStatus(school.getStatus());
        item.setUserCount(appUserRepository.countBySchool_Id(schoolId));
        item.setKlasCount(klasRepository.countBySchool_Id(schoolId));
        return item;
    }

    private AdminUserListItem toUserListItem(AppUser user) {
        AdminUserListItem item = new AdminUserListItem();
        item.setId(user.getId());
        item.setSub(user.getSub());
        item.setRole(user.getRole());
        item.setKlasNaam(user.getKlas() != null ? user.getKlas().getNaam() : null);
        item.setActive(user.isActive());
        return item;
    }
}
