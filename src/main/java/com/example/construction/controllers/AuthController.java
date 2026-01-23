package com.example.construction.controllers;

import com.example.construction.dto.LoginRequest;
import com.example.construction.dto.LoginResponse;
import com.example.construction.dto.UserCreateDto;
import com.example.construction.dto.UserDto;
import com.example.construction.service.JwtService;
import com.example.construction.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserService userService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {

        Authentication authentication =
                authManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getEmail(),
                                request.getPassword()
                        )
                );

        String token = jwtService.generateToken(
                (UserDetails) authentication.getPrincipal()
        );

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        return response;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody UserCreateDto request) {

         userService.create(request);
         return ResponseEntity.status(201).build();
    }

    @GetMapping("/me")
    public UserDto me(Authentication authentication) {
        String email = authentication.getName();
        Long userId = userService.getUserIdByEmail(email);
        return userService.getById(userId);
    }
}

