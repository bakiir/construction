package com.example.construction.reposirtories;

import com.example.construction.model.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {

    List<ChecklistItem> findByTaskIdOrderByOrderIndexAsc(Long taskId);

    void deleteByTaskId(Long taskId);

    long countByTaskIdAndIsCompletedFalse(Long taskId);

    long countByTaskIdAndPhotoUrlIsNull(Long taskId);
}
