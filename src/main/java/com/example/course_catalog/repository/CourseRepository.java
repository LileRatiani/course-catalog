package com.example.course_catalog.repository;

import com.example.course_catalog.dto.CoursePopularity;
import com.example.course_catalog.entity.Course;
import com.example.course_catalog.entity.Level;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    // ---------- Method-name queries ----------

    // SELECT COUNT(*) FROM courses WHERE instructor_id = ?
    // Used to block deleting an instructor who still teaches courses
    long countByInstructorId(Long instructorId);

    // SELECT * FROM courses WHERE instructor_id = ? ORDER BY title ASC
    // Used on the instructor detail page
    List<Course> findByInstructorIdOrderByTitleAsc(Long instructorId);

    // ---------- Custom @Query (JPQL) queries ----------

    // 1) Search + filter WITH pagination and sorting.
    //    title: part of the title ("" matches everything)
    //    level: if null, the level filter is ignored
    //    pageable: page number, page size and sort, all from the URL
    @Query("""
            SELECT c FROM Course c
            WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :title, '%'))
              AND (:level IS NULL OR c.level = :level)
            """)
    Page<Course> search(@Param("title") String title,
                        @Param("level") Level level,
                        Pageable pageable);

    // 2) JOIN through the Many-to-Many relationship:
    //    all courses a given student is enrolled in
    @Query("""
            SELECT c FROM Course c
            JOIN c.students s
            WHERE s.id = :studentId
            ORDER BY c.title
            """)
    List<Course> findCoursesByStudentId(@Param("studentId") Long studentId);

    // 3) Aggregation: number of enrolled students per course, most popular first.
    //    "SELECT new ..." creates a CoursePopularity object for each result row.
    //    LEFT JOIN so courses with 0 students are included too.
    @Query("""
            SELECT new com.example.course_catalog.dto.CoursePopularity(c.id, c.title, COUNT(s))
            FROM Course c
            LEFT JOIN c.students s
            GROUP BY c.id, c.title
            ORDER BY COUNT(s) DESC
            """)
    List<CoursePopularity> findCoursePopularity(Pageable pageable);
}