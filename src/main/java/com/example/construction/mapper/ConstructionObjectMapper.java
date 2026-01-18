package com.example.construction.mapper;

import com.example.construction.dto.ConstructionObjectDto;
import com.example.construction.model.ConstructionObject;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConstructionObjectMapper {
    ConstructionObjectDto toDto(ConstructionObject constructionObject);
    ConstructionObject toEntity(ConstructionObjectDto objectDto);
}
