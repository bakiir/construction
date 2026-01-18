package com.example.construction.dto;

import com.example.construction.Enums.Role;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserUpdateDto {

    private String email;

    private String fullName;
    private Role role;
    private Boolean isActive;
}
