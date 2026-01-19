package com.example.construction.mapper;

import com.example.construction.dto.TaskDto;
import com.example.construction.model.Task;
import com.example.construction.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(source = "subObject.id", target = "subObjectId")
    @Mapping(source = "assignees", target = "assigneeIds")
    TaskDto toDto(Task task);

    default Set<Long> map(Set<User> value) {
        if (value == null) {
            return null;
        }
        return value.stream()
                .map(User::getId)
                .collect(Collectors.toSet());
    }

    @Mapping(target = "subObject", ignore = true)
    @Mapping(target = "checklistItems", ignore = true)
    @Mapping(target = "report", ignore = true)
    @Mapping(target = "assignees", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Task toEntity(TaskDto taskDto);
}
