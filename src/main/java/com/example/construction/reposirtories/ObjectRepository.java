package com.example.construction.reposirtories;

import com.example.construction.model.Object;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ObjectRepository extends JpaRepository<Object, Long> {
}
