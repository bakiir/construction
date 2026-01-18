package com.example.construction.mapper;

import com.example.construction.dto.ProjectCreateDto;
import com.example.construction.dto.ProjectDto;
import com.example.construction.model.Project;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProjectMapper {
    ProjectDto toDto(Project project);

    Project toEntity (ProjectDto projectDto);
    Project toEntity (ProjectCreateDto projectDto);
}
