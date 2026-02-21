package com.example.construction.dto;

import com.example.construction.Enums.Role;
import com.example.construction.Enums.UserStatus;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserDto {

    private Long id;
    private String phone;
    private String fullName;
    private Role role;

    private UserStatus status;
}
