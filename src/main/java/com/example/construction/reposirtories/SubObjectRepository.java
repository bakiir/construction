package com.example.construction.reposirtories;

import com.example.construction.model.SubObject;
import org.hibernate.boot.models.JpaAnnotations;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubObjectRepository extends JpaRepository<SubObject, Long> {
}
