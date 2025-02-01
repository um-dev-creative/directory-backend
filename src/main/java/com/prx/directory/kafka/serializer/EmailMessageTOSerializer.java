package com.prx.directory.kafka.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prx.directory.kafka.to.EmailMessageTO;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailMessageTOSerializer implements Serializer<EmailMessageTO> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailMessageTOSerializer.class);

    @Override
    public byte[] serialize(String topic, EmailMessageTO data) {
        try {
            return new ObjectMapper().writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error serializing EmailMessageTO: {}", e.getMessage());
            return new byte[0];
        }
    }

    @Override
    public void close() {
        // Nothing to do
    }
}
