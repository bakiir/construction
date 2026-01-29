package com.example.construction.service;

import com.example.construction.Enums.TaskStatus;
import com.example.construction.dto.ReportCreateDto;
import com.example.construction.model.*;
import com.example.construction.reposirtories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
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

    @Transactional
    public void createReport(Long taskId, ReportCreateDto reportDto, List<MultipartFile> files, String authorEmail) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));

        User author = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + authorEmail));

        // Validation
        if (!task.getAssignees().contains(author)) {
            throw new IllegalStateException("User is not an assignee of this task.");
        }
        if (task.getStatus() != TaskStatus.ACTIVE && task.getStatus() != TaskStatus.REWORK
                && task.getStatus() != TaskStatus.REWORK_FOREMAN) {
            throw new IllegalStateException(
                    "Task must be in ACTIVE, REWORK or REWORK_FOREMAN status to submit a report.");
        }

        Report report = task.getReport();
        if (report == null) {
            report = new Report();
            report.setTask(task);
        } else {
            // Clear existing data for update
            report.getPhotos().clear();
            report.getChecklistAnswers().clear();
        }

        report.setAuthor(author);
        report.setComment(reportDto.getComment());

        reportRepository.save(report);

        // Store photos
        // Clear existing photos first (orphanRemoval will delete them from DB)
        report.getPhotos().clear();

        final Report finalReport = report;

        List<ReportPhoto> newPhotos = Arrays.stream(files.toArray())
                .map(file -> {
                    String fileName = fileStorageService.storeFile((MultipartFile) file);
                    ReportPhoto reportPhoto = new ReportPhoto();
                    reportPhoto.setFilePath(fileName);
                    reportPhoto.setReport(finalReport);
                    return reportPhoto;
                })
                .collect(Collectors.toList());

        // Add new photos to the collection
        report.getPhotos().addAll(newPhotos);

        // Save checklist answers
        report.getChecklistAnswers().clear();

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

        // Save the report again to cascade the new collection elements
        reportRepository.save(report);

        // Trigger workflow
        workflowService.submitTaskForReview(task.getId(), null);

    }
}
