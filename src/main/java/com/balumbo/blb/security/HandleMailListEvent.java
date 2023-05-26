package com.balumbo.blb.security;

import com.balumbo.blb.model.MailList;
import lombok.Data;

@Data
public class HandleMailListEvent {
    private MailList mailList;

    public HandleMailListEvent(MailList mailList) {
        this.mailList = mailList;
    }
}

