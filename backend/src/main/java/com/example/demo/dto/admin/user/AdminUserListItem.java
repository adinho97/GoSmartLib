package com.example.demo.dto.admin.user;

public class AdminUserListItem {

    private Long id;
    private String sub;
    private String displayName;
    private String role;
    private String klasNaam;
    private boolean active;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSub() { return sub; }
    public void setSub(String sub) { this.sub = sub; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getKlasNaam() { return klasNaam; }
    public void setKlasNaam(String klasNaam) { this.klasNaam = klasNaam; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
