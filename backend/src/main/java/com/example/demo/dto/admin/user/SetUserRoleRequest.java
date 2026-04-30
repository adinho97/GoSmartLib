package com.example.demo.dto.admin.user;

import jakarta.validation.constraints.NotBlank;

public class SetUserRoleRequest {

    @NotBlank(message = "Rol is verplicht")
    private String role;

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
