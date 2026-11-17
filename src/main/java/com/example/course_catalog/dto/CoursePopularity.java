package com.example.course_catalog.dto;

/**
 * Result of the "most popular courses" query.
 * Not an entity: it is just a container for query results.
 */
public record CoursePopularity(Long courseId, String title, Long studentCount) {
}