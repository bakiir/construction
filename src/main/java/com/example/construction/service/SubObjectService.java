package com.example.construction.service;

import com.example.construction.Enums.Role;
import com.example.construction.dto.SubObjectCreateDto;
import com.example.construction.dto.SubObjectDto;
import com.example.construction.dto.UserDto;
import com.example.construction.mapper.SubObjectMapper;
import com.example.construction.mapper.UserMapper;
import com.example.construction.model.ConstructionObject;
import com.example.construction.model.SubObject;
import com.example.construction.model.User;
import com.example.construction.reposirtories.ObjectRepository;
import com.example.construction.reposirtories.SubObjectRepository;
import com.example.construction.reposirtories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubObjectService {
        private final SubObjectRepository subObjectRepository;
        private final ObjectRepository objectRepository;
        private final SubObjectMapper mapper;
        private final UserRepository userRepository;
        private final UserMapper userMapper;

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

        // Worker assignment methods

        @Transactional
        public void addWorker(Long subObjectId, Long workerId) {
                SubObject subObject = subObjectRepository.findById(subObjectId)
                                .orElseThrow(() -> new RuntimeException("SubObject not found"));
                User worker = userRepository.findById(workerId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                if (worker.getRole() != Role.WORKER) {
                        throw new IllegalArgumentException("User must have WORKER role");
                }

                subObject.getWorkers().add(worker);
                subObjectRepository.save(subObject);
        }

        @Transactional
        public void removeWorker(Long subObjectId, Long workerId) {
                SubObject subObject = subObjectRepository.findById(subObjectId)
                                .orElseThrow(() -> new RuntimeException("SubObject not found"));

                subObject.getWorkers().removeIf(w -> w.getId().equals(workerId));
                subObjectRepository.save(subObject);
        }

        public List<UserDto> getWorkers(Long subObjectId) {
                SubObject subObject = subObjectRepository.findById(subObjectId)
                                .orElseThrow(() -> new RuntimeException("SubObject not found"));

                return subObject.getWorkers().stream()
                                .map(userMapper::toDto)
                                .collect(Collectors.toList());
        }

}
