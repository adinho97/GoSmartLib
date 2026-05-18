package com.example.demo.oneroster.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OneRosterClass {

    private String sourcedId;
    private String title;
    private String classCode;
    private String type;
    private OneRosterMetadata metadata;

    public String getSourcedId() {
        return sourcedId;
    }

    public void setSourcedId(String sourcedId) {
        this.sourcedId = sourcedId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public OneRosterMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(OneRosterMetadata metadata) {
        this.metadata = metadata;
    }

    public String legacyIdentifier() {
        return metadata == null ? null : metadata.getLegacyIdentifier();
    }
}
