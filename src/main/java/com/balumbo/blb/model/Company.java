package com.balumbo.blb.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;
import java.sql.Date;

@Data
@Entity
public class Company implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @SerializedName("orgnr") private String orgNo;
    @SerializedName("linkTo") private String linkTo;
    @SerializedName("jurnamn") private String cmpName;
    @SerializedName("abv_hgrupp") private String hBranch;
    @SerializedName("abv_ugrupp") private String uBranch;
    @SerializedName("ba_postort") private String city;
    @SerializedName("hasremarks") private boolean hasRemarks;
    private String executive;
    private long revenue;
    private long assets;
    private String telephone;
    private String visitAdress;
    private String postalAdress;
    private String county;
    private String regDate;
    private String website;
    private String email;
    private Date updatedInfo;
    private Date updatedWebsite;
}
