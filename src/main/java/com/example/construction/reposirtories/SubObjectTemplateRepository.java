package com.example.construction.reposirtories;

import com.example.construction.model.SubObjectTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubObjectTemplateRepository extends JpaRepository<SubObjectTemplate, Long> {
    Optional<SubObjectTemplate> findByName(String name);
}
