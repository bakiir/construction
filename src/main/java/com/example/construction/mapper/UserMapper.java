package com.example.construction.mapper;

import com.example.construction.dto.UserDto;
import com.example.construction.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto (User user);

    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    @Mapping(target = "reports", ignore = true)
    User toEntity(UserDto userDto);
}
