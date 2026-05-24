package com.example.demo.services;

/**
 * Caller context for review operations. The controller resolves auth state
 * (sub, role, school) and passes it in so the service stays free of
 * Spring Security types.
 *
 * @param userSub            authenticated user's sub identifier, or null when unauthenticated
 * @param isLibrarian        true when the caller has BIBBEHEERDER or SUPER_ADMIN role
 * @param leerlingSchoolId   non-null only when the caller is a LEERLING (used to scope
 *                           which books the student is allowed to see)
 */
public record ReviewContext(String userSub, boolean isLibrarian, Long leerlingSchoolId) {
}
