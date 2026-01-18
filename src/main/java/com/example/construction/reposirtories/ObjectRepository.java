package com.example.construction.reposirtories;

import com.example.construction.model.ConstructionObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObjectRepository extends JpaRepository<ConstructionObject, Long> {
    List<ConstructionObject> findByProjectId(Long projectId);
}
