package com.recipereels.service.impl;

import com.recipereels.dto.*;
import com.recipereels.entity.Role;
import com.recipereels.entity.User;
import com.recipereels.exception.BadRequestException;
import com.recipereels.exception.ResourceNotFoundException;
import com.recipereels.repository.RecipeRepository;
import com.recipereels.repository.UserRepository;
import com.recipereels.security.JwtTokenProvider;
import com.recipereels.service.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public UserServiceImpl(UserRepository userRepository,
                           RecipeRepository recipeRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().trim().toLowerCase(), request.getPassword())
        );

        String token = tokenProvider.generateToken(authentication);
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole().name(), user.getAvatarUrl());
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already registered");
        }

        Role role = Role.ROLE_USER;
        if (request.getRole() != null) {
            String roleStr = request.getRole().trim().toUpperCase();
            if (roleStr.equals("ROLE_CONTRIBUTOR") || roleStr.equals("CONTRIBUTOR") || roleStr.equals("RECIPE_CONTRIBUTOR")) {
                role = Role.ROLE_CONTRIBUTOR;
            } else if (roleStr.equals("ROLE_ADMIN") || roleStr.equals("ADMIN")) {
                throw new BadRequestException("Registration as ADMIN is not permitted");
            }
        }

        User user = new User(request.getName().trim(), email, passwordEncoder.encode(request.getPassword()), role);
        user.setAvatarUrl("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150");
        User savedUser = userRepository.save(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );
        String token = tokenProvider.generateToken(authentication);

        return new AuthResponse(token, savedUser.getId(), savedUser.getName(), savedUser.getEmail(), savedUser.getRole().name(), savedUser.getAvatarUrl());
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDTO getCurrentUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return mapToProfileDTO(user);
    }

    @Override
    public UserProfileDTO updateProfile(String email, UserProfileDTO updateDto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (updateDto.getName() != null && !updateDto.getName().trim().isEmpty()) {
            user.setName(updateDto.getName().trim());
        }
        if (updateDto.getBio() != null) {
            user.setBio(updateDto.getBio().trim());
        }
        if (updateDto.getAvatarUrl() != null && !updateDto.getAvatarUrl().trim().isEmpty()) {
            user.setAvatarUrl(updateDto.getAvatarUrl().trim());
        }
        if (updateDto.getDietaryPreferences() != null) {
            user.setDietaryPreferences(updateDto.getDietaryPreferences().trim());
        }

        User updatedUser = userRepository.save(user);
        return mapToProfileDTO(updatedUser);
    }

    // Admin User Management
    @Override
    @Transactional(readOnly = true)
    public List<UserAdminDTO> getAllUsersForAdmin(String searchQuery) {
        List<User> users;
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            users = userRepository.searchUsers(searchQuery.trim());
        } else {
            users = userRepository.findAll();
        }

        return users.stream().map(user -> {
            long count = recipeRepository.countByUser(user);
            return new UserAdminDTO(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole().name(),
                    user.isEnabled(),
                    user.getCreatedAt(),
                    count
            );
        }).collect(Collectors.toList());
    }

    @Override
    public UserAdminDTO createUserByAdmin(UserAdminDTO userDto) {
        String email = userDto.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already registered");
        }

        Role role = Role.ROLE_USER;
        try {
            role = Role.valueOf(userDto.getRole());
        } catch (Exception e) {
            // Default
        }

        String rawPassword = (userDto.getPassword() != null && !userDto.getPassword().trim().isEmpty())
                ? userDto.getPassword().trim()
                : "Pass@123";

        User user = new User(userDto.getName().trim(), email, passwordEncoder.encode(rawPassword), role);
        user.setEnabled(userDto.isEnabled());
        user.setAvatarUrl("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150");
        User saved = userRepository.save(user);

        return new UserAdminDTO(saved.getId(), saved.getName(), saved.getEmail(), saved.getRole().name(), saved.isEnabled(), saved.getCreatedAt(), 0);
    }

    @Override
    public UserAdminDTO updateUserByAdmin(Long id, UserAdminDTO userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (userDto.getName() != null && !userDto.getName().trim().isEmpty()) {
            user.setName(userDto.getName().trim());
        }
        if (userDto.getEmail() != null && !userDto.getEmail().trim().isEmpty()) {
            String newEmail = userDto.getEmail().trim().toLowerCase();
            if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                throw new BadRequestException("Email is already taken by another account");
            }
            user.setEmail(newEmail);
        }
        if (userDto.getPassword() != null && !userDto.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDto.getPassword().trim()));
        }
        if (userDto.getRole() != null) {
            try {
                user.setRole(Role.valueOf(userDto.getRole()));
            } catch (Exception ignored) {}
        }
        user.setEnabled(userDto.isEnabled());

        User updated = userRepository.save(user);
        long count = recipeRepository.countByUser(updated);
        return new UserAdminDTO(updated.getId(), updated.getName(), updated.getEmail(), updated.getRole().name(), updated.isEnabled(), updated.getCreatedAt(), count);
    }

    @Override
    public void deleteUserByAdmin(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userRepository.delete(user);
    }

    @Override
    public void toggleUserStatus(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setEnabled(enabled);
        userRepository.save(user);
    }

    private UserProfileDTO mapToProfileDTO(User user) {
        return new UserProfileDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getDietaryPreferences(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
