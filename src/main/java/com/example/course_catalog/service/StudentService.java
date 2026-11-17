package com.example.course_catalog.service;

import com.example.course_catalog.dto.StudentForm;
import com.example.course_catalog.entity.Course;
import com.example.course_catalog.entity.Student;
import com.example.course_catalog.exception.BusinessRuleException;
import com.example.course_catalog.exception.NotFoundException;
import com.example.course_catalog.repository.CourseRepository;
import com.example.course_catalog.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    public StudentService(StudentRepository studentRepository,
                          CourseRepository courseRepository) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    // ---------- Read operations ----------

    @Transactional(readOnly = true)
    public List<Student> findAll() {
        return studentRepository.findAllByOrderByLastNameAscFirstNameAsc();
    }

    @Transactional(readOnly = true)
    public Student findById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Student with id " + id + " was not found"));
    }

    // Uses the custom JOIN @Query from CourseRepository
    @Transactional(readOnly = true)
    public List<Course> findCoursesOf(Long studentId) {
        findById(studentId);
        return courseRepository.findCoursesByStudentId(studentId);
    }

    // ---------- Write operations ----------

    @Transactional
    public Student create(StudentForm form) {
        String email = normalizeEmail(form.getEmail());
        if (studentRepository.existsByEmail(email)) {
            throw new BusinessRuleException("email", "This email is already used by another student");
        }
        Student student = new Student();
        copyFormToEntity(form, student);
        return studentRepository.save(student);
    }

    @Transactional
    public Student update(Long id, StudentForm form) {
        Student student = findById(id);
        String email = normalizeEmail(form.getEmail());
        if (studentRepository.existsByEmailAndIdNot(email, id)) {
            throw new BusinessRuleException("email", "This email is already used by another student");
        }
        copyFormToEntity(form, student);
        return student; // dirty checking -> UPDATE at commit
    }

    @Transactional
    public void delete(Long id) {
        Student student = findById(id);
        // Student is the INVERSE side of the Many-to-Many (mappedBy).
        // The join table is controlled by Course, so we must remove the student
        // from every course first; otherwise rows in course_student would still
        // point to this student and the DELETE would fail (foreign key).
        // We loop over a COPY because removeStudent() changes student.getCourses().
        for (Course course : new ArrayList<>(student.getCourses())) {
            course.removeStudent(student);
        }
        studentRepository.delete(student);
    }

    // ---------- Helpers ----------

    private void copyFormToEntity(StudentForm form, Student student) {
        student.setFirstName(form.getFirstName().trim());
        student.setLastName(form.getLastName().trim());
        student.setEmail(normalizeEmail(form.getEmail()));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}