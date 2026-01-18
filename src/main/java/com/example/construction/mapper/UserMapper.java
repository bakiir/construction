package com.example.construction.mapper;

import com.example.construction.dto.UserDto;
import com.example.construction.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto (User user);
    User toEntity(UserDto userDto);
}
