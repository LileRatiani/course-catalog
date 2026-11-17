package com.example.course_catalog.controller;

import com.example.course_catalog.dto.StudentForm;
import com.example.course_catalog.entity.Student;
import com.example.course_catalog.exception.BusinessRuleException;
import com.example.course_catalog.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // GET /students
    @GetMapping
    public String list(Model model) {
        model.addAttribute("students", studentService.findAll());
        return "students/list";
    }

    // GET /students/5 -> uses the custom JOIN @Query to list the student's courses
    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("student", studentService.findById(id));
        model.addAttribute("courses", studentService.findCoursesOf(id));
        return "students/details";
    }

    // GET /students/new
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new StudentForm());
        return "students/form";
    }

    // POST /students
    @PostMapping
    public String create(@Valid @ModelAttribute("form") StudentForm form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "students/form";
        }
        Student saved;
        try {
            saved = studentService.create(form);
        } catch (BusinessRuleException e) {
            bindingResult.rejectValue(e.getField(), "duplicate", e.getMessage());
            return "students/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Student " + saved.getFullName() + " was created.");
        return "redirect:/students/" + saved.getId();
    }

    // GET /students/5/edit
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", StudentForm.fromEntity(studentService.findById(id)));
        model.addAttribute("studentId", id);
        return "students/form";
    }

    // POST /students/5
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") StudentForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("studentId", id);
            return "students/form";
        }
        try {
            studentService.update(id, form);
        } catch (BusinessRuleException e) {
            bindingResult.rejectValue(e.getField(), "duplicate", e.getMessage());
            model.addAttribute("studentId", id);
            return "students/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Student was updated.");
        return "redirect:/students/" + id;
    }

    // POST /students/5/delete
    // (No BusinessRuleException here: deleting a student is always allowed,
    //  the service removes them from their courses first.)
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        studentService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Student was deleted.");
        return "redirect:/students";
    }
}