package com.example.demo.dto.admin.school;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateSchoolRequest {

    @NotBlank(message = "Subdomein is verplicht")
    @Size(max = 100, message = "Subdomein is te lang")
    @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Subdomein mag enkel letters, cijfers en koppeltekens bevatten")
    private String subdomain;

    @Size(max = 255, message = "Naam is te lang")
    private String naam;

    @Size(max = 500, message = "Adres is te lang")
    private String adres;

    private Double latitude;

    private Double longitude;

    public String getSubdomain() {
        return subdomain;
    }

    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }

    public String getNaam() {
        return naam;
    }

    public void setNaam(String naam) {
        this.naam = naam;
    }

    public String getAdres() {
        return adres;
    }

    public void setAdres(String adres) {
        this.adres = adres;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
