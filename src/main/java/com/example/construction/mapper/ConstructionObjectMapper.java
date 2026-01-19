package com.example.construction.mapper;

import com.example.construction.dto.ConstructionObjectDto;
import com.example.construction.model.ConstructionObject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConstructionObjectMapper {
    @Mapping(source = "project.id", target = "projectId")
    ConstructionObjectDto toDto(ConstructionObject constructionObject);

    @Mapping(target = "project", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "id", ignore = true) // Also ignore id
    ConstructionObject toEntity(ConstructionObjectDto objectDto);
}
