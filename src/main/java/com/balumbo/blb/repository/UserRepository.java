package com.balumbo.blb.repository;

import com.balumbo.blb.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public interface UserRepository extends CrudRepository<User,Long> {

    User findByEmail(String email);
    User findById(long id);
    ArrayList<User> findAll();
}

