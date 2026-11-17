package com.example.course_catalog.dto;

import com.example.course_catalog.entity.Course;
import com.example.course_catalog.entity.Level;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Data the user submits in the course form.
 * The syllabus FILE is not here: the controller receives it separately
 * with @RequestParam("syllabus") MultipartFile.
 */
public class CourseForm {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    @NotNull(message = "Please choose a level")
    private Level level;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", message = "Price cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Price can have at most 2 decimal places")
    private BigDecimal price;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 hour")
    @Max(value = 1000, message = "Duration must be at most 1000 hours")
    private Integer durationHours;

    // The form sends only the instructor's ID (selected from a dropdown)
    @NotNull(message = "Please choose an instructor")
    private Long instructorId;

    public CourseForm() {
    }

    public static CourseForm fromEntity(Course course) {
        CourseForm form = new CourseForm();
        form.setTitle(course.getTitle());
        form.setDescription(course.getDescription());
        form.setLevel(course.getLevel());
        form.setPrice(course.getPrice());
        form.setDurationHours(course.getDurationHours());
        form.setInstructorId(course.getInstructor().getId());
        return form;
    }

    // ---------- Getters and setters ----------

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }

    public Long getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Long instructorId) {
        this.instructorId = instructorId;
    }
}