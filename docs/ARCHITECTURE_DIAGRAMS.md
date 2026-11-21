# Directory Backend - Architecture & Flowchart Diagrams

Visual representation of system architecture, data flows, and operational processes.

---

## 1. System Architecture Overview

```
┌────────────────────────────────────────────────────────────────────────────┐
│                          CLIENT APPLICATIONS                                │
│              (Web Portal, Mobile App, Third-party Integrations)            │
└────────────────────────────┬─────────────────────────────────────────────────┘
                              │
                              │ HTTPS/REST
                              ↓
┌────────────────────────────────────────────────────────────────────────────┐
│                     DIRECTORY-BACKEND MICROSERVICE                          │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                    API LAYER (Controllers)                           │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐   │   │
│  │  │ Campaign    │ │ Business    │ │ Product     │ │ User/       │   │   │
│  │  │ Controller  │ │ Controller  │ │ Controller  │ │ Favorite    │   │   │
│  │  └──────┬──────┘ └──────┬──────┘ └──────┬──────┘ │ Controller  │   │   │
│  │         │                │                │      └──────┬──────┘   │   │
│  │         └────────────────┼────────────────┼─────────────┘           │   │
│  │                          ↓                                          │   │
│  │                   ┌─────────────────┐                               │   │
│  │                   │ Security Filter │ (JWT Validation)             │   │
│  │                   └────────┬────────┘                               │   │
│  └────────────────────────────┼──────────────────────────────────────┘   │
│                               ↓                                            │
│  ┌────────────────────────────────────────────────────────────────────┐   │
│  │                   SERVICE LAYER (Business Logic)                   │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────┐  │   │
│  │  │ Campaign     │ │ Business     │ │ Product      │ │ User     │  │   │
│  │  │ Service      │ │ Service      │ │ Service      │ │ Service  │  │   │
│  │  │              │ │              │ │              │ │          │  │   │
│  │  │ • Validation │ │ • Validation │ │ • Validation │ │ • Auth   │  │   │
│  │  │ • Filtering  │ │ • Filtering  │ │ • Filtering  │ │ • JWT    │  │   │
│  │  │ • Auth check │ │ • Auth check │ │ • Auth check │ │ • Hash   │  │   │
│  │  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └────┬─────┘  │   │
│  │         │                │                │              │        │   │
│  └────────────────────────────┼────────────────────────────┼────────┘   │
│                               ↓                             │            │
│  ┌────────────────────────────────────────────────────────┼─────────┐  │
│  │                   MAPPER LAYER (MapStruct)            │         │  │
│  │           Entity ←→ DTO Transformation                 ↓         │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                               ↓                                        │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │              REPOSITORY LAYER (Spring Data JPA)                    │  │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────┐  │  │
│  │  │ Campaign     │ │ Business     │ │ Product      │ │ User     │  │  │
│  │  │ Repository   │ │ Repository   │ │ Repository   │ │ Repo     │  │  │
│  │  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └────┬─────┘  │  │
│  │         └────────────────┼────────────────┼──────────────┘         │  │
│  └────────────────────────────┼──────────────────────────────────────┘  │
│                               ↓                                        │
└────────────────────────────────┼──────────────────────────────────────┘
                                 │
                ┌────────────────┼────────────────┐
                ↓                ↓                ↓
        ┌──────────────┐ ┌──────────────┐ ┌─────────────┐
        │  PostgreSQL  │ │   Kafka      │ │   External  │
        │  Database    │ │   Messaging  │ │   Services  │
        │              │ │              │ │             │
        │ • Campaigns  │ │ • Email      │ │ • Backbone  │
        │ • Products   │ │   Notifications│• Mercury   │
        │ • Users      │ │ • Async Tasks│ │             │
        │ • Favorites  │ │              │ │             │
        └──────────────┘ └──────────────┘ └─────────────┘
```

---

## 2. Request/Response Lifecycle

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         COMPLETE REQUEST CYCLE                               │
└─────────────────────────────────────────────────────────────────────────────┘

