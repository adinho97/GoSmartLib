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
