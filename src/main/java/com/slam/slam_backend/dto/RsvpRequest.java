package com.slam.slam_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RsvpRequest {
    @JsonProperty("isAttending")
    private boolean attending;
    
    @JsonProperty("afterParty")
    private boolean afterParty;
    
    // 명시적으로 boolean getter 메서드 정의
    public boolean isAttending() {
        return attending;
    }
    
    public void setAttending(boolean attending) {
        this.attending = attending;
    }
    
    public boolean isAfterParty() {
        return afterParty;
    }
    
    @Override
    public String toString() {
        return "RsvpRequest{" +
                "attending=" + attending +
                ", afterParty=" + afterParty +
                '}';
    }
}