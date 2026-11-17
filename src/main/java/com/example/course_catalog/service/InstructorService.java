package com.example.course_catalog.service;

import com.example.course_catalog.dto.InstructorForm;
import com.example.course_catalog.entity.Course;
import com.example.course_catalog.entity.Instructor;
import com.example.course_catalog.exception.BusinessRuleException;
import com.example.course_catalog.exception.NotFoundException;
import com.example.course_catalog.repository.CourseRepository;
import com.example.course_catalog.repository.InstructorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InstructorService {

    private final InstructorRepository instructorRepository;
    private final CourseRepository courseRepository;

    // Constructor injection: Spring sees this constructor and passes in the
    // repository beans automatically. No @Autowired needed (only one constructor).
    public InstructorService(InstructorRepository instructorRepository,
                             CourseRepository courseRepository) {
        this.instructorRepository = instructorRepository;
        this.courseRepository = courseRepository;
    }

    // ---------- Read operations ----------

    @Transactional(readOnly = true)
    public List<Instructor> findAll() {
        return instructorRepository.findAllByOrderByLastNameAsc();
    }

    @Transactional(readOnly = true)
    public Instructor findById(Long id) {
        return instructorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Instructor with id " + id + " was not found"));
    }

    @Transactional(readOnly = true)
    public List<Course> findCoursesOf(Long instructorId) {
        findById(instructorId); // throws NotFoundException if the instructor does not exist
        return courseRepository.findByInstructorIdOrderByTitleAsc(instructorId);
    }

    // ---------- Write operations (modify data -> @Transactional) ----------

    @Transactional
    public Instructor create(InstructorForm form) {
        String email = normalizeEmail(form.getEmail());
        if (instructorRepository.existsByEmail(email)) {
            throw new BusinessRuleException("email", "This email is already used by another instructor");
        }
        Instructor instructor = new Instructor();
        copyFormToEntity(form, instructor);
        return instructorRepository.save(instructor); // INSERT
    }

    @Transactional
    public Instructor update(Long id, InstructorForm form) {
        Instructor instructor = findById(id);
        String email = normalizeEmail(form.getEmail());
        if (instructorRepository.existsByEmailAndIdNot(email, id)) {
            throw new BusinessRuleException("email", "This email is already used by another instructor");
        }
        copyFormToEntity(form, instructor);
        // No save() needed: "instructor" was loaded inside this transaction, so
        // Hibernate tracks it ("dirty checking") and runs the UPDATE automatically
        // when the transaction commits at the end of this method.
        return instructor;
    }

    @Transactional
    public void delete(Long id) {
        Instructor instructor = findById(id);
        long courseCount = courseRepository.countByInstructorId(id);
        if (courseCount > 0) {
            throw new BusinessRuleException("Cannot delete " + instructor.getFullName()
                    + ": they still teach " + courseCount + " course(s). Reassign or delete those courses first.");
        }
        instructorRepository.delete(instructor); // DELETE
    }

    // ---------- Helpers ----------

    private void copyFormToEntity(InstructorForm form, Instructor instructor) {
        instructor.setFirstName(form.getFirstName().trim());
        instructor.setLastName(form.getLastName().trim());
        instructor.setEmail(normalizeEmail(form.getEmail()));
        instructor.setBio(form.getBio() == null || form.getBio().isBlank() ? null : form.getBio().trim());
    }

    // "Anna@Mail.com " and "anna@mail.com" should count as the same email
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}