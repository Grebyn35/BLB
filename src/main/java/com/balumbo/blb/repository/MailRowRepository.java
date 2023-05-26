package com.balumbo.blb.repository;

import com.balumbo.blb.model.MailRow;
import com.balumbo.blb.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Repository
public interface MailRowRepository extends CrudRepository<MailRow,Long> {

    MailRow findById(long id);
    ArrayList<MailRow> findAllByUserId(long userId);
    ArrayList<MailRow> findByMailListId(long id);
    MailRow findFirstByMailListIdAndIsHeader(long id, boolean header);
    ArrayList<MailRow> findByMailListIdAndSentAndErrorAndEmailNotIn(long id, boolean sent, boolean error, ArrayList<String> email);
    ArrayList<MailRow> findByMailListIdAndSentAndError(long id, boolean sent, boolean error);
    Page<MailRow> findByMailListIdAndIsHeader(long id, boolean header, Pageable pageable);
}
