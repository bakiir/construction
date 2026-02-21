package com.example.construction.service;

import com.example.construction.Enums.TaskStatus;
import com.example.construction.dto.ReportCreateDto;
import com.example.construction.model.*;
import com.example.construction.reposirtories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final FileStorageService fileStorageService;
    private final WorkflowService workflowService;

    public void createReport(Long taskId, ReportCreateDto reportDto, List<MultipartFile> files, String authorPhone) {
        // 1. Upload files first (I/O, slow, non-transactional)
        List<String> uploadedFileNames = new ArrayList<>();
        try {
            if (files != null) {
                for (MultipartFile file : files) {
                    uploadedFileNames.add(fileStorageService.upload(file, "reports"));
                }
            }

            // 2. Perform DB operations in separate transaction
            saveReportToDb(taskId, reportDto, uploadedFileNames, authorPhone);

        } catch (Exception e) {
            // 3. Compensation: Delete uploaded files if DB save fails
            for (String fileName : uploadedFileNames) {
                try {
                    fileStorageService.delete(fileName);
                } catch (Exception deleteEx) {
                    System.err.println("Failed to cleanup file: " + fileName);
                }
            }
            throw e; // Re-throw to propagate error
        }
    }

    @Transactional
    public void saveReportToDb(Long taskId, ReportCreateDto reportDto, List<String> uploadedFileNames,
            String authorPhone) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));

        User author = userRepository.findByPhone(authorPhone)
                .orElseThrow(() -> new RuntimeException("User not found with phone: " + authorPhone));

        // Validation
        if (!task.getAssignees().contains(author)) {
            throw new IllegalStateException("User is not an assignee of this task.");
        }
        if (task.getStatus() != TaskStatus.ACTIVE
                && task.getStatus() != TaskStatus.REWORK_FOREMAN) {
            throw new IllegalStateException(
                    "Task must be in ACTIVE or REWORK_FOREMAN status to submit a report.");
        }

        Report report = task.getReport();
        if (report == null) {
            report = new Report();
            report.setTask(task);
        } else {
            report.getPhotos().clear();
            report.getChecklistAnswers().clear();
        }

        report.setAuthor(author);
        report.setComment(reportDto.getComment());

        // Save to establish ID if needed (though cascading usually handles it)
        reportRepository.save(report);

        final Report finalReport = report;

        // Map uploaded strings to entities
        List<ReportPhoto> newPhotos = uploadedFileNames.stream()
                .map(fileName -> {
                    ReportPhoto reportPhoto = new ReportPhoto();
                    reportPhoto.setFilePath(fileName);
                    reportPhoto.setReport(finalReport);
                    return reportPhoto;
                })
                .collect(Collectors.toList());

        report.getPhotos().addAll(newPhotos);

        // Save checklist answers
        List<ReportChecklistAnswer> newAnswers = reportDto.getChecklistAnswers().stream()
                .map(answerDto -> {
                    ChecklistItem item = checklistItemRepository.findById(answerDto.getChecklistItemId())
                            .orElseThrow(() -> new RuntimeException(
                                    "ChecklistItem not found with id: " + answerDto.getChecklistItemId()));

                    ReportChecklistAnswer answer = new ReportChecklistAnswer();
                    answer.setReport(finalReport);
                    answer.setChecklistItem(item);
                    answer.setCompleted(answerDto.isCompleted());
                    return answer;
                })
                .collect(Collectors.toList());

        report.getChecklistAnswers().addAll(newAnswers);

        reportRepository.save(report);
        workflowService.submitTaskForReview(task.getId(), null, authorPhone);
    }
}
