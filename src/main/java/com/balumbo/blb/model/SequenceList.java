package com.balumbo.blb.model;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Date;

@Data
@Entity
public class SequenceList implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private long mailListId;
    @Column(length=200000000,columnDefinition="LONGTEXT")
    private String mainContent;
    @Column(length=200000000,columnDefinition="LONGTEXT")
    private String title;
    private int sequenceAfterDays;
    private long userId;
    private boolean ongoing;
    private boolean startedSending;
    private boolean finished;

    private Date sendingDate;

}