CLIENT REQUEST (HTTP)
        │
        │ POST /api/v1/campaigns
        │ {
        │   "name": "Summer Sale 2026",
        │   "businessId": "123",
        │   "startDate": "2026-06-01"
        │ }
        ↓

[1] CONTROLLER LAYER
    ├─ Extract HTTP headers
    ├─ Extract path parameters & request body
    ├─ Call Campaign*Api interface (Swagger-annotated)
    └─ Delegate to Service
        │
        ↓

[2] SERVICE LAYER (CampaignServiceImpl)
    ├─ Authorization check
    │  ├─ Verify user has permission
    │  └─ Check if user owns business
    │
    ├─ Validation
    │  ├─ NotNull checks
    │  ├─ Format validation
    │  └─ Business rule checks
    │
    ├─ If errors → Return 400 Bad Request
    │
    ├─ If valid:
    │  └─ Call Mapper.toEntity(DTO)
    │
    ↓

[3] MAPPER LAYER (MapStruct - CampaignMapper)
    ├─ Transform DTO fields to Entity fields
    ├─ Apply default values
    ├─ Convert nested objects if needed
    └─ Return CampaignEntity with all fields set
        │
        ↓

[4] REPOSITORY LAYER
    ├─ Call campaignRepository.save(entity)
    │
    ↓ (Hibernate/JPA)
    │
    ├─ Generate INSERT SQL
    │  INSERT INTO campaigns (name, business_id, start_date, created_at, updated_at, last_update)
    │  VALUES (?, ?, ?, NOW(), NOW(), NOW())
    │
    └─ Return saved entity with ID assigned
        │
        ↓

[5] DATABASE (PostgreSQL)
    ├─ Write to campaigns table
    ├─ Generate ID (if auto-increment)
    └─ Confirm write success
        │
        ↓

[6] NOTIFICATION LAYER (Async/Optional)
    ├─ Call Backbone client (Feign)
    │  └─ Notify: "Campaign created: ID=123"
    │
    └─ Publish to Kafka topic
       └─ Topic: campaigns.events
          Payload: CampaignCreatedEvent
        │
        ↓

[7] RESPONSE MAPPING
    ├─ Call Mapper.toDTO(savedEntity)
    │
    ├─ Transform Entity fields back to DTO
    │
    └─ Return CampaignTO object
        │
        ↓

[8] RESPONSE BUILD
    ├─ Set HTTP status: 201 Created
    ├─ Set Location header: /api/v1/campaigns/123
    ├─ Set X-Message-Header: "Campaign created successfully"
    ├─ Set response body: CampaignTO as JSON
    │
    └─ Build ResponseEntity
        │
        ↓

HTTP RESPONSE (201 Created)
    {
      "id": "123",
      "name": "Summer Sale 2026",
      "businessId": "456",
      "startDate": "2026-06-01T00:00:00Z",
      "createdAt": "2026-03-26T10:30:00Z",
      "updatedAt": "2026-03-26T10:30:00Z",
      "lastUpdate": "2026-03-26T10:30:00Z",
      "deleted": false
    }

    Headers:
    ├─ Location: /api/v1/campaigns/123
    ├─ X-Message-Header: Campaign created successfully
    └─ Content-Type: application/json
```

---

## 3. CRUD Operations - Detailed Flows

### Create (POST)

```
CLIENT REQUEST
    │
    ↓ POST /api/v1/campaigns { payload }
    │
┌───────────────────────────────────────────────────┐
│ 1. CONTROLLER                                     │
│    ├─ Receive @RequestBody CampaignTO           │
│    ├─ Extract @PathVariable, @RequestParam       │
│    └─ Call campaignService.create(campaignTO)   │
└────────────┬────────────────────────────────────┘
             ↓
┌───────────────────────────────────────────────────┐
│ 2. SERVICE - VALIDATION                          │
│    ├─ Check authorization                        │
│    ├─ Validate payload:                          │
│    │  ├─ name not null/empty                     │
│    │  ├─ businessId exists                       │
│    │  └─ dates valid (startDate < endDate)       │
│    └─ Halt if validation fails (400)            │
└────────────┬────────────────────────────────────┘
             ↓
