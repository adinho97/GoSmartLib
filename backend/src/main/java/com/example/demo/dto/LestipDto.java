package com.example.demo.dto;

public class LestipDto {
    private String lestip;
    private String auteurNaam;
    private Boolean magVerwijderen;

    public String getLestip() {
        return lestip;
    }

    public void setLestip(String lestip) {
        this.lestip = lestip;
    }

    public String getAuteurNaam() {
        return auteurNaam;
    }

    public void setAuteurNaam(String auteurNaam) {
        this.auteurNaam = auteurNaam;
    }

    public Boolean getMagVerwijderen() {
        return magVerwijderen;
    }

    public void setMagVerwijderen(Boolean magVerwijderen) {
        this.magVerwijderen = magVerwijderen;
    }
}
