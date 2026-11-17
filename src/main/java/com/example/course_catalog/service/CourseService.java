package com.example.course_catalog.service;

import com.example.course_catalog.dto.CourseForm;
import com.example.course_catalog.dto.CoursePopularity;
import com.example.course_catalog.entity.Course;
import com.example.course_catalog.entity.Level;
import com.example.course_catalog.entity.Student;
import com.example.course_catalog.exception.BusinessRuleException;
import com.example.course_catalog.exception.NotFoundException;
import com.example.course_catalog.repository.CourseRepository;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final InstructorService instructorService;
    private final StudentService studentService;
    private final FileStorageService fileStorageService;

    // Constructor injection of 1 repository + 3 other services
    public CourseService(CourseRepository courseRepository,
                         InstructorService instructorService,
                         StudentService studentService,
                         FileStorageService fileStorageService) {
        this.courseRepository = courseRepository;
        this.instructorService = instructorService;
        this.studentService = studentService;
        this.fileStorageService = fileStorageService;
    }

    // ---------- Read operations ----------

    /**
     * Search + filter + pagination + sorting for the course list page.
     * title: text to search for (null/empty = all), level: null = all levels.
     */
    @Transactional(readOnly = true)
    public Page<Course> search(String title, Level level, Pageable pageable) {
        String searchText = (title == null) ? "" : title.trim();
        return courseRepository.search(searchText, level, pageable);
    }

    @Transactional(readOnly = true)
    public Course findById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Course with id " + id + " was not found"));
    }

    /**
     * Top N courses by number of enrolled students (uses the GROUP BY @Query).
     */
    @Transactional(readOnly = true)
    public List<CoursePopularity> findMostPopular(int limit) {
        return courseRepository.findCoursePopularity(PageRequest.of(0, limit));
    }

    /**
     * Students who are NOT yet in this course (for the "enroll" dropdown).
     */
    @Transactional(readOnly = true)
    public List<Student> findStudentsNotEnrolled(Long courseId) {
        Course course = findById(courseId);
        return studentService.findAll().stream()
                .filter(student -> !course.getStudents().contains(student))
                .toList();
    }

    @Transactional(readOnly = true)
    public Resource loadSyllabus(Long courseId) {
        Course course = findById(courseId);
        if (course.getSyllabusFileName() == null) {
            throw new NotFoundException("This course has no syllabus");
        }
        return fileStorageService.load(course.getSyllabusFileName());
    }

    // ---------- Write operations ----------

    @Transactional
    public Course create(CourseForm form, MultipartFile syllabus) {
        Course course = new Course();
        copyFormToEntity(form, course);
        if (syllabus != null && !syllabus.isEmpty()) {
            // 1) save the file on disk, 2) remember its name in the entity
            course.setSyllabusFileName(fileStorageService.store(syllabus));
        }
        return courseRepository.save(course); // INSERT (createdAt is set by @PrePersist)
    }

    @Transactional
    public Course update(Long id, CourseForm form, MultipartFile syllabus) {
        Course course = findById(id);
        copyFormToEntity(form, course);
        if (syllabus != null && !syllabus.isEmpty()) {
            // New file uploaded: store it, then delete the old one
            String oldFileName = course.getSyllabusFileName();
            course.setSyllabusFileName(fileStorageService.store(syllabus));
            fileStorageService.delete(oldFileName);
        }
        // No upload -> the existing syllabus is kept.
        return course; // dirty checking -> UPDATE at commit
    }

    @Transactional
    public void delete(Long id) {
        Course course = findById(id);
        String fileName = course.getSyllabusFileName();
        // Course is the OWNING side of the Many-to-Many, so Hibernate
        // automatically deletes its rows from course_student first.
        courseRepository.delete(course);
        fileStorageService.delete(fileName);
    }

    @Transactional
    public void enroll(Long courseId, Long studentId) {
        Course course = findById(courseId);
        Student student = studentService.findById(studentId);
        if (course.getStudents().contains(student)) {
            throw new BusinessRuleException(student.getFullName() + " is already enrolled in this course");
        }
        course.addStudent(student); // INSERT INTO course_student at commit
    }

    @Transactional
    public void unenroll(Long courseId, Long studentId) {
        Course course = findById(courseId);
        Student student = studentService.findById(studentId);
        if (!course.getStudents().contains(student)) {
            throw new BusinessRuleException(student.getFullName() + " is not enrolled in this course");
        }
        course.removeStudent(student); // DELETE FROM course_student at commit
    }

    // ---------- Helpers ----------

    private void copyFormToEntity(CourseForm form, Course course) {
        course.setTitle(form.getTitle().trim());
        course.setDescription(form.getDescription() == null || form.getDescription().isBlank()
                ? null : form.getDescription().trim());
        course.setLevel(form.getLevel());
        course.setPrice(form.getPrice());
        course.setDurationHours(form.getDurationHours());
        // Turn the instructorId from the form into a real Instructor entity
        course.setInstructor(instructorService.findById(form.getInstructorId()));
    }
}