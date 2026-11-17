package com.example.course_catalog.dto;

import com.example.course_catalog.entity.Instructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data the user submits in the instructor form.
 * Validation annotations are checked when the controller uses @Valid.
 */
public class InstructorForm {

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must be at most 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must be at most 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String email;

    @Size(max = 1000, message = "Bio must be at most 1000 characters")
    private String bio;

    public InstructorForm() {
    }

    // Fills the form with existing data (used by the "edit" page)
    public static InstructorForm fromEntity(Instructor instructor) {
        InstructorForm form = new InstructorForm();
        form.setFirstName(instructor.getFirstName());
        form.setLastName(instructor.getLastName());
        form.setEmail(instructor.getEmail());
        form.setBio(instructor.getBio());
        return form;
    }

    // ---------- Getters and setters ----------

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}