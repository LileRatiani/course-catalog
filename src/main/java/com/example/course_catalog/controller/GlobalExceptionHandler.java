package com.example.course_catalog.controller;

import com.example.course_catalog.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Handles exceptions thrown from ANY controller in one place.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    // /courses/999 etc. -> friendly 404 page
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/not-found";
    }

    // Uploaded file is bigger than spring.servlet.multipart.max-file-size (5MB)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleFileTooLarge(HttpServletRequest request) {
        RequestContextUtils.getOutputFlashMap(request)
                .put("errorMessage", "The file is too large. Maximum size is 5 MB.");
        return "redirect:/courses";
    }
}