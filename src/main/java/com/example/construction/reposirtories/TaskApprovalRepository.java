package com.example.construction.reposirtories;

import com.example.construction.model.TaskApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskApprovalRepository extends JpaRepository<TaskApproval, Long> {
}
