package com.example.construction.dto;

import com.example.construction.Enums.Role;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserCreateDto {

    private String phone;
    private String password;
    private String fullName;
    private Role role;
}
