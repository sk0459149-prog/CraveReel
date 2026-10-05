package com.recipereels.service;

import com.recipereels.dto.*;
import com.recipereels.entity.User;

import java.util.List;

public interface UserService {
    AuthResponse login(AuthRequest request);
    AuthResponse register(RegisterRequest request);
    UserProfileDTO getCurrentUserProfile(String email);
    UserProfileDTO updateProfile(String email, UserProfileDTO updateDto);

    // Admin User Management
    List<UserAdminDTO> getAllUsersForAdmin(String searchQuery);
    UserAdminDTO createUserByAdmin(UserAdminDTO userDto);
    UserAdminDTO updateUserByAdmin(Long id, UserAdminDTO userDto);
    void deleteUserByAdmin(Long id);
    void toggleUserStatus(Long id, boolean enabled);
}
