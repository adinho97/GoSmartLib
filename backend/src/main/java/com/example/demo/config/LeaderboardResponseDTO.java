package com.example.demo.config;

import java.util.List;

public class LeaderboardResponseDTO {
    private List<LeaderboardEntryDTO> topClassReaders;
    private List<LeaderboardEntryDTO> topSchoolReaders;
    private LeaderboardEntryDTO userClassRank;
    private LeaderboardEntryDTO userSchoolRank;
    private List<LeaderboardKlasDTO> availableClasses;

    public LeaderboardResponseDTO() {
    }

    public List<LeaderboardEntryDTO> getTopClassReaders() {
        return topClassReaders;
    }

    public void setTopClassReaders(List<LeaderboardEntryDTO> topClassReaders) {
        this.topClassReaders = topClassReaders;
    }

    public List<LeaderboardEntryDTO> getTopSchoolReaders() {
        return topSchoolReaders;
    }

    public void setTopSchoolReaders(List<LeaderboardEntryDTO> topSchoolReaders) {
        this.topSchoolReaders = topSchoolReaders;
    }

    public LeaderboardEntryDTO getUserClassRank() {
        return userClassRank;
    }

    public void setUserClassRank(LeaderboardEntryDTO userClassRank) {
        this.userClassRank = userClassRank;
    }

    public LeaderboardEntryDTO getUserSchoolRank() {
        return userSchoolRank;
    }

    public void setUserSchoolRank(LeaderboardEntryDTO userSchoolRank) {
        this.userSchoolRank = userSchoolRank;
    }

    public List<LeaderboardKlasDTO> getAvailableClasses() {
        return availableClasses;
    }

    public void setAvailableClasses(List<LeaderboardKlasDTO> availableClasses) {
        this.availableClasses = availableClasses;
    }

    public static class LeaderboardKlasDTO {
        private Long id;
        private String naam;

        public LeaderboardKlasDTO(Long id, String naam) {
            this.id = id;
            this.naam = naam;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getNaam() {
            return naam;
        }

        public void setNaam(String naam) {
            this.naam = naam;
        }
    }
}