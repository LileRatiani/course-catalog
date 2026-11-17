package com.example.course_catalog.repository;

import com.example.course_catalog.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    // ---------- Method-name queries ----------

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    // SELECT * FROM students ORDER BY last_name ASC, first_name ASC
    List<Student> findAllByOrderByLastNameAscFirstNameAsc();
}