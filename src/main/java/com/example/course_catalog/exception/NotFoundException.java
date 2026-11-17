package com.example.course_catalog.exception;

/**
 * Thrown when a requested record (instructor, student, course) does not exist.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}