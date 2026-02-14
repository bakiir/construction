package com.example.construction.reposirtories;

import com.example.construction.model.TaskTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskTemplateRepository extends JpaRepository<TaskTemplate, Long> {
    List<TaskTemplate> findBySubObjectTemplateId(Long subObjectTemplateId);
}
