package com.balumbo.blb.repository;

import com.balumbo.blb.model.MailList;
import com.balumbo.blb.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Repository
public interface MailListRepository extends CrudRepository<MailList,Long> {

    MailList findById(long id);
    ArrayList<MailList> findAllByUserId(long userId);
    ArrayList<MailList> findAllByFinishedAndUserId(boolean finished, long userId);
}
