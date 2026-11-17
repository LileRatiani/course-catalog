package com.example.course_catalog.dto;

import com.example.course_catalog.entity.Course;
import com.example.course_catalog.entity.Level;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * What the REST API returns for a course (converted to JSON).
 * We don't return the Course entity directly because:
 *  - course -> students -> courses -> students ... would loop forever in JSON
 *  - we decide exactly which fields are public
 */
public record CourseResponse(
        Long id,
        String title,
        String description,
        Level level,
        BigDecimal price,
        Integer durationHours,
        Long instructorId,
        String instructorName,
        int studentCount,
        String syllabusFileName,
        LocalDateTime createdAt
) {
    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getLevel(),
                course.getPrice(),
                course.getDurationHours(),
                course.getInstructor().getId(),
                course.getInstructor().getFullName(),
                course.getStudents().size(),
                course.getSyllabusFileName(),
                course.getCreatedAt()
        );
    }
}