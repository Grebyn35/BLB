package com.balumbo.blb.repository;

import com.balumbo.blb.model.Company;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@Repository
public interface CompanyRepository extends CrudRepository<Company,Long> {
    Company findByCmpName(String cmpName);
    Company findByOrgNo(String orgNo);
    ArrayList<Company> findAll();
    ArrayList<Company> findAllByOrgNoNotIn(ArrayList<String> orgNo);
    List<Company> findByOrgNoIn(List<String> orgNos);

    @Query(value = "SELECT c FROM Company c WHERE c.updatedInfo <= :date OR c.updatedInfo IS NULL ORDER BY function('RAND')")
    ArrayList<Company> findCompaniesOlderThanForInfo(@Param("date") Date date, Pageable pageable);

    @Query(value = "SELECT c FROM Company c WHERE c.updatedWebsite <= :date OR c.updatedWebsite IS NULL ORDER BY function('RAND')")
    ArrayList<Company> findCompaniesOlderThanForWebsite(@Param("date") Date date, Pageable pageable);
}
