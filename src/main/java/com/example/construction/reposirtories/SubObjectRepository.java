package com.example.construction.reposirtories;

import com.example.construction.model.SubObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubObjectRepository extends JpaRepository<SubObject, Long> {
    List<SubObject> findByConstructionObjectId(Long id);
}
