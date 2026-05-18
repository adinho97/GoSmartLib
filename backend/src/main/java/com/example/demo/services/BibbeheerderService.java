package com.example.demo.services;

import com.example.demo.dto.admin.school.KlasListItem;
import com.example.demo.dto.admin.user.AdminUserListItem;
import com.example.demo.entities.AppUser;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.KlasRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BibbeheerderService {

    private final AppUserRepository appUserRepository;
    private final KlasRepository klasRepository;

    public BibbeheerderService(AppUserRepository appUserRepository, KlasRepository klasRepository) {
        this.appUserRepository = appUserRepository;
        this.klasRepository = klasRepository;
    }

    @Transactional(readOnly = true)
    public Long getCallerSchoolId(String callerSub) {
        return Objects.requireNonNull(resolveCaller(callerSub).getSchool().getId(), "schoolId is required");
    }

    @Transactional(readOnly = true)
    public List<AdminUserListItem> getLeerkrachtenInOwnSchool(String callerSub) {
        AppUser caller = resolveCaller(callerSub);
        Long schoolId = Objects.requireNonNull(caller.getSchool().getId(), "schoolId is required");
        return appUserRepository.findBySchool_IdAndRole(schoolId, "leerkracht")
                .stream()
                .map(this::toUserListItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminUserListItem> getAllUsersInOwnSchool(String callerSub) {
        AppUser caller = resolveCaller(callerSub);
        Long schoolId = Objects.requireNonNull(caller.getSchool().getId(), "schoolId is required");
        return appUserRepository.findBySchool_Id(schoolId).stream()
                .map(this::toUserListItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<KlasListItem> getKlassenInOwnSchool(String callerSub) {
        AppUser caller = resolveCaller(callerSub);
        Long schoolId = Objects.requireNonNull(caller.getSchool().getId(), "schoolId is required");
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

    @Transactional
    public AdminUserListItem promoteLeerkrachtToBibbeheerder(String callerSub, Long targetUserId) {
        AppUser caller = resolveCaller(callerSub);
        Long callerSchoolId = Objects.requireNonNull(caller.getSchool().getId(), "schoolId is required");

        AppUser target = appUserRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException("Gebruiker niet gevonden", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        if (target.getSchool() == null || !target.getSchool().getId().equals(callerSchoolId)) {
            throw new ApiException("Gebruiker behoort niet tot uw school", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }

        if (!"leerkracht".equals(target.getRole())) {
            throw new ApiException("Alleen leerkrachten kunnen worden gepromoveerd", HttpStatus.BAD_REQUEST,
                    "INVALID_ROLE_TRANSITION");
        }

        target.setRole("bibbeheerder");
        return toUserListItem(appUserRepository.save(target));
    }

    private AppUser resolveCaller(String callerSub) {
        AppUser caller = appUserRepository.findBySub(callerSub)
                .orElseThrow(() -> new ApiException("Gebruiker niet gevonden", HttpStatus.FORBIDDEN, "NO_SCHOOL"));
        if (caller.getSchool() == null) {
            throw new ApiException("Geen school gevonden voor uw account", HttpStatus.FORBIDDEN, "NO_SCHOOL");
        }
        return caller;
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
