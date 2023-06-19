package com.balumbo.blb.repository;

import com.balumbo.blb.model.MailRow;
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
public interface MailRowRepository extends CrudRepository<MailRow,Long> {

    MailRow findById(long id);
    ArrayList<MailRow> findAllByUserId(long userId);
    ArrayList<MailRow> findByMailListId(long id);
    ArrayList<MailRow> findByMailListIdAndErrorIsFalse(long id);
    MailRow findFirstByMailListIdAndIsHeader(long id, boolean header);

    //Dessa är till för första utskick
    ArrayList<MailRow> findByMailListIdAndSentAndErrorAndIsHeaderIsFalseAndEmailNotIn(long id, boolean sent, boolean error, ArrayList<String> email);
    ArrayList<MailRow> findByMailListIdAndSentAndErrorAndIsHeaderIsFalse(long id, boolean sent, boolean error);

    //Dessa är till för sekvenser
    @Query("SELECT m FROM MailRow m WHERE m.mailListId = :id AND m.error = :error AND m.isHeader = false AND m.sentDate <= :date AND m.email NOT IN :emails")
    ArrayList<MailRow> findByMailListIdAndErrorAndIsHeaderIsFalseAndSentDateEqualOrAfterXDaysAndEmailNotIn(@Param("id") long id, @Param("error") boolean error, @Param("date") Date date, @Param("emails") ArrayList<String> emails);
    @Query("SELECT m FROM MailRow m WHERE m.mailListId = :id AND m.error = :error AND m.isHeader = false AND m.sentDate <= :date")
    ArrayList<MailRow> findByMailListIdAndErrorAndIsHeaderIsFalseAndSentDateEqualOrAfterXDays(@Param("id") long id, @Param("error") boolean error, @Param("date") Date date);


    Page<MailRow> findByMailListIdAndIsHeader(long id, boolean header, Pageable pageable);
}
