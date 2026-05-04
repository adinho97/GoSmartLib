package com.example.demo.config;

public class LeaderboardEntryDTO {
    private int rank;
    private String displayName;
    private int count;
    private boolean isCurrentUser;

    public LeaderboardEntryDTO() {
    }

    public LeaderboardEntryDTO(int rank, String displayName, int count, boolean isCurrentUser) {
        this.rank = rank;
        this.displayName = displayName;
        this.count = count;
        this.isCurrentUser = isCurrentUser;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean getIsCurrentUser() {
        return isCurrentUser;
    }

    public void setCurrentUser(boolean currentUser) {
        isCurrentUser = currentUser;
    }
}