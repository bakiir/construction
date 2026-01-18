package com.example.construction.service;

import com.example.construction.dto.SubObjectCreateDto;
import com.example.construction.dto.SubObjectDto;
import com.example.construction.mapper.SubObjectMapper;
import com.example.construction.model.ConstructionObject;
import com.example.construction.model.SubObject;
import com.example.construction.reposirtories.ObjectRepository;
import com.example.construction.reposirtories.SubObjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubObjectService {
    private final SubObjectRepository subObjectRepository;
    private final ObjectRepository objectRepository;
    private final SubObjectMapper mapper;

    public SubObjectDto create(SubObjectCreateDto dto) {
        ConstructionObject object = objectRepository.findById(dto.getObjectId())
                .orElseThrow(() -> new RuntimeException("ConstructionObject not found"));

        SubObject subObject = new SubObject();
        subObject.setName(dto.getName());
        subObject.setConstructionObject(object);

        return mapper.toDto(subObjectRepository.save(subObject));
    }

    public SubObjectDto getById(Long id) {
        return subObjectRepository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new RuntimeException("SubObject not found"));
    }

    public List<SubObjectDto> getByObject(Long objectId) {
        return subObjectRepository.findByConstructionObjectId(objectId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    public SubObjectDto update(Long id, SubObjectCreateDto dto) {
        SubObject subObject = subObjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SubObject not found"));

        subObject.setName(dto.getName());

        return mapper.toDto(subObjectRepository.save(subObject));
    }

    public void delete(Long id) {
        subObjectRepository.deleteById(id);
    }

}
