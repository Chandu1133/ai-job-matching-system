package com.chandu.job_matching_system.entity;

import jakarta.persistence.*;

@Entity 
@Table(name = "users")

public class User{
    @Id 
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;
    private String name;
    private String email;
    private String password;
    private String role;
}