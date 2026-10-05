package com.recipereels.service;

import com.recipereels.dto.ReelDTO;
import com.recipereels.entity.User;

import java.util.List;

public interface ReelService {
    List<ReelDTO> getApprovedReels(User currentUser);
    ReelDTO getReelById(Long id, User currentUser);
    void recordReelView(Long id);
}
