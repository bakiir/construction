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
    private final ReportPhotoRepository reportPhotoRepository;
    private final ReportChecklistAnswerRepository reportChecklistAnswerRepository;
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
        if (task.getStatus() != TaskStatus.ACTIVE && task.getStatus() != TaskStatus.REWORK) {
            throw new IllegalStateException("Task must be in ACTIVE or REWORK status to submit a report.");
        }

        Report report = new Report();
        report.setTask(task);
        report.setAuthor(author);
        report.setComment(reportDto.getComment());

        Report savedReport = reportRepository.save(report);

        // Store photos
        List<ReportPhoto> photos = Arrays.stream(files.toArray())
                .map(file -> {
                    String fileName = fileStorageService.storeFile((MultipartFile) file);
                    ReportPhoto reportPhoto = new ReportPhoto();
                    reportPhoto.setFilePath(fileName);
                    reportPhoto.setReport(savedReport);
                    return reportPhotoRepository.save(reportPhoto);
                })
                .collect(Collectors.toList());
        savedReport.setPhotos(photos);


        // Save checklist answers
        List<ReportChecklistAnswer> answers = reportDto.getChecklistAnswers().stream()
                .map(answerDto -> {
                    ChecklistItem item = checklistItemRepository.findById(answerDto.getChecklistItemId())
                            .orElseThrow(() -> new RuntimeException("ChecklistItem not found with id: " + answerDto.getChecklistItemId()));

                    ReportChecklistAnswer answer = new ReportChecklistAnswer();
                    answer.setReport(savedReport);
                    answer.setChecklistItem(item);
                    answer.setCompleted(answerDto.isCompleted());
                    return reportChecklistAnswerRepository.save(answer);
                })
                .collect(Collectors.toList());
        savedReport.setChecklistAnswers(answers);


        // Trigger workflow
        workflowService.submitTaskForReview(task.getId());

    }
}
