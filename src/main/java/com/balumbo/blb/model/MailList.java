package com.balumbo.blb.model;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Date;

@Data
@Entity
public class MailList implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(length=200000000,columnDefinition="LONGTEXT")
    private String fileName;
    private Date dispatchDate;
    @Column(length=200000000,columnDefinition="LONGTEXT")
    private String mainContent;
    @Column(length=200000000,columnDefinition="LONGTEXT")
    private String footerContent;
    @Column(length=200000000,columnDefinition="LONGTEXT")
    private String title;
    private boolean finished;
    private boolean ongoing;
    private long userId;
}
