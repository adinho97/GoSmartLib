package com.example.demo.oneroster.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OneRosterMetadata {

    @JsonProperty("smsc.legacyIdentifier")
    private String legacyIdentifier;

    public String getLegacyIdentifier() {
        return legacyIdentifier;
    }

    public void setLegacyIdentifier(String legacyIdentifier) {
        this.legacyIdentifier = legacyIdentifier;
    }
}
