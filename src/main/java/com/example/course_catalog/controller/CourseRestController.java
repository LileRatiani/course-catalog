package com.example.course_catalog.controller;

import com.example.course_catalog.dto.CourseForm;
import com.example.course_catalog.dto.CourseResponse;
import com.example.course_catalog.dto.PageResponse;
import com.example.course_catalog.entity.Course;
import com.example.course_catalog.entity.Level;
import com.example.course_catalog.exception.BusinessRuleException;
import com.example.course_catalog.exception.NotFoundException;
import com.example.course_catalog.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * JSON REST API for courses.
 * @RestController = @Controller + @ResponseBody: return values are converted to JSON
 * instead of being treated as template names.
 */
@RestController
@RequestMapping("/api/courses")
public class CourseRestController {

    private static final Set<String> SORTABLE_FIELDS = Set.of("title", "price", "durationHours", "createdAt");

    private final CourseService courseService;

    // Same service as the HTML CourseController: no business logic is duplicated
    public CourseRestController(CourseService courseService) {
        this.courseService = courseService;
    }

    // GET /api/courses?search=java&level=BEGINNER&page=0&size=5&sort=price,desc
    @GetMapping
    public PageResponse<CourseResponse> list(@RequestParam(defaultValue = "") String search,
                                             @RequestParam(required = false) Level level,
                                             @PageableDefault(size = 5, sort = "title") Pageable pageable) {
        return PageResponse.from(
                courseService.search(search, level, onlyAllowedSorting(pageable)),
                CourseResponse::from);
    }

    // GET /api/courses/3
    @GetMapping("/{id}")
    public CourseResponse get(@PathVariable Long id) {
        return CourseResponse.from(courseService.findById(id));
    }

    // POST /api/courses   (JSON body) -> 201 Created + Location header
    @PostMapping
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CourseForm form) {
        Course created = courseService.create(form, null);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(CourseResponse.from(created));
    }

    // PUT /api/courses/3   (JSON body) -> replaces the course data, 200 OK
    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseForm form) {
        return CourseResponse.from(courseService.update(id, form, null));
    }

    // DELETE /api/courses/3 -> 204 No Content
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // POST /api/courses/3/students/7 -> enroll student 7 in course 3
    @PostMapping("/{id}/students/{studentId}")
    public CourseResponse enroll(@PathVariable Long id, @PathVariable Long studentId) {
        courseService.enroll(id, studentId);
        return CourseResponse.from(courseService.findById(id));
    }

    // DELETE /api/courses/3/students/7 -> remove student 7 from course 3
    @DeleteMapping("/{id}/students/{studentId}")
    public ResponseEntity<Void> unenroll(@PathVariable Long id, @PathVariable Long studentId) {
        courseService.unenroll(id, studentId);
        return ResponseEntity.noContent().build();
    }

    // =====================================================================
    // Error handling for THIS controller (returns JSON instead of HTML pages).
    // Handlers inside a controller take priority over @ControllerAdvice.
    // =====================================================================

    // @Valid @RequestBody failed -> 400 with one message per field
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400,
                "error", "Validation failed",
                "fields", fieldErrors));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", 404, "error", ex.getMessage()));
    }

    // Business rule broken (e.g. student already enrolled) -> 409 Conflict
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRule(BusinessRuleException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("status", 409, "error", ex.getMessage()));
    }

    private Pageable onlyAllowedSorting(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("title"));
            }
        }
        return pageable;
    }
}