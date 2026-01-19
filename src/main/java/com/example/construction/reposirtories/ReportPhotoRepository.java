package com.example.construction.reposirtories;

import com.example.construction.model.ReportPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportPhotoRepository extends JpaRepository<ReportPhoto, Long> {
}
