package com.example.course_catalog.repository;

import com.example.course_catalog.entity.Instructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstructorRepository extends JpaRepository<Instructor, Long> {

    // ---------- Method-name queries ----------

    // SELECT COUNT(*) > 0 FROM instructors WHERE email = ?
    // Used when CREATING an instructor: is this email already taken?
    boolean existsByEmail(String email);

    // SELECT COUNT(*) > 0 FROM instructors WHERE email = ? AND id <> ?
    // Used when EDITING: is the email taken by ANOTHER instructor?
    boolean existsByEmailAndIdNot(String email, Long id);

    // SELECT * FROM instructors ORDER BY last_name ASC
    // Used for the instructor dropdown in the course form
    List<Instructor> findAllByOrderByLastNameAsc();
}