package com.example.demo.dto;

public class ReturnLoanRequest {
    public enum ReturnCondition {
        GOOD,
        MODERATE,
        BAD
    }

    private ReturnCondition condition = ReturnCondition.GOOD;
    private boolean lost;

    public ReturnCondition getCondition() {
        return condition;
    }

    public void setCondition(ReturnCondition condition) {
        this.condition = condition;
    }

    public boolean isLost() {
        return lost;
    }

    public void setLost(boolean lost) {
        this.lost = lost;
    }
}