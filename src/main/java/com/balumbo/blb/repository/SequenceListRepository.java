package com.balumbo.blb.repository;

import com.balumbo.blb.model.MailList;
import com.balumbo.blb.model.MailRow;
import com.balumbo.blb.model.SequenceList;
import com.balumbo.blb.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.ArrayList;

@Repository
public interface SequenceListRepository extends CrudRepository<SequenceList,Long> {

    SequenceList findById(long id);
    ArrayList<SequenceList> findAllByUserId(long userId);
    ArrayList<SequenceList> findByMailListId(long id);
    ArrayList<SequenceList> findAll();
    ArrayList<SequenceList> findAllByOngoingAndFinished(@Param("ongoing") boolean ongoing, boolean finished);
    ArrayList<SequenceList> findAllByFinished(boolean finished);
    @Query("SELECT m FROM SequenceList m WHERE (m.sequenceAfterDays <= :date)")
    ArrayList<SequenceList> findAllBySequenceAfterDaysEqualOrBefore(@Param("date") Date date);

}
