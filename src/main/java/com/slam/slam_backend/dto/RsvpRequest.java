package com.slam.slam_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RsvpRequest {
    @JsonProperty("isAttending")
    private boolean isAttending;
    
    @JsonProperty("afterParty")
    private boolean afterParty;
    
    @Override
    public String toString() {
        return "RsvpRequest{" +
                "isAttending=" + isAttending +
                ", afterParty=" + afterParty +
                '}';
    }
}