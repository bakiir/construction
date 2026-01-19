package com.example.construction.reposirtories;

import com.example.construction.model.ReportChecklistAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportChecklistAnswerRepository extends JpaRepository<ReportChecklistAnswer, Long> {
}
