package com.balumbo.blb.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.Email;
import java.io.Serializable;
import java.util.ArrayList;

@Data
@Entity
public class User implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Email(regexp = "^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,6}$", message = "Enter a valid email")
    private String email;
    private String password;
    private  String role = "ROLE_USER";

    private String mailEmail;
    private String mailAlias;
    private String mailPassword;
    private String mailHost;
    private String mailPort;
    private boolean error;
}


