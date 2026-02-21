package com.example.construction.dto;

import com.example.construction.Enums.Role;
import com.example.construction.Enums.UserStatus;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserUpdateDto {

    private String phone;

    private String password;

    private String fullName;
    private Role role;

    private UserStatus status;
}
