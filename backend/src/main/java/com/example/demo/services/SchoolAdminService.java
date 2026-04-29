package com.example.demo.services;

import com.example.demo.dto.admin.school.CreateSchoolRequest;
import com.example.demo.dto.admin.school.CreateSchoolResponse;
import com.example.demo.dto.admin.school.SchoolDashboardItemResponse;
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

    @Transactional
    public SchoolDashboardItemResponse updateSchoolStatus(Long schoolId, SchoolStatus status) {
        schoolAdminValidationService.validateUpdatableStatus(status);

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ApiException("School niet gevonden", HttpStatus.NOT_FOUND, "SCHOOL_NOT_FOUND"));

        school.setStatus(status);
        School saved = schoolRepository.save(school);
        return toDashboardItem(saved);
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

    private SchoolDashboardItemResponse toDashboardItem(School school) {
        SchoolDashboardItemResponse item = new SchoolDashboardItemResponse();
        item.setId(school.getId());
        item.setNaam(school.getNaam());
        item.setSubdomain(school.getSubdomein());
        item.setStatus(school.getStatus());
        item.setUserCount(appUserRepository.countByPlatformIgnoreCase(school.getSmartschoolUrl()));
        item.setKlasCount(klasRepository.countBySchool_Id(school.getId()));
        return item;
    }
}
