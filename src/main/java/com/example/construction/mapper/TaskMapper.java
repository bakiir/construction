package com.example.construction.mapper;

import com.example.construction.dto.ChecklistItemDto;
import com.example.construction.dto.ReportDto;
import com.example.construction.dto.TaskDto;
import com.example.construction.model.ChecklistItem;
import com.example.construction.model.Report;
import com.example.construction.model.ReportPhoto;
import com.example.construction.model.Task;
import com.example.construction.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(source = "subObject.id", target = "subObjectId")
    @Mapping(source = "subObject.name", target = "subObjectName")
    @Mapping(source = "subObject.constructionObject.name", target = "objectName")
    @Mapping(source = "subObject.constructionObject.address", target = "objectAddress")
    @Mapping(source = "subObject.constructionObject.project.name", target = "projectName")
    @Mapping(source = "assignees", target = "assigneeIds")
    @Mapping(source = "checklistItems", target = "checklist")
    @Mapping(source = "report", target = "report")
    @Mapping(source = "subObject.constructionObject.project.projectManager.id", target = "projectManagerId")
    @Mapping(source = "subObject.constructionObject.project.foremen", target = "projectForemanIds")
    @Mapping(source = "subObject.workers", target = "subObjectWorkerIds")
    TaskDto toDto(Task task);

    ChecklistItemDto toChecklistItemDto(ChecklistItem item);

    @Mapping(target = "photos", expression = "java(mapPhotos(report.getPhotos()))")
    @Mapping(source = "author.fullName", target = "authorName")
    ReportDto toReportDto(Report report);

    default List<String> mapPhotos(List<ReportPhoto> photos) {
        if (photos == null)
            return null;
        return photos.stream()
                .map(ReportPhoto::getFilePath)
                .collect(Collectors.toList());
    }

    default Set<Long> map(Set<User> value) {
        if (value == null) {
            return null;
        }
        return value.stream()
                .map(User::getId)
                .collect(Collectors.toSet());
    }

    default List<Long> mapToList(Set<User> value) {
        if (value == null) {
            return null;
        }
        return value.stream()
                .map(User::getId)
                .collect(Collectors.toList());
    }

    @Mapping(target = "subObject", ignore = true)
    @Mapping(target = "checklistItems", ignore = true)
    @Mapping(target = "report", ignore = true)
    @Mapping(target = "assignees", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Task toEntity(TaskDto taskDto);
}
