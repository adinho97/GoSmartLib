package com.example.demo.services;

import com.example.demo.dto.admin.school.CreateSchoolRequest;
import com.example.demo.dto.admin.school.CreateSchoolResponse;
import com.example.demo.dto.admin.school.KlasListItem;
import com.example.demo.dto.admin.school.SchoolDashboardItemResponse;
import com.example.demo.dto.admin.school.SchoolDetailResponse;
import com.example.demo.dto.admin.school.UpdateSchoolInfoRequest;
import com.example.demo.dto.admin.user.AdminUserListItem;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.School;
import com.example.demo.entities.SchoolStatus;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.KlasRepository;
import com.example.demo.repositories.SchoolRepository;
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

    public SchoolAdminService(SchoolRepository schoolRepository,
            KlasRepository klasRepository,
            AppUserRepository appUserRepository,
            SchoolAdminValidationService schoolAdminValidationService) {
        this.schoolRepository = schoolRepository;
        this.klasRepository = klasRepository;
        this.appUserRepository = appUserRepository;
        this.schoolAdminValidationService = schoolAdminValidationService;
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
        return toCreateResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SchoolDashboardItemResponse> getAllSchools() {
        return schoolRepository.findAll().stream()
                .map(this::toDashboardItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public SchoolDetailResponse getSchoolDetail(Long id) {
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new ApiException("School niet gevonden", HttpStatus.NOT_FOUND, "SCHOOL_NOT_FOUND"));
        return toDetailResponse(school);
    }

    @Transactional
    public SchoolDetailResponse updateSchoolInfo(Long id, UpdateSchoolInfoRequest request) {
        School school = schoolRepository.findById(id)
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

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ApiException("School niet gevonden", HttpStatus.NOT_FOUND, "SCHOOL_NOT_FOUND"));

        school.setStatus(status);
        return toDetailResponse(schoolRepository.save(school));
    }

    @Transactional(readOnly = true)
    public List<AdminUserListItem> getSchoolUsers(Long schoolId) {
        schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ApiException("School niet gevonden", HttpStatus.NOT_FOUND, "SCHOOL_NOT_FOUND"));
        return appUserRepository.findBySchool_Id(schoolId).stream()
                .map(this::toUserListItem)
                .toList();
    }

    @Transactional
    public AdminUserListItem toggleUserActive(Long schoolId, Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ApiException("Gebruiker niet gevonden", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        if (user.getSchool() == null || !user.getSchool().getId().equals(schoolId)) {
            throw new ApiException("Gebruiker behoort niet tot deze school", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }
        user.setActive(!user.isActive());
        return toUserListItem(appUserRepository.save(user));
    }

    @Transactional
    public AdminUserListItem setUserRole(Long schoolId, Long userId, String newRole) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ApiException("Gebruiker niet gevonden", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        if (user.getSchool() == null || !user.getSchool().getId().equals(schoolId)) {
            throw new ApiException("Gebruiker behoort niet tot deze school", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }
        if (!"leerkracht".equals(newRole) && !"bibbeheerder".equals(newRole)) {
            throw new ApiException("Rol moet 'leerkracht' of 'bibbeheerder' zijn", HttpStatus.BAD_REQUEST, "INVALID_ROLE");
        }
        if (!"leerkracht".equals(user.getRole()) && !"bibbeheerder".equals(user.getRole())) {
            throw new ApiException("Alleen leerkrachten en bibbeheerders kunnen van rol wisselen", HttpStatus.BAD_REQUEST, "INVALID_ROLE_TRANSITION");
        }
        user.setRole(newRole);
        return toUserListItem(appUserRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<KlasListItem> getSchoolKlassen(Long schoolId) {
        schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ApiException("School niet gevonden", HttpStatus.NOT_FOUND, "SCHOOL_NOT_FOUND"));
        return klasRepository.findBySchool_Id(schoolId).stream()
                .map(k -> {
                    KlasListItem item = new KlasListItem();
                    item.setId(k.getId());
                    item.setGroupId(k.getGroupId());
                    item.setNaam(k.getNaam());
                    return item;
                })
                .toList();
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
        r.setUserCount(appUserRepository.countBySchool_Id(school.getId()));
        r.setKlasCount(klasRepository.countBySchool_Id(school.getId()));
        return r;
    }

    private SchoolDashboardItemResponse toDashboardItem(School school) {
        SchoolDashboardItemResponse item = new SchoolDashboardItemResponse();
        item.setId(school.getId());
        item.setNaam(school.getNaam());
        item.setSubdomain(school.getSubdomein());
        item.setStatus(school.getStatus());
        item.setUserCount(appUserRepository.countBySchool_Id(school.getId()));
        item.setKlasCount(klasRepository.countBySchool_Id(school.getId()));
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
