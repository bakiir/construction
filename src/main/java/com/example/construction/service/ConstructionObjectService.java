package com.example.construction.service;

import com.example.construction.dto.ConstructionObjectCreateDto;
import com.example.construction.dto.ConstructionObjectDto;
import com.example.construction.mapper.ConstructionObjectMapper;
import com.example.construction.model.ConstructionObject;
import com.example.construction.model.Project;
import com.example.construction.reposirtories.ObjectRepository;
import com.example.construction.reposirtories.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConstructionObjectService {
    private final ObjectRepository objectRepository;
    private final ConstructionObjectMapper mapper;
    private final ProjectRepository projectRepository;

    public ConstructionObjectDto create(ConstructionObjectCreateDto dto) {
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));
        log.info(project.getName());


        ConstructionObject object = new ConstructionObject();
        object.setName(dto.getName());
        object.setAddress(dto.getAddress());
        object.setProject(project);

        return mapper.toDto(objectRepository.save(object));
    }

    public ConstructionObjectDto getById(Long id) {
        return objectRepository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new RuntimeException("Object not found"));
    }



    public List<ConstructionObjectDto> getByProject(Long projectId) {
        return objectRepository.findByProjectId(projectId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    public ConstructionObjectDto update(Long id, ConstructionObjectCreateDto dto) {
        ConstructionObject object = objectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Object not found"));

        object.setName(dto.getName());
        object.setAddress(dto.getAddress());
        object.setUpdatedAt(LocalDateTime.now());

        return mapper.toDto(objectRepository.save(object));
    }


    public void delete(Long id) {
        objectRepository.deleteById(id);
    }


}
