package com.balumbo.blb.model;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
public class MailRow implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private long mailListId;
    @Column(length=200000000,columnDefinition="LONGTEXT")
    private String dataRow;
    private String email;
    private boolean sent;
    private boolean error;
    private boolean isHeader;
    private boolean opened;
    private String timeOpened;
    private int timesOpened;
    private long userId;

}
