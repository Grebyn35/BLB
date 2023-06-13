package com.balumbo.blb.repository;

import com.balumbo.blb.model.MailList;
import com.balumbo.blb.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.ArrayList;

@Repository
public interface MailListRepository extends CrudRepository<MailList,Long> {

    MailList findById(long id);
    ArrayList<MailList> findAllByUserId(long userId);
    ArrayList<MailList> findAllByFinishedAndUserId(boolean finished, long userId);
    @Query("SELECT m FROM MailList m WHERE m.finished = :finished AND m.ongoing = :ongoing AND (m.dispatchDate <= :date)")
    ArrayList<MailList> findAllByFinishedAndDispatchDateEqualOrBeforeAndOngoing(@Param("finished") boolean finished, @Param("ongoing") boolean ongoing, @Param("date") Date date);

    @Query("SELECT m FROM MailList m WHERE m.finished = :finished AND (m.dispatchDate <= :date)")
    ArrayList<MailList> findAllByFinishedAndDispatchDateEqualOrBefore(@Param("finished") boolean finished, @Param("date") Date date);

    Page<MailList> findAllByFinishedAndUserIdAndFinishedUploadingIsTrueOrderByDispatchDate(boolean finished, long userId, Pageable pageable);
}
