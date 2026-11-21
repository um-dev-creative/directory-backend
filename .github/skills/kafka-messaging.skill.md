---
name: Kafka Messaging
description: Skill for Spring Kafka outbound message production
applies-to:
  - Developer
---

# Kafka Messaging Skill

## Scope

This skill covers Spring Kafka outbound messaging patterns in the **directory-backend** project.

## Kafka Producer

### Email Message Producer
- Interface: `EmailMessageProducerService`
- Implementation: `EmailMessageProducerServiceImpl`
- Package: `com.umdc.directory.kafka.producer`
- Purpose: Send email notification events to Kafka topics for downstream processing.

### Kafka Configuration
- Config class: `com.umdc.directory.kafka.config`
- DTOs: `com.umdc.directory.kafka.to` — message payload records

## Pattern

```java
@Service
public class EmailMessageProducerServiceImpl implements EmailMessageProducerService {
    private final KafkaTemplate<String, EmailMessageTO> kafkaTemplate;

    // Constructor injection
    // send() method with topic, key, and payload
}
```

## Testing

Kafka is **disabled** in test profile (`application-test.yml`).
For unit tests, mock the `KafkaTemplate`:
```java
@Mock KafkaTemplate<String, EmailMessageTO> kafkaTemplate;
verify(kafkaTemplate).send(eq("topic"), eq("key"), any(EmailMessageTO.class));
```
