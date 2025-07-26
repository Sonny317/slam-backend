package com.slam.slam_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RsvpRequest {
    private boolean isAttending;
    private boolean afterParty;
}