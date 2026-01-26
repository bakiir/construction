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
public abstract class TaskMapper {

    @org.springframework.beans.factory.annotation.Autowired
    protected com.example.construction.service.S3Service s3Service;

    @Mapping(source = "subObject.id", target = "subObjectId")
    @Mapping(source = "subObject.name", target = "subObjectName")
    @Mapping(source = "subObject.constructionObject.name", target = "objectName")
    @Mapping(source = "subObject.constructionObject.address", target = "objectAddress")
    @Mapping(source = "subObject.constructionObject.project.name", target = "projectName")
    @Mapping(source = "assignees", target = "assigneeIds")
    @Mapping(source = "checklistItems", target = "checklist")
    @Mapping(source = "report", target = "report")
    public abstract TaskDto toDto(Task task);

    public abstract ChecklistItemDto toChecklistItemDto(ChecklistItem item);

    @Mapping(target = "photos", expression = "java(mapPhotos(report.getPhotos()))")
    @Mapping(source = "author.fullName", target = "authorName")
    public abstract ReportDto toReportDto(Report report);

    protected List<String> mapPhotos(List<ReportPhoto> photos) {
        if (photos == null)
            return null;
        return photos.stream()
                .filter(photo -> photo.getStoredFile() != null)
                .map(photo -> s3Service.generatePresignedUrl(photo.getStoredFile()))
                .collect(Collectors.toList());
    }

    protected Set<Long> map(Set<User> value) {
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
    public abstract Task toEntity(TaskDto taskDto);
}