┌───────────────────────────────────────────────────┐
│ 3. MAPPER                                         │
│    ├─ Create new CampaignEntity                  │
│    ├─ Set fields from DTO                        │
│    ├─ Set system fields:                         │
│    │  ├─ createdAt = NOW()                       │
│    │  ├─ updatedAt = NOW()                       │
│    │  ├─ lastUpdate = NOW()                      │
│    │  └─ deleted = false                         │
│    └─ Return entity                              │
└────────────┬────────────────────────────────────┘
             ↓
┌───────────────────────────────────────────────────┐
│ 4. REPOSITORY.SAVE()                             │
│    ├─ INSERT INTO campaigns (...)                │
│    ├─ Hibernate generates ID                     │
│    └─ Return entity with ID                      │
└────────────┬────────────────────────────────────┘
             ↓
┌───────────────────────────────────────────────────┐
│ 5. EXTERNAL NOTIFICATIONS (Async)                │
│    ├─ Backbone.notifyCreated(entity)             │
│    └─ Kafka.publish(CampaignCreatedEvent)        │
└────────────┬────────────────────────────────────┘
             ↓
┌───────────────────────────────────────────────────┐
│ 6. RESPONSE MAPPING                              │
│    ├─ Convert entity → DTO                       │
│    └─ Build ResponseEntity<CampaignTO>          │
└────────────┬────────────────────────────────────┘
             ↓
HTTP 201 CREATED + Location header + body
```

### Read (GET)

```
CLIENT REQUEST
    │
    ↓ GET /api/v1/campaigns/123
    │
┌───────────────────────────────────────────────────┐
│ 1. CONTROLLER                                     │
│    ├─ Extract @PathVariable("id") id             │
│    └─ Call campaignService.findById(id)         │
└────────────┬────────────────────────────────────┘
             ↓
┌───────────────────────────────────────────────────┐
│ 2. SERVICE                                        │
│    ├─ Call repository.findById(id)               │
│    └─ Halt if not found (404)                   │
└────────────┬────────────────────────────────────┘
             ↓
┌───────────────────────────────────────────────────┐
│ 3. REPOSITORY.FINDBYID()                         │
│    ├─ SELECT * FROM campaigns WHERE id = ?       │
│    └─ Return Optional<CampaignEntity>            │
└────────────┬────────────────────────────────────┘
             ↓
┌───────────────────────────────────────────────────┐
│ 4. RESPONSE MAPPING                              │
│    ├─ Convert entity → DTO                       │
│    └─ Build ResponseEntity<CampaignTO>          │
└────────────┬────────────────────────────────────┘
             ↓
HTTP 200 OK + body
```

### Update (PUT) - Optimistic Locking

```
CLIENT REQUEST
    │
    ↓ PUT /api/v1/campaigns/123
      {
        "name": "Updated Name",
        "lastUpdate": "2026-03-26T10:30:00Z"
      }
    │
┌───────────────────────────────────────────────────┐
│ 1. CONTROLLER                                     │
│    ├─ Extract ID from path                       │
│    ├─ Extract lastUpdate from payload            │
│    └─ Call campaignService.update(id, dto, lU)  │
└────────────┬────────────────────────────────────┘
             ↓
┌───────────────────────────────────────────────────┐
│ 2. SERVICE - OPTIMISTIC LOCK CHECK                │
│    ├─ Fetch current entity by ID                 │
│    ├─ Compare:                                    │
│    │  if (currentEntity.lastUpdate != lastUpdate)│
│    │      → Return 409 Conflict                  │
│    │  else                                        │
│    │      → Proceed                              │
│    └─ Validate new payload                       │
└────────────┬────────────────────────────────────┘
             ↓
┌───────────────────────────────────────────────────┐
│ 3. MAPPER - MERGE                                │
│    ├─ Copy updatable fields from DTO             │
│    ├─ Preserve system fields:                    │
│    │  ├─ createdAt (unchanged)                   │
│    │  ├─ id (unchanged)                          │
│    │  └─ deleted (unchanged unless explicit)     │
│    ├─ Update:                                    │
│    │  ├─ updatedAt = NOW()                       │
│    │  └─ lastUpdate = NOW()                      │
│    └─ Return merged entity                       │
└────────────┬────────────────────────────────────┘
             ↓
┌───────────────────────────────────────────────────┐
│ 4. REPOSITORY.SAVE()                             │
│    ├─ UPDATE campaigns SET ... WHERE id = ?      │
│    └─ Return updated entity                      │
└────────────┬────────────────────────────────────┘
             ↓
┌───────────────────────────────────────────────────┐
│ 5. EXTERNAL NOTIFICATIONS (Async)                │
│    ├─ Backbone.notifyUpdated(entity)             │
│    └─ Kafka.publish(CampaignUpdatedEvent)        │
└────────────┬────────────────────────────────────┘
             ↓
┌───────────────────────────────────────────────────┐
│ 6. RESPONSE MAPPING                              │
│    ├─ Convert entity → DTO                       │
│    └─ Build ResponseEntity<CampaignTO>          │
└────────────┬────────────────────────────────────┘
             ↓
HTTP 202 ACCEPTED + body
(Note: 202, not 200 - indicates async/deferred processing)
```

### Delete (DELETE)

```
CLIENT REQUEST
    │
    ↓ DELETE /api/v1/campaigns/123
    │
┌───────────────────────────────────────────────────┐
│ 1. CONTROLLER                                     │
│    ├─ Extract @PathVariable("id") id             │
│    └─ Call campaignService.delete(id)           │
└────────────┬────────────────────────────────────┘
             ↓
┌───────────────────────────────────────────────────┐
│ 2. SERVICE                                        │
│    ├─ Authorization check                        │
│    ├─ Fetch entity by ID                         │
│    └─ Halt if not found (404)                   │
└────────────┬────────────────────────────────────┘
             ↓
┌───────────────────────────────────────────────────┐
│ 3. SOFT DELETE LOGIC                             │
│    ├─ Set entity.deleted = true                  │
│    ├─ Update updatedAt = NOW()                   │
│    └─ Call repository.save(entity)               │
└────────────┬────────────────────────────────────┘
             ↓
┌───────────────────────────────────────────────────┐
│ 4. DATABASE                                       │
│    ├─ UPDATE campaigns SET deleted=true          │
│    │         WHERE id = ?                        │
│    └─ Entity remains in database                 │
└────────────┬────────────────────────────────────┘
             ↓
┌───────────────────────────────────────────────────┐
│ 5. EXTERNAL NOTIFICATIONS (Async)                │
│    ├─ Backbone.notifyDeleted(entity)             │
│    └─ Kafka.publish(CampaignDeletedEvent)        │
└────────────┬────────────────────────────────────┘
             ↓
HTTP 204 NO CONTENT (empty response body)
```

---

## 4. List/Search with Filtering

```
CLIENT REQUEST
    │
    ↓ GET /api/v1/campaigns?q=sale&businessId=456&page=0&size=20
    │
┌───────────────────────────────────────────────────────────────────┐
│ 1. CONTROLLER - EXTRACT PARAMETERS                                │
│    ├─ Query params: q, businessId, startDate, etc.               │
│    ├─ Pagination: page=0, size=20, sort=name,asc                 │
│    └─ Call campaignService.list(filters, pageable)               │
└────────────┬─────────────────────────────────────────────────────┘
             ↓
┌───────────────────────────────────────────────────────────────────┐
│ 2. SERVICE - BUILD FILTERS                                        │
│    ├─ CampaignFilterParser.parse(queryParams)                     │
│    │  ├─ Extract: q (free text)                                  │
│    │  ├─ Extract: businessId                                     │
│    │  ├─ Extract: startDate range                                │
│    │  └─ Create CampaignFilter object                            │
│    │                                                              │
│    └─ CampaignSpecifications.toSpecification(filter)              │
│       ├─ Build JPA Predicate tree                                │
│       ├─ WHERE name LIKE '%sale%'                                │
│       │  AND business_id = 456                                   │
│       │  AND start_date >= ?                                     │
│       │  AND deleted = false                                     │
│       └─ Return Specification<CampaignEntity>                    │
└────────────┬─────────────────────────────────────────────────────┘
             ↓
┌───────────────────────────────────────────────────────────────────┐
│ 3. REPOSITORY - PAGINATED QUERY                                   │
│    ├─ repository.findAll(specification, pageable)                 │
│    │                                                              │
│    │  SQL Generated:                                             │
│    │  SELECT c.* FROM campaigns c                                │
│    │  WHERE c.name LIKE '%sale%'                                 │
│    │    AND c.business_id = 456                                  │
│    │    AND c.deleted = false                                    │
│    │  ORDER BY c.name ASC                                        │
│    │  LIMIT 20 OFFSET 0                                          │
│    │                                                              │
│    └─ Return Page<CampaignEntity>                                │
│       ├─ content: [entity1, entity2, ...]                        │
│       ├─ totalElements: 42                                       │
│       ├─ totalPages: 3                                           │
│       └─ currentPage: 0                                          │
└────────────┬─────────────────────────────────────────────────────┘
             ↓
┌───────────────────────────────────────────────────────────────────┐
│ 4. MAPPER - BATCH CONVERT                                         │
│    ├─ For each entity in Page<CampaignEntity>                     │
│    │  └─ Convert to DTO                                          │
│    │                                                              │
│    └─ Return PageImpl<CampaignTO>                                 │
└────────────┬─────────────────────────────────────────────────────┘
             ↓
┌───────────────────────────────────────────────────────────────────┐
│ 5. RESPONSE BUILD                                                 │
│    ├─ Set HTTP 200 OK                                            │
│    ├─ Set response body:                                         │
│    │  {                                                          │
│    │    "content": [                                             │
│    │      { "id": "1", "name": "Spring Sale", ... },             │
│    │      { "id": "2", "name": "Summer Sale", ... }              │
│    │    ],                                                       │
│    │    "pageable": {                                            │
│    │      "pageNumber": 0,                                       │
│    │      "pageSize": 20,                                        │
│    │      "sort": ["name,asc"]                                   │
│    │    },                                                       │
│    │    "totalElements": 42,                                     │
│    │    "totalPages": 3,                                         │
│    │    "last": false                                            │
│    │  }                                                          │
│    │                                                              │
│    └─ Return ResponseEntity                                      │
└────────────┬─────────────────────────────────────────────────────┘
             ↓
HTTP 200 OK + paginated response
```

---

## 5. Authentication & Authorization Flow

```
CLIENT REQUEST
    │
    ↓ GET /api/v1/campaigns/123
  Headers: Authorization: Bearer <JWT_TOKEN>
    │
┌─────────────────────────────────────────────────────┐
│ 1. SPRING SECURITY FILTER CHAIN                     │
│    ├─ Extract Authorization header                │
│    ├─ Extract JWT token                           │
│    └─ Pass to JwtAuthenticationFilter              │
└────────┬────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────┐
│ 2. JWT VALIDATION (SessionJwtServiceImpl)            │
│    ├─ Parse token                                  │
│    ├─ Verify signature                             │
│    ├─ Check expiration                             │
│    ├─ Extract claims:                              │
│    │  ├─ user_id                                   │
│    │  ├─ username                                  │
│    │  ├─ roles                                     │
│    │  └─ permissions                               │
│    │                                               │
│    └─ If invalid → Return 401 Unauthorized        │
└────────┬────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────┐
│ 3. SET SECURITY CONTEXT                            │
│    ├─ Create UsernamePasswordAuthenticationToken   │
│    ├─ Set principal (user details)                 │
│    ├─ Set authorities (roles/permissions)          │
│    └─ Store in SecurityContextHolder               │
└────────┬────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────┐
│ 4. CONTROLLER EXECUTION                            │
│    ├─ Access SecurityContext if needed             │
│    ├─ Get current authenticated user               │
│    └─ Pass user context to service                 │
└────────┬────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────┐
│ 5. SERVICE-LEVEL AUTHORIZATION                     │
│    ├─ Check: Does user own this resource?          │
│    ├─ Check: Does user have required role?         │
│    ├─ Check: Is user allowed operation?            │
│    │                                               │
│    └─ If unauthorized → Return 403 Forbidden      │
└────────┬────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────┐
│ 6. BUSINESS LOGIC EXECUTION                        │
│    ├─ Safe to proceed (user authenticated)         │
│    ├─ Fetch and return resource                    │
│    └─ Return 200 OK                                │
└────────┬────────────────────────────────────────────┘
         ↓
