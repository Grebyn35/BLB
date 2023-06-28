package com.balumbo.blb.repository;

import com.balumbo.blb.model.BatchUpdater;
import com.balumbo.blb.model.Company;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Repository
public interface BatchUpdaterRepository extends CrudRepository<BatchUpdater,Long> {
    ArrayList<BatchUpdater> findAll();
}
