# TASK-011: Implement Kafka Email Producer Service

## Priority
**High**

## Status
Open

## Component
- Email Messaging
- Package: `com.umdc.directory.kafka.producer`
- Class: `EmailMessageProducerService` (interface)
- Implementation: `EmailMessageProducerServiceImpl`

## Description
The `EmailMessageProducerService` interface currently has a default method that throws `UnsupportedOperationException`. While `EmailMessageProducerServiceImpl` is implemented, the interface default method should be removed or properly implemented.

Additionally, verify that the Kafka email producer is fully functional and tested across all use cases where emails need to be sent.

## Current State
```java
default void sendEmailWithTemplate(EmailMessageTO emailMessage) {
    throw new UnsupportedOperationException(HttpStatus.NOT_IMPLEMENTED.getReasonPhrase());
}
```

## Expected Behavior
- Interface should not have default implementation throwing exception
- Kafka producer sends email messages successfully
- All email use cases are implemented:
  - User registration confirmation
  - Password reset
  - Business creation notification
  - Campaign launch notification
  - Favorite product updates (optional)

## Acceptance Criteria
1. **Interface cleanup**
   - Remove default implementation from `EmailMessageProducerService`
   - Make method abstract (standard interface pattern)

2. **Implementation verification**
   - `EmailMessageProducerServiceImpl.sendEmailWithTemplate()` works correctly
   - Messages are sent to correct Kafka topic
   - Message format follows `EmailMessageTO` structure

3. **Email use cases implemented**
   - [ ] User registration confirmation email
   - [ ] Password reset email
   - [ ] Welcome email after registration
   - [ ] Business creation confirmation
   - [ ] Campaign launch notification (optional)

4. **Error handling**
   - Kafka send failures are logged
   - Failed messages are retried or dead-lettered
   - Exceptions don't break main application flow

5. **Testing**
   - Unit test for email producer service
   - Integration test with embedded Kafka
   - Test various email templates
   - Test error scenarios (Kafka unavailable)

## Technical Tasks
- [ ] Remove default implementation from `EmailMessageProducerService` interface
- [ ] Verify `EmailMessageProducerServiceImpl` implementation
- [ ] Verify Kafka topic configuration
- [ ] Implement user registration confirmation email
- [ ] Implement password reset email
- [ ] Implement welcome email
- [ ] Add error handling and retry logic
- [ ] Add logging for email send operations
- [ ] Create unit tests for email producer
- [ ] Create integration tests with embedded Kafka
- [ ] Add monitoring/metrics for email sends

## Email Use Cases Detail

### 1. User Registration Confirmation
**Trigger:** User completes registration
**Template:** registration-confirmation
**Data:**
- User name
- Confirmation code
- Confirmation link
- Expiration time

### 2. Password Reset
**Trigger:** User requests password reset
**Template:** password-reset
**Data:**
- User name
- Reset token
- Reset link
- Expiration time

### 3. Welcome Email
**Trigger:** User confirms registration
**Template:** welcome
**Data:**
- User name
- Getting started guide link
- Support contact

### 4. Business Creation Notification
**Trigger:** Business is created/approved
**Template:** business-created
**Data:**
- Business name
- Business owner
- Next steps
- Dashboard link

## Testing Requirements

### Unit Tests
- Send email message successfully
- Handle null email message
- Handle invalid recipient
- Handle Kafka send failure

### Integration Tests
- Send email to embedded Kafka
- Verify message on Kafka topic
- Test with multiple recipients
- Test with attachments (if supported)

## Kafka Configuration Verification
Verify these configurations exist:
```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    template:
      default-topic: directory-emails
```

## Dependencies
- Spring Kafka (already in pom.xml)
- Kafka broker (runtime dependency)
- Email templates (may need to create/configure)

## Estimated Effort
**8-10 hours**
- Interface cleanup: 30 minutes
- Implementation verification: 1 hour
- Email use case implementation: 4 hours
- Error handling and retry: 2 hours
- Testing: 3 hours
- Documentation: 30 minutes

## References
- Interface: `src/main/java/com/prx/directory/kafka/producer/EmailMessageProducerService.java`
- Implementation: `src/main/java/com/prx/directory/kafka/producer/EmailMessageProducerServiceImpl.java`
- Email DTO: `src/main/java/com/prx/directory/kafka/to/EmailMessageTO.java`
- Recipient DTO: `src/main/java/com/prx/directory/kafka/to/Recipient.java`

## Quality Gates
- [ ] Code coverage ≥ 80% for email producer service
- [ ] All tests pass (unit + integration)
- [ ] No PMD violations
- [ ] Kafka messages successfully sent in test environment

## Monitoring & Observability
Add metrics for:
- Email send success count
- Email send failure count
- Email send duration
- Kafka producer lag

Add logging for:
- Email send initiated (INFO)
- Email sent successfully (INFO)
- Email send failed (ERROR with exception)

## Security Considerations
- Ensure email content doesn't leak sensitive data
- Sanitize user input in email templates
- Use secure Kafka connections (SSL/TLS) in production
- Rate limit email sends to prevent spam

## Business Value
Functional email notifications improve:
- **User engagement** - timely communication
- **User onboarding** - guided experience
- **Security** - password reset and confirmation
- **Transparency** - status updates for business operations

## Notes
This service is critical for user communication. Ensure robust error handling so email failures don't impact core business operations. Consider implementing a fallback mechanism or admin notification for persistent failures.

