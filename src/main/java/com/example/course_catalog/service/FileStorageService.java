package com.example.course_catalog.service;

import com.example.course_catalog.exception.BusinessRuleException;
import com.example.course_catalog.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Saves uploaded files to a folder on disk and loads them again.
 * The database only stores the file NAME (Course.syllabusFileName).
 */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");

    private final Path uploadDir;

    // @Value reads "app.upload-dir" from application.properties.
    // This is still constructor injection: the value is passed into the constructor.
    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir); // create the folder if it doesn't exist
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create upload folder " + this.uploadDir, e);
        }
    }

    /**
     * Saves the file to disk and returns the generated file name.
     */
    public String store(MultipartFile file) {
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(originalName);

        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessRuleException("syllabus", "Only PDF, DOC or DOCX files are allowed");
        }

        // A random unique name: two uploads called "syllabus.pdf" never overwrite each other,
        // and a malicious name like "../../secret.txt" can never be used as a path.
        String storedName = UUID.randomUUID() + "." + extension.toLowerCase();
        Path target = uploadDir.resolve(storedName);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save file " + originalName, e);
        }
        return storedName;
    }

    /**
     * Loads a stored file so the controller can send it to the browser.
     */
    public Resource load(String fileName) {
        Path file = uploadDir.resolve(fileName).normalize();
        // Safety check: the file must be INSIDE the upload folder
        if (!file.startsWith(uploadDir) || !Files.exists(file)) {
            throw new NotFoundException("File " + fileName + " was not found");
        }
        return new FileSystemResource(file);
    }

    /**
     * Deletes a stored file (does nothing if fileName is null or the file is already gone).
     */
    public void delete(String fileName) {
        if (fileName == null) {
            return;
        }
        try {
            Files.deleteIfExists(uploadDir.resolve(fileName).normalize());
        } catch (IOException e) {
            // Not critical: the database is already correct, only a leftover file remains
            System.err.println("Could not delete file " + fileName + ": " + e.getMessage());
        }
    }
}