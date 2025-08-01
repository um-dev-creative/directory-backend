package com.prx.directory.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prx.commons.constants.httpstatus.type.MessageType;
import com.prx.commons.exception.StandardException;
import com.prx.directory.kafka.to.EmailMessageTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class EmailMessageProducerServiceImpl implements EmailMessageProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${prx.producer.mercury.topic}")
    private String topic;

    private static final Logger logger = LoggerFactory.getLogger(EmailMessageProducerServiceImpl.class);

    public EmailMessageProducerServiceImpl(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        logger.info("EmailMessageProducerServiceImpl created");
    }

    @Override
    public void sendMessage(EmailMessageTO emailMessageTO) {
        try {
            String message = objectMapper.writeValueAsString(emailMessageTO);
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, message);
            logger.debug("Sending message: {}", message);
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Message sent: {} with offset: {}", message, result.getRecordMetadata().offset());
                } else {
                    logger.error("Error sending message: {} due to: {}", message, ex.getMessage(), ex);
                }
            });
        } catch (JsonProcessingException e) {
            throw new StandardException("Error serializing EmailMessageTO: " + e.getMessage(), MessageType.DEFAULT_MESSAGE, e);
        }
    }
}
