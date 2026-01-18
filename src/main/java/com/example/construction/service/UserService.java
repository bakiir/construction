package com.example.construction.service;

import com.example.construction.dto.UserCreateDto;
import com.example.construction.dto.UserDto;
import com.example.construction.dto.UserUpdateDto;
import com.example.construction.mapper.UserMapper;
import com.example.construction.model.User;
import com.example.construction.reposirtories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    // CREATE
    public UserDto create(UserCreateDto dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setFullName(dto.getFullName());
        user.setRole(dto.getRole());
        user.setActive(true);

        return mapper.toDto(userRepository.save(user));
    }

    // READ
    public UserDto getById(Long id) {
        return userRepository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<UserDto> getAll() {
        return userRepository.findAll()
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    // UPDATE
    public UserDto update(Long id, UserUpdateDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (dto.getFullName() != null)
            user.setFullName(dto.getFullName());

        if (dto.getRole() != null)
            user.setRole(dto.getRole());

        if (dto.getIsActive() != null)
            user.setActive(dto.getIsActive());

        return mapper.toDto(userRepository.save(user));
    }

    // DELETE (soft delete)
    public void deactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setActive(false);
        userRepository.save(user);
    }
}