HTTP 200 OK + resource
```

---

## 6. Kafka Async Notification Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                   SYNCHRONOUS OPERATION                          │
│              (Create/Update/Delete Campaign)                     │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ↓
        ┌────────────────────────────────────┐
        │ 1. Save to Database                │
        │    (Synchronous, waits for DB)     │
        └────────────────────────────────────┘
                         │
                         ↓
┌────────────────────────────────────────────────────────────────┐
│         2. ASYNC NOTIFICATION (Fire & Forget)                   │
│         ├─ Email notification needs to be sent                 │
│         ├─ External system needs to be notified                │
│         └─ Avoid blocking main request                         │
└────────────────────┬───────────────────────────────────────────┘
                     │
         ┌───────────┴───────────┐
         ↓                       ↓
    ┌─────────────┐        ┌──────────────┐
    │   KAFKA     │        │  Backbone    │
    │  PRODUCER   │        │  REST Client │
    └──────┬──────┘        └──────┬───────┘
           │                      │
           ↓                      ↓
    ┌─────────────────────┐  ┌──────────────────┐
    │ Kafka Broker        │  │ Backbone Service │
    │                     │  │                  │
    │ Topic:              │  │ Async HTTP Call  │
    │ campaigns.created   │  │ (non-blocking)   │
    │                     │  └──────────────────┘
    │ Message:            │
    │ {                   │
    │   "id": "123",      │
    │   "name": "Sale",   │
    │   "timestamp": ...  │
    │ }                   │
    └─────────┬───────────┘
              │
              ├────────────────────────────────────┐
              │                                    │
              ↓                                    ↓
        ┌──────────────────┐            ┌──────────────────┐
        │ Email Service    │            │ Other Consumers  │
        │ (Subscriber)     │            │ (if any)         │
        │                  │            │                  │
        │ Consumes message │            │ Consumes message │
        │ Sends email to   │            │ Performs action  │
        │ campaign owner   │            │                  │
        └──────────────────┘            └──────────────────┘

┌────────────────────────────────────────────────────────────────┐
│  MEANWHILE, BACK IN MAIN REQUEST...                            │
│  ├─ Response sent immediately (HTTP 201/202)                  │
│  ├─ User doesn't wait for email                               │
│  ├─ Email sent asynchronously in background                   │
│  └─ Request completes within milliseconds                     │
└────────────────────────────────────────────────────────────────┘
```

---

## 7. External Service Integration

```
┌────────────────────────────────────────────────────────────┐
│  DIRECTORY-BACKEND (Our Microservice)                      │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Service Layer                                        │  │
│  │ (CampaignServiceImpl, BusinessServiceImpl, etc.)      │  │
│  └────────────┬─────────────────────────────────────────┘  │
│               │ (when external sync needed)                 │
│        ┌──────┴──────┐                                      │
│        ↓             ↓                                      │
│  ┌──────────┐  ┌──────────┐                                │
│  │ Backbone │  │ Mercury  │                                │
│  │ Feign    │  │ Feign    │                                │
│  │ Client   │  │ Client   │                                │
│  └────┬─────┘  └────┬─────┘                                │
└───────┼─────────────┼──────────────────────────────────────┘
        │             │
        │ HTTP CALL   │ HTTP CALL
        ↓             ↓
  ┌──────────────┐  ┌──────────────────┐
  │ BACKBONE     │  │ MERCURY SERVICE  │
  │ SERVICE      │  │                  │
  │              │  │ - Notifications  │
  │ - Partner    │  │ - Communications │
  │   mgmt       │  │ - Messaging      │
  │ - Campaign   │  │                  │
  │   sync       │  │                  │
  └──────────────┘  └──────────────────┘

  ┌─ Sync Endpoints
  │  ├─ POST /sync/campaigns
  │  ├─ PUT /sync/campaigns/{id}
  │  └─ DELETE /sync/campaigns/{id}
  │
  └─ Response: ACK / Error

  Example Feign Client:
  ┌──────────────────────────────────────────┐
  │ BackboneClient.java                      │
  │                                          │
  │ @FeignClient(name="backbone",            │
  │   configuration=BackboneFeignConfigurer) │
  │ public interface BackboneClient {        │
  │                                          │
  │   @PostMapping("/sync/campaigns")        │
  │   ResponseEntity syncCampaign(           │
  │     @RequestBody CampaignDTO dto);       │
  │                                          │
  │ }                                        │
  └──────────────────────────────────────────┘
```

