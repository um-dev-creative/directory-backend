package com.prx.directory.kafka.producer;

import com.prx.directory.kafka.to.EmailMessageTO;
import org.springframework.http.HttpStatus;

public interface EmailMessageProducerService {


    default void sendMessage(EmailMessageTO emailMessageTO) {
        throw new UnsupportedOperationException(HttpStatus.NOT_IMPLEMENTED.getReasonPhrase());
    }
}
