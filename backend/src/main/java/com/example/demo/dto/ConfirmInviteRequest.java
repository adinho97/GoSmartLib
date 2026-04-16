package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

public class ConfirmInviteRequest {

    @NotNull(message = "selectedTeacherId mag niet leeg zijn")
    private Long selectedTeacherId;

    public ConfirmInviteRequest() {
    }

    public ConfirmInviteRequest(Long selectedTeacherId) {
        this.selectedTeacherId = selectedTeacherId;
    }

    public Long getSelectedTeacherId() {
        return selectedTeacherId;
    }

    public void setSelectedTeacherId(Long selectedTeacherId) {
        this.selectedTeacherId = selectedTeacherId;
    }
}
