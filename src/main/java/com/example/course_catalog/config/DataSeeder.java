package com.example.course_catalog.config;

import com.example.course_catalog.dto.CourseForm;
import com.example.course_catalog.dto.InstructorForm;
import com.example.course_catalog.dto.StudentForm;
import com.example.course_catalog.entity.Course;
import com.example.course_catalog.entity.Instructor;
import com.example.course_catalog.entity.Level;
import com.example.course_catalog.entity.Student;
import com.example.course_catalog.service.CourseService;
import com.example.course_catalog.service.InstructorService;
import com.example.course_catalog.service.StudentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

/**
 * Configuration class that fills an EMPTY database with sample data at startup.
 * If there is already at least one instructor, it does nothing.
 */
@Configuration
public class DataSeeder {

    private final InstructorService instructorService;
    private final StudentService studentService;
    private final CourseService courseService;

    public DataSeeder(InstructorService instructorService,
                      StudentService studentService,
                      CourseService courseService) {
        this.instructorService = instructorService;
        this.studentService = studentService;
        this.courseService = courseService;
    }

    // @Bean: Spring calls this method once and registers the returned object as a bean.
    // A CommandLineRunner bean is executed automatically after the application has started.
    @Bean
    public CommandLineRunner seedSampleData() {
        return args -> {
            if (!instructorService.findAll().isEmpty()) {
                System.out.println("DataSeeder: database already has data, skipping.");
                return;
            }
            System.out.println("DataSeeder: empty database, inserting sample data...");

            Instructor nino = instructorService.create(instructor("Nino", "Beridze", "nino.beridze@example.com",
                    "Backend developer with 10 years of Java experience."));
            Instructor giorgi = instructorService.create(instructor("Giorgi", "Kapanadze", "giorgi.kapanadze@example.com",
                    "Data engineer and database enthusiast."));
            Instructor mariam = instructorService.create(instructor("Mariam", "Lomidze", "mariam.lomidze@example.com",
                    "Frontend developer and UX designer."));

            List<Student> students = List.of(
                    studentService.create(student("Ana", "Gelashvili", "ana.gelashvili@example.com")),
                    studentService.create(student("Luka", "Tsiklauri", "luka.tsiklauri@example.com")),
                    studentService.create(student("Elene", "Maisuradze", "elene.maisuradze@example.com")),
                    studentService.create(student("Davit", "Chikovani", "davit.chikovani@example.com")),
                    studentService.create(student("Tamar", "Abashidze", "tamar.abashidze@example.com"))
            );

            List<Course> courses = List.of(
                    courseService.create(course("Java Fundamentals", Level.BEGINNER, "49.99", 30, nino), null),
                    courseService.create(course("Spring Boot in Practice", Level.INTERMEDIATE, "89.00", 40, nino), null),
                    courseService.create(course("Advanced JPA and Hibernate", Level.ADVANCED, "119.00", 25, nino), null),
                    courseService.create(course("SQL for Beginners", Level.BEGINNER, "29.99", 15, giorgi), null),
                    courseService.create(course("Database Design", Level.INTERMEDIATE, "69.50", 20, giorgi), null),
                    courseService.create(course("Data Engineering Pipelines", Level.ADVANCED, "149.00", 45, giorgi), null),
                    courseService.create(course("HTML and CSS Basics", Level.BEGINNER, "19.99", 12, mariam), null),
                    courseService.create(course("JavaScript Essentials", Level.BEGINNER, "39.99", 24, mariam), null),
                    courseService.create(course("UX Design Principles", Level.INTERMEDIATE, "59.00", 18, mariam), null),
                    courseService.create(course("Web Accessibility", Level.INTERMEDIATE, "45.00", 10, mariam), null),
                    courseService.create(course("Microservices Architecture", Level.ADVANCED, "159.00", 50, nino), null),
                    courseService.create(course("Git and Version Control", Level.BEGINNER, "0.00", 6, giorgi), null)
            );

            // Some enrollments, so the "most popular" list on the home page is interesting
            int[][] enrollments = {
                    {0, 0}, {0, 1}, {0, 2}, {0, 3}, {0, 4},   // Java Fundamentals: 5 students
                    {1, 0}, {1, 1}, {1, 2}, {1, 3},           // Spring Boot: 4
                    {3, 1}, {3, 4}, {3, 2},                   // SQL: 3
                    {7, 0}, {7, 3},                           // JavaScript: 2
                    {2, 0}                                    // Advanced JPA: 1
            };
            for (int[] e : enrollments) {
                courseService.enroll(courses.get(e[0]).getId(), students.get(e[1]).getId());
            }

            System.out.println("DataSeeder: inserted 3 instructors, 5 students, 12 courses.");
        };
    }

    // ---------- Small helpers to build forms ----------

    private InstructorForm instructor(String first, String last, String email, String bio) {
        InstructorForm form = new InstructorForm();
        form.setFirstName(first);
        form.setLastName(last);
        form.setEmail(email);
        form.setBio(bio);
        return form;
    }

    private StudentForm student(String first, String last, String email) {
        StudentForm form = new StudentForm();
        form.setFirstName(first);
        form.setLastName(last);
        form.setEmail(email);
        return form;
    }

    private CourseForm course(String title, Level level, String price, int hours, Instructor instructor) {
        CourseForm form = new CourseForm();
        form.setTitle(title);
        form.setDescription("A sample course: " + title + ".");
        form.setLevel(level);
        form.setPrice(new BigDecimal(price));
        form.setDurationHours(hours);
        form.setInstructorId(instructor.getId());
        return form;
    }
}