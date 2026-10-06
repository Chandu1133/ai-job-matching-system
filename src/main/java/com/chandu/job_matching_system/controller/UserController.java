package com.chandu.job_matching_system.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController 
public class UserController {
    @GetMapping ("/api/user/hello")
    public String hello(){
        return "Welcome to AI Job Matching Platform";
    }
}