---

## 8. Data Flow for Common Scenarios

### Scenario 1: User Creates a Campaign

```
User clicks "Create Campaign" button in web app
    ↓
POST /api/v1/campaigns with campaign details
    ↓
┌─ Controller validates headers
├─ Service validates payload
├─ Service authorizes user (owns business)
├─ Mapper converts DTO → Entity
├─ Repository saves to PostgreSQL
├─ Async: Notify Backbone
├─ Async: Publish campaign.created to Kafka
├─ Kafka listeners pick up and send emails
├─ Mapper converts Entity → DTO
└─ Return 201 Created

User sees success message immediately
(Email sent in background)
```

### Scenario 2: User Searches & Filters Campaigns

```
User enters search term "sale" and selects business
    ↓
GET /api/v1/campaigns?q=sale&businessId=456
    ↓
┌─ Controller extracts filter parameters
├─ Service builds CampaignFilter
├─ Filter parser creates JPA Specification
├─ Repository queries with Specification + Pagination
├─ Database returns matching campaigns (with LIMIT/OFFSET)
├─ Mapper converts List<Entity> → List<DTO>
└─ Return 200 OK with paginated results

User sees first page of results (max 20 per page)
Can navigate to next pages
```

### Scenario 3: User Updates Campaign (Optimistic Lock)

```
User modifies campaign name and clicks save
    ↓
Frontend sends current version: lastUpdate = "2026-03-26T10:30:00Z"
    ↓
PUT /api/v1/campaigns/123 { name: "New Name", lastUpdate: "2026-03-26T10:30:00Z" }
    ↓
┌─ Controller extracts ID and lastUpdate
├─ Service fetches current campaign
├─ Service checks: DB lastUpdate == request lastUpdate?
│   └─ If NO → Return 409 Conflict (someone else updated)
├─ If YES → Merge fields
├─ Mapper updates Entity
├─ Repository saves with new lastUpdate
├─ Async: Notify Backbone & Kafka
└─ Return 202 Accepted

If conflict:
  User sees error message: "This campaign was updated by someone else"
  User must refresh and try again
```

---

## 9. Error Handling & Status Codes

```
┌─────┬────────────────────────────────────────────────┐
│Code │ Scenario                                       │
├─────┼────────────────────────────────────────────────┤
│ 200 │ GET request succeeded                          │
│ 201 │ POST (Create) succeeded                        │
│ 202 │ PUT/PATCH accepted (async processing)          │
│ 204 │ DELETE succeeded (no content)                  │
├─────┼────────────────────────────────────────────────┤
│ 400 │ Bad request (validation error)                │
│ 401 │ Unauthorized (invalid/missing JWT)            │
│ 403 │ Forbidden (user lacks permission)             │
│ 404 │ Not found (resource doesn't exist)            │
│ 409 │ Conflict (lastUpdate mismatch / optimistic    │
│     │ lock failure)                                  │
│ 422 │ Unprocessable entity (business rule violation)│
├─────┼────────────────────────────────────────────────┤
│ 500 │ Internal server error                          │
│ 502 │ Bad gateway (external service unavailable)    │
│ 503 │ Service unavailable (Vault/DB down)           │
└─────┴────────────────────────────────────────────────┘

Error Response Format:
{
  "timestamp": "2026-03-26T10:30:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Campaign lastUpdate does not match current version",
  "path": "/api/v1/campaigns/123"
}
```

---

**End of Architecture & Flowchart Diagrams**

For detailed code examples and implementation details, refer to PROJECT_DOCUMENTATION.md
