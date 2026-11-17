package com.example.course_catalog.controller;

import com.example.course_catalog.service.CourseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final CourseService courseService;

    public HomeController(CourseService courseService) {
        this.courseService = courseService;
    }

    // GET / -> home page with the 5 most popular courses (GROUP BY @Query)
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("popularCourses", courseService.findMostPopular(5));
        return "home";
    }
}