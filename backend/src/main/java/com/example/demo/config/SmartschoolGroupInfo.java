package com.example.demo.config;

import java.util.List;

public class SmartschoolGroupInfo {
    private List<SmartschoolGroup> groups = List.of();
    private List<SmartschoolGroup> parentGroups = List.of();

    public List<SmartschoolGroup> getGroups() {
        return groups == null ? List.of() : groups;
    }

    public void setGroups(List<SmartschoolGroup> groups) {
        this.groups = groups;
    }

    public List<SmartschoolGroup> getParentGroups() {
        return parentGroups == null ? List.of() : parentGroups;
    }

    public void setParentGroups(List<SmartschoolGroup> parentGroups) {
        this.parentGroups = parentGroups;
    }
}

class SmartschoolGroup {
    private String groupID;
    private String name;
    private String description;
    private String platform;
    private Integer instituteNumber;
    private Integer adminNumber;

    public String getGroupID() {
        return groupID;
    }

    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public Integer getInstituteNumber() {
        return instituteNumber;
    }

    public void setInstituteNumber(Integer instituteNumber) {
        this.instituteNumber = instituteNumber;
    }

    public Integer getAdminNumber() {
        return adminNumber;
    }

    public void setAdminNumber(Integer adminNumber) {
        this.adminNumber = adminNumber;
    }
}
