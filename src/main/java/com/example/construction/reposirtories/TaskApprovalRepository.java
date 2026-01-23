package com.example.construction.reposirtories;

import com.example.construction.model.TaskApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskApprovalRepository extends JpaRepository<TaskApproval, Long> {
    Optional<TaskApproval> findTopByTaskIdAndDecisionOrderByCreatedAtDesc(Long taskId, String decision);
}
