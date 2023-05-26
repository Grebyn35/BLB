package com.balumbo.blb.repository;

import com.balumbo.blb.model.Blacklist;
import com.balumbo.blb.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Repository
public interface BlacklistRepository extends CrudRepository<Blacklist,Long> {

    Blacklist findByEmail(String email);
    Blacklist findById(long id);
    ArrayList<Blacklist> findAll();
    ArrayList<Blacklist> findAllByUserId(long userId);
}