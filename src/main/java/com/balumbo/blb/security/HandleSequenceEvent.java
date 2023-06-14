package com.balumbo.blb.security;

import com.balumbo.blb.model.MailList;
import com.balumbo.blb.model.SequenceList;
import lombok.Data;

@Data
public class HandleSequenceEvent {
    private SequenceList sequenceList;

    public HandleSequenceEvent(SequenceList sequenceList) {
        this.sequenceList = sequenceList;
    }
}
