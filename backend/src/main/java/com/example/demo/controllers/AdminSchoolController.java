package com.example.demo.controllers;

import com.example.demo.dto.admin.school.CreateSchoolRequest;
import com.example.demo.dto.admin.school.CreateSchoolResponse;
import com.example.demo.dto.admin.school.KlasListItem;
import com.example.demo.dto.admin.school.SchoolDashboardItemResponse;
import com.example.demo.dto.admin.school.SchoolDetailResponse;
import com.example.demo.dto.admin.school.UpdateSchoolInfoRequest;
import com.example.demo.dto.admin.school.UpdateSchoolStatusRequest;
import com.example.demo.dto.admin.user.AdminUserListItem;
import com.example.demo.services.SchoolAdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/schools")
public class AdminSchoolController {

    private final SchoolAdminService schoolAdminService;

    public AdminSchoolController(SchoolAdminService schoolAdminService) {
        this.schoolAdminService = schoolAdminService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public CreateSchoolResponse createSchool(@Valid @RequestBody CreateSchoolRequest request) {
        return schoolAdminService.addSchool(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<SchoolDashboardItemResponse> getSchools() {
        return schoolAdminService.getAllSchools();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SchoolDetailResponse getSchoolDetail(@PathVariable Long id) {
        return schoolAdminService.getSchoolDetail(id);
    }

    @PatchMapping("/{id}/info")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SchoolDetailResponse updateSchoolInfo(@PathVariable Long id,
            @Valid @RequestBody UpdateSchoolInfoRequest request) {
        return schoolAdminService.updateSchoolInfo(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SchoolDetailResponse updateSchoolStatus(@PathVariable Long id,
            @Valid @RequestBody UpdateSchoolStatusRequest request) {
        return schoolAdminService.updateSchoolStatus(id, request.getStatus());
    }

    @GetMapping("/{id}/users")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<AdminUserListItem> getSchoolUsers(@PathVariable Long id) {
        return schoolAdminService.getSchoolUsers(id);
    }

    @PatchMapping("/{id}/users/{userId}/active")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public AdminUserListItem toggleUserActive(@PathVariable Long id, @PathVariable Long userId) {
        return schoolAdminService.toggleUserActive(id, userId);
    }

    @GetMapping("/{id}/klassen")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<KlasListItem> getSchoolKlassen(@PathVariable Long id) {
        return schoolAdminService.getSchoolKlassen(id);
    }
}
