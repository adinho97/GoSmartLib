package com.example.demo.dto;

import java.util.List;

public class LestipDto {
    private String lestip;
    private String auteurNaam;
    private Boolean magVerwijderen;
    private String fileName;
    private String fileContentType;
    private byte[] fileData;
    private List<AttachmentDto> attachments;

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

    public List<AttachmentDto> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentDto> attachments) {
        this.attachments = attachments;
    }

    public static class AttachmentDto {
        private String fileName;
        private String contentType;
        private String fileData;

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public String getFileData() {
            return fileData;
        }

        public void setFileData(String fileData) {
            this.fileData = fileData;
        }
    }
}
