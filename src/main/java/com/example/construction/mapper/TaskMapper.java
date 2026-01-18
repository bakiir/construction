package com.example.construction.mapper;

import com.example.construction.dto.TaskDto;
import com.example.construction.model.Task;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskMapper {
    TaskDto toDto(Task task);

    Task toEntity(TaskDto taskDto);
}
