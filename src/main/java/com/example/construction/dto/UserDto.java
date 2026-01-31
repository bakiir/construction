package com.example.construction.dto;

import com.example.construction.Enums.Role;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserDto {

    private Long id;
    private String email;
    private String fullName;
    private Role role;
    private boolean isActive;
}
