package com.balumbo.blb.repository;

import com.balumbo.blb.model.MailRow;
import com.balumbo.blb.model.SequenceList;
import com.balumbo.blb.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Repository
public interface SequenceListRepository extends CrudRepository<SequenceList,Long> {

    SequenceList findById(long id);
    ArrayList<SequenceList> findAllByUserId(long userId);
    ArrayList<SequenceList> findByMailListId(long id);
}
