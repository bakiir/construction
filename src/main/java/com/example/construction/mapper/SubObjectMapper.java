package com.example.construction.mapper;


import com.example.construction.dto.SubObjectDto;
import com.example.construction.model.SubObject;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubObjectMapper {
    SubObjectDto toDto(SubObject subObject);
    SubObject toEntity(SubObjectDto dto);
}
