package com.example.demo.dto.admin.school;

public class KlasListItem {

    private Long id;
    private String groupId;
    private String naam;

    public KlasListItem() {}

    public KlasListItem(Long id, String groupId, String naam) {
        this.id = id;
        this.groupId = groupId;
        this.naam = naam;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getNaam() { return naam; }
    public void setNaam(String naam) { this.naam = naam; }
}
