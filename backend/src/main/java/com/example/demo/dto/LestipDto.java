package com.example.demo.dto;

public class LestipDto {
    private String lestip;
    private String auteurNaam;
    private Boolean magVerwijderen;
    private String fileName;
    private String fileContentType;
    private byte[] fileData;

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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileContentType() {
        return fileContentType;
    }

    public void setFileContentType(String fileContentType) {
        this.fileContentType = fileContentType;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }
}
