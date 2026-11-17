package com.example.course_catalog.controller;

import com.example.course_catalog.dto.CourseForm;
import com.example.course_catalog.entity.Course;
import com.example.course_catalog.entity.Level;
import com.example.course_catalog.exception.BusinessRuleException;
import com.example.course_catalog.service.CourseService;
import com.example.course_catalog.service.InstructorService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/courses")
public class CourseController {

    // Only these fields may be used for sorting (protects against ?sort=anything)
    private static final Set<String> SORTABLE_FIELDS = Set.of("title", "price", "durationHours", "createdAt");

    // Options shown in the "Sort by" dropdown: value -> label
    private static final Map<String, String> SORT_OPTIONS = new LinkedHashMap<>();

    static {
        SORT_OPTIONS.put("title,asc", "Title (A-Z)");
        SORT_OPTIONS.put("title,desc", "Title (Z-A)");
        SORT_OPTIONS.put("price,asc", "Price (low to high)");
        SORT_OPTIONS.put("price,desc", "Price (high to low)");
        SORT_OPTIONS.put("durationHours,asc", "Duration (short first)");
        SORT_OPTIONS.put("durationHours,desc", "Duration (long first)");
        SORT_OPTIONS.put("createdAt,desc", "Newest first");
    }

    private final CourseService courseService;
    private final InstructorService instructorService;

    public CourseController(CourseService courseService, InstructorService instructorService) {
        this.courseService = courseService;
        this.instructorService = instructorService;
    }

    // =====================================================================
    // LIST: search + filter + pagination + sorting
    // Example URL: /courses?search=java&level=BEGINNER&page=1&size=5&sort=price,asc
    // =====================================================================
    @GetMapping
    public String list(@RequestParam(defaultValue = "") String search,
                       @RequestParam(required = false) Level level,
                       @PageableDefault(size = 5, sort = "title", direction = Sort.Direction.ASC) Pageable pageable,
                       Model model) {
        Pageable safePageable = onlyAllowedSorting(pageable);
        Page<Course> page = courseService.search(search, level, safePageable);

        model.addAttribute("page", page);
        model.addAttribute("search", search);
        model.addAttribute("level", level);
        model.addAttribute("sort", toSortParam(safePageable.getSort()));
        model.addAttribute("levels", Level.values());
        model.addAttribute("sortOptions", SORT_OPTIONS);
        return "courses/list";
    }

    // =====================================================================
    // DETAILS
    // =====================================================================
    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("course", courseService.findById(id));
        model.addAttribute("availableStudents", courseService.findStudentsNotEnrolled(id));
        return "courses/details";
    }

    // =====================================================================
    // CREATE (with file upload)
    // =====================================================================
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new CourseForm());
        prepareForm(model, null);
        return "courses/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CourseForm form,
                         BindingResult bindingResult,
                         @RequestParam(value = "syllabus", required = false) MultipartFile syllabus,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, null);
            return "courses/form";
        }
        Course saved;
        try {
            saved = courseService.create(form, syllabus);
        } catch (BusinessRuleException e) {
            addBusinessError(e, bindingResult, model);
            prepareForm(model, null);
            return "courses/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Course \"" + saved.getTitle() + "\" was created.");
        return "redirect:/courses/" + saved.getId();
    }

    // =====================================================================
    // EDIT (optionally replace the file)
    // =====================================================================
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", CourseForm.fromEntity(courseService.findById(id)));
        prepareForm(model, id);
        return "courses/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") CourseForm form,
                         BindingResult bindingResult,
                         @RequestParam(value = "syllabus", required = false) MultipartFile syllabus,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, id);
            return "courses/form";
        }
        try {
            courseService.update(id, form, syllabus);
        } catch (BusinessRuleException e) {
            addBusinessError(e, bindingResult, model);
            prepareForm(model, id);
            return "courses/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Course was updated.");
        return "redirect:/courses/" + id;
    }

    // =====================================================================
    // DELETE
    // =====================================================================
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        courseService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Course was deleted.");
        return "redirect:/courses";
    }

    // =====================================================================
    // ENROLL / UNENROLL (Many-to-Many)
    // =====================================================================
    @PostMapping("/{id}/enroll")
    public String enroll(@PathVariable Long id,
                         @RequestParam Long studentId,
                         RedirectAttributes redirectAttributes) {
        try {
            courseService.enroll(id, studentId);
            redirectAttributes.addFlashAttribute("successMessage", "Student was enrolled.");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/courses/" + id;
    }

    @PostMapping("/{id}/unenroll")
    public String unenroll(@PathVariable Long id,
                           @RequestParam Long studentId,
                           RedirectAttributes redirectAttributes) {
        try {
            courseService.unenroll(id, studentId);
            redirectAttributes.addFlashAttribute("successMessage", "Student was removed from the course.");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/courses/" + id;
    }

    // =====================================================================
    // DOWNLOAD the uploaded syllabus
    // =====================================================================
    @GetMapping("/{id}/syllabus")
    public ResponseEntity<Resource> downloadSyllabus(@PathVariable Long id) {
        Resource file = courseService.loadSyllabus(id);
        MediaType type = MediaTypeFactory.getMediaType(file).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(type)
                // "inline" = open in the browser (PDFs) instead of forcing a download
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.getFilename()).build().toString())
                .body(file);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    // Data every course form needs: dropdown options + (when editing) the existing course
    private void prepareForm(Model model, Long courseId) {
        model.addAttribute("levels", Level.values());
        model.addAttribute("instructors", instructorService.findAll());
        if (courseId != null) {
            model.addAttribute("course", courseService.findById(courseId));
        }
    }

    // Shows a BusinessRuleException in the right place on the form
    private void addBusinessError(BusinessRuleException e, BindingResult bindingResult, Model model) {
        if ("syllabus".equals(e.getField())) {
            // the file is not part of CourseForm, so it gets its own model attribute
            model.addAttribute("syllabusError", e.getMessage());
        } else if (e.getField() != null) {
            bindingResult.rejectValue(e.getField(), "business", e.getMessage());
        } else {
            bindingResult.reject("business", e.getMessage()); // general (global) form error
        }
    }

    // If someone puts an unknown field in ?sort=, fall back to sorting by title
    private Pageable onlyAllowedSorting(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("title"));
            }
        }
        return pageable;
    }

    // Sort -> "price,asc" (used to keep the sort when clicking page links)
    private String toSortParam(Sort sort) {
        Sort.Order order = sort.iterator().hasNext() ? sort.iterator().next() : Sort.Order.asc("title");
        return order.getProperty() + "," + order.getDirection().name().toLowerCase();
    }
}