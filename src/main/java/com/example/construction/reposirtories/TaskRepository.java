package com.example.construction.reposirtories;

import com.example.construction.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findBySubObjectId(Long id);

    List<Task> findBySubObjectIdAndIndex(Long subObjectId, Integer index);
}
