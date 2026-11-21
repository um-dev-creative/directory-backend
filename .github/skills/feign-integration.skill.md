---
name: Feign Integration
description: Skill for OpenFeign client patterns for inter-service communication
applies-to:
  - Developer
---

# Feign Integration Skill

## Scope

This skill covers OpenFeign client patterns for inter-service HTTP communication
in the **directory-backend** project.

## Feign Clients

### Backbone Client
- Interface: `com.umdc.backbone.client.directory.BackboneClient`
- Configurer: `BackboneFeignConfigurer`
- Purpose: Core platform operations, user management

### Mercury Client
- Interface: `com.umdc.mercury.client.directory.MercuryClient`
- Configurer: `MercuryFeignConfigurer`
- Purpose: Communication/notification service integration

## Configuration

Feign clients are enabled via `@EnableFeignClients(basePackages = "com.umdc.directory.client")`
on the main application class.

Auth credentials are configured per client in `bootstrap.yml` with environment variables:
- Backbone: `BACKBONE_CLIENT_ID`, `BACKBONE_CLIENT_SECRET`, `BACKBONE_GRANT_TYPE`, etc.
- Mercury: `MERCURY_CLIENT_ID`, `MERCURY_CLIENT_SECRET`, `MERCURY_GRANT_TYPE`, etc.

## Interceptor

Request interceptors in `com.umdc.directory.client.interceptor` handle token injection
and header propagation for outbound Feign requests.

## Testing

Mock Feign clients in unit tests:
```java
@Mock BackboneClient backboneClient;
when(backboneClient.someMethod(any())).thenReturn(expectedResponse);
```
