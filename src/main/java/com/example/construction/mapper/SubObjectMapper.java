package com.example.construction.mapper;


import com.example.construction.dto.SubObjectDto;
import com.example.construction.model.SubObject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SubObjectMapper {
    @Mapping(source = "constructionObject.id", target = "objectId")
    SubObjectDto toDto(SubObject subObject);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "constructionObject", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    SubObject toEntity(SubObjectDto dto);
}
