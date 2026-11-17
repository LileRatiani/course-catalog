package com.example.course_catalog.controller;

import com.example.course_catalog.dto.InstructorForm;
import com.example.course_catalog.entity.Instructor;
import com.example.course_catalog.exception.BusinessRuleException;
import com.example.course_catalog.service.InstructorService;
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
@RequestMapping("/instructors") // every URL in this class starts with /instructors
public class InstructorController {

    private final InstructorService instructorService;

    // Constructor injection: the service is passed in by Spring
    public InstructorController(InstructorService instructorService) {
        this.instructorService = instructorService;
    }

    // GET /instructors -> list page
    @GetMapping
    public String list(Model model) {
        model.addAttribute("instructors", instructorService.findAll());
        return "instructors/list"; // templates/instructors/list.html
    }

    // GET /instructors/5 -> details page of instructor 5
    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("instructor", instructorService.findById(id));
        model.addAttribute("courses", instructorService.findCoursesOf(id));
        return "instructors/details";
    }

    // GET /instructors/new -> empty form
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new InstructorForm());
        return "instructors/form";
    }

    // POST /instructors -> the form was submitted (create)
    @PostMapping
    public String create(@Valid @ModelAttribute("form") InstructorForm form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        // 1) Validation annotations failed (e.g. empty name, bad email) -> show the form again with errors
        if (bindingResult.hasErrors()) {
            return "instructors/form";
        }
        // 2) Business rule failed (e.g. email already used) -> show the error under the field
        Instructor saved;
        try {
            saved = instructorService.create(form);
        } catch (BusinessRuleException e) {
            bindingResult.rejectValue(e.getField(), "duplicate", e.getMessage());
            return "instructors/form";
        }
        // 3) Success -> redirect (Post/Redirect/Get), message survives the redirect via flash attribute
        redirectAttributes.addFlashAttribute("successMessage", "Instructor " + saved.getFullName() + " was created.");
        return "redirect:/instructors/" + saved.getId();
    }

    // GET /instructors/5/edit -> form filled with existing data
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Instructor instructor = instructorService.findById(id);
        model.addAttribute("form", InstructorForm.fromEntity(instructor));
        model.addAttribute("instructorId", id); // tells the template we are editing
        return "instructors/form";
    }

    // POST /instructors/5 -> the edit form was submitted (update)
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") InstructorForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("instructorId", id);
            return "instructors/form";
        }
        try {
            instructorService.update(id, form);
        } catch (BusinessRuleException e) {
            bindingResult.rejectValue(e.getField(), "duplicate", e.getMessage());
            model.addAttribute("instructorId", id);
            return "instructors/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Instructor was updated.");
        return "redirect:/instructors/" + id;
    }

    // POST /instructors/5/delete -> delete button
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            instructorService.delete(id);
        } catch (BusinessRuleException e) {
            // e.g. "Cannot delete: they still teach 2 course(s)"
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/instructors/" + id;
        }
        redirectAttributes.addFlashAttribute("successMessage", "Instructor was deleted.");
        return "redirect:/instructors";
    }
}