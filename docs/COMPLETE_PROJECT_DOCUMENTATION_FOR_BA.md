# Directory Backend - Complete Documentation for Business Analyst

**Version:** 0.0.1  
**Last Updated:** March 27, 2026  
**Audience:** Business Analysts, Product Owners, Project Managers, Stakeholders  
**Project:** directory-backend - Business Directory Microservice

---

## 📋 Table of Contents

1. [Executive Summary](#executive-summary)
2. [Business Overview](#business-overview)
3. [System Architecture](#system-architecture)
4. [Technical Stack](#technical-stack)
5. [Functional Domains](#functional-domains)
6. [Data Model & Relationships](#data-model--relationships)
7. [API Specifications](#api-specifications)
8. [Business Process Flows](#business-process-flows)
9. [Key Business Concepts](#key-business-concepts)
10. [Integration Points](#integration-points)
11. [Security & Authentication](#security--authentication)
12. [Quality & Testing](#quality--testing)
13. [Configuration & Deployment](#configuration--deployment)
14. [Common Scenarios & Use Cases](#common-scenarios--use-cases)
15. [Troubleshooting Guide](#troubleshooting-guide)
16. [Glossary](#glossary)
17. [Quick Reference](#quick-reference)

---

## Executive Summary

### What is Directory Backend?

**Directory Backend** is a Spring Boot microservice that provides REST APIs for managing a comprehensive business directory platform. It serves as the central backend system for handling business listings, marketing campaigns, product catalogs, user accounts, and user interactions (favorites).

### Key Capabilities

✅ **Campaign Management** - Marketing and promotional campaign lifecycle  
✅ **Business Directory** - Organization profiles and contact information  
✅ **Product Catalog** - Searchable inventory of products  
✅ **User Management** - Account creation, authentication, and profiles  
✅ **Favorites System** - User bookmarking of products  
✅ **Search & Filtering** - Advanced query capabilities with pagination  
✅ **External Integration** - Backbone and Mercury service synchronization  
✅ **Async Notifications** - Email and event messaging via Kafka  
✅ **API Documentation** - OpenAPI/Swagger specifications  

### Business Value

- **Centralized Directory** - Single source of truth for businesses, products, and campaigns
- **User Engagement** - Favorite products, personalized experience
- **Marketing Platform** - Campaign management with date-based targeting
- **Integration Hub** - Connects with internal services (Backbone, Mercury) and external systems
- **Scalable Architecture** - Cloud-native design supporting growth
- **High Quality** - 80%+ test coverage, automated quality gates

---

## Business Overview

### Business Context

The Directory Backend serves as the backbone for a business directory platform where:
- **Businesses** can list their company profiles and products
- **Campaigns** promote products or services for limited periods
- **Users** can browse, search, and favorite products
- **Platform administrators** manage the directory content

### Stakeholders

| Stakeholder | Role | Interests |
|------------|------|-----------|
| **End Users** | Consumers | Browse businesses, save favorites, receive notifications |
| **Business Owners** | Content creators | Manage their business profile, create campaigns, list products |
| **Platform Admins** | Operators | Moderate content, manage users, monitor system health |
| **Marketing Team** | Campaign creators | Design and launch promotional campaigns |
| **Integration Partners** | External systems | Backbone (business data), Mercury (notifications) |
| **Development Team** | Maintainers | Build features, fix bugs, ensure quality |

### Business Goals

1. **User Acquisition** - Attract businesses and consumers to the platform
2. **User Engagement** - Keep users active through campaigns and favorites
3. **Data Quality** - Maintain accurate, up-to-date business information
4. **Platform Reliability** - Ensure 99.9% uptime, fast response times
5. **Integration Success** - Seamless data flow with partner systems

---

## System Architecture

### High-Level Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                    CLIENT APPLICATIONS LAYER                    │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐   │
│  │  Web Portal  │  │  Mobile App  │  │  Third-party APIs  │   │
│  │  (React/Vue) │  │  (iOS/Andrd) │  │  (Partner Systems) │   │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬─────────┘   │
│         │                 │                      │             │
│         └─────────────────┴──────────────────────┘             │
└────────────────────────────┬───────────────────────────────────┘
                             │ HTTPS/REST
                             ↓
┌────────────────────────────────────────────────────────────────┐
│              DIRECTORY-BACKEND MICROSERVICE                     │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                     API LAYER                            │  │
│  │  ┌───────────┐ ┌───────────┐ ┌──────────┐ ┌──────────┐  │  │
│  │  │ Campaign  │ │ Business  │ │ Product  │ │   User   │  │  │
│  │  │Controller │ │Controller │ │Controller│ │Controller│  │  │
│  │  └─────┬─────┘ └─────┬─────┘ └────┬─────┘ └────┬─────┘  │  │
│  │        │             │            │            │         │  │
│  │        │    [Security Filter - JWT Validation]  │         │  │
│  │        └─────────────┼────────────┴────────────┘         │  │
│  └──────────────────────┼────────────────────────────────────┘  │
│                         ↓                                       │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                   SERVICE LAYER                          │  │
│  │  (Business Logic, Validation, Authorization)             │  │
│  │  ┌───────────────┐ ┌───────────────┐ ┌──────────────┐   │  │
│  │  │  Campaign     │ │  Business     │ │  Product     │   │  │
│  │  │  ServiceImpl  │ │  ServiceImpl  │ │  Service     │   │  │
│  │  └───────┬───────┘ └───────┬───────┘ └──────┬───────┘   │  │
│  │          │                 │                │           │  │
│  │  ┌───────────────┐ ┌───────────────┐ ┌──────────────┐   │  │
│  │  │  User         │ │  Auth         │ │  Favorite    │   │  │
│  │  │  Service      │ │  Service      │ │  Service     │   │  │
│  │  └───────┬───────┘ └───────┬───────┘ └──────┬───────┘   │  │
│  └──────────┼─────────────────┼────────────────┼───────────┘  │
│             ↓                 ↓                ↓              │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                 MAPPER LAYER (MapStruct)                 │  │
│  │          DTO ←→ Entity Transformation                    │  │
│  └──────────────────────────────────────────────────────────┘  │
│             ↓                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │           REPOSITORY LAYER (Spring Data JPA)             │  │
│  │  ┌───────────────┐ ┌───────────────┐ ┌──────────────┐   │  │
│  │  │  Campaign     │ │  Business     │ │  Product     │   │  │
│  │  │  Repository   │ │  Repository   │ │  Repository  │   │  │
│  │  └───────────────┘ └───────────────┘ └──────────────┘   │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────┬───────────────────────────────────────┘
                         ↓
        ┌────────────────┴────────────────┬──────────────────┐
        ↓                                 ↓                  ↓
┌───────────────┐              ┌──────────────────┐  ┌────────────────┐
│  PostgreSQL   │              │  Kafka Messaging │  │  External APIs │
│   Database    │              │                  │  │                │
│               │              │  • Email topics  │  │  • Backbone    │
│ • campaigns   │              │  • Event stream  │  │  • Mercury     │
│ • businesses  │              │  • Async tasks   │  │  • Config Srv  │
│ • products    │              │                  │  │  • Vault       │
│ • users       │              │                  │  │                │
│ • favorites   │              │                  │  │                │
└───────────────┘              └──────────────────┘  └────────────────┘
```

### Layered Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  PRESENTATION LAYER (Controllers + API Interfaces)          │
│  • Receive HTTP requests                                    │
│  • Extract headers, path parameters, query params           │
│  • OpenAPI/Swagger documentation                           │
│  • Delegate to service layer                               │
└───────────────────────────┬─────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  SERVICE LAYER (Business Logic)                             │
│  • Validation (NotNull, formats, business rules)            │
│  • Authorization (user permissions)                         │
│  • Orchestration (multi-step operations)                    │
│  • External notifications (Backbone, Mercury, Kafka)        │
│  • Build ResponseEntity with status + headers               │
└───────────────────────────┬─────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  MAPPER LAYER (MapStruct)                                   │
│  • Transform DTO (TO classes) ↔ Entity (JPA models)        │
│  • Apply default values                                     │
│  • Handle nested object mapping                            │
└───────────────────────────┬─────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  REPOSITORY LAYER (Spring Data JPA)                         │
│  • Interface-based repositories                             │
│  • JPA Specifications for dynamic queries                   │
│  • Pagination support                                       │
│  • Transaction management                                   │
└───────────────────────────┬─────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  PERSISTENCE LAYER (PostgreSQL)                             │
│  • Relational database                                      │
│  • Foreign key constraints                                  │
│  • Indexes for query performance                           │
└─────────────────────────────────────────────────────────────┘
```

---

## Technical Stack

### Core Technologies

| Category | Technology | Version | Purpose |
|----------|-----------|---------|---------|
| **Language** | Java | 21 | Programming language |
| **Framework** | Spring Boot | 3.5.8 | Application framework |
| **Cloud** | Spring Cloud | 2025.0.1 | Microservice patterns |
| **Build** | Maven | 3.8.0+ | Dependency & build management |

### Data & Persistence

| Technology | Version | Purpose |
|-----------|---------|---------|
| **Database** | PostgreSQL | 42.7.7 | Primary data store |
| **ORM** | Hibernate/JPA | (Spring default) | Object-relational mapping |
| **Repository** | Spring Data JPA | (Spring default) | Data access abstraction |
| **Migration** | Flyway | (detected) | Database versioning |

### API & Documentation

| Technology | Version | Purpose |
|-----------|---------|---------|
| **OpenAPI** | SpringDoc | 2.6.0 | API documentation & Swagger UI |
| **JSON** | Jackson | (Spring default) | JSON serialization |

### Testing & Quality

| Technology | Version | Purpose |
|-----------|---------|---------|
| **Test Framework** | JUnit 5 | 5.14.1 | Unit & integration testing |
| **Mocking** | Mockito | 5.21.0 | Test doubles |
| **Coverage** | JaCoCo | 0.8.14 | Code coverage (80% target) |
| **Static Analysis** | SonarCloud | Latest | Code quality metrics |
| **Code Inspection** | Qodana | Latest | JetBrains quality analysis |
| **PMD** | Maven PMD Plugin | 3.28.0 | Static code analysis |

### Mapping & Utilities

| Technology | Version | Purpose |
|-----------|---------|---------|
| **Mapping** | MapStruct | 1.6.3 | DTO ↔ Entity conversion |
| **JWT** | jjwt | 0.12.3 | Token-based authentication |
| **JSON Parsing** | Google Gson | 2.13.2 | Alternative JSON library |
| **Utilities** | Apache Commons Lang3 | 3.18.0 | Common utility functions |

### Infrastructure & Integration

| Technology | Version | Purpose |
|-----------|---------|---------|
| **Messaging** | Apache Kafka | (Spring Kafka) | Async event streaming |
| **Secrets** | HashiCorp Vault | (detected) | Secrets management |
| **Config** | Spring Cloud Config | 2025.0.1 | Centralized configuration |
| **Container** | Docker | amazoncorretto:21-alpine3.23 | Containerization |

### CI/CD Pipeline

| Workflow | Trigger | Purpose |
|---------|---------|---------|
| **ci.yml** | push/PR to main/master | Build, test, SonarCloud upload |
| **build.yml** | push/PR to main/development | Build & SonarQube analysis |
| **qodana_code_quality.yml** | push/PR, manual | Qodana static analysis |

---

## Functional Domains

### 1. Campaign Management 📢

**Business Purpose:** Manage marketing and promotional campaigns linked to businesses.

**Key Features:**
- Create time-bound campaigns (start/end dates)
- Link campaigns to businesses
- Categorize campaigns
- Search and filter campaigns
- Update campaign details
- Soft-delete campaigns (preserve history)

**Typical Use Cases:**
- "Summer Sale 2026" - retail promotion
- "New Product Launch" - product announcement
- "Holiday Special" - seasonal marketing
- "Limited Time Offer" - urgency-driven campaign

**Business Rules:**
- Campaign must have a name
- Campaign must be linked to a valid business
- Start date must be before end date
- Only business owner can modify their campaigns
- Deleted campaigns remain in database for audit

### 2. Business Directory 🏢

**Business Purpose:** Centralized registry of businesses/organizations.

**Key Features:**
- Business profile management
- Contact information
- Link to campaigns and products
- Search businesses by name/category
- Update business details
- Soft-delete businesses

**Typical Use Cases:**
- "Acme Corporation" - retail business
- "TechStart Inc." - technology company
- "Local Bakery" - small business
- "Enterprise Solutions LLC" - B2B service provider

**Business Rules:**
- Business must have a unique name
- Contact email must be valid format
- Business can have multiple campaigns and products
- Deleted businesses cascade to related entities

### 3. Product Catalog 📦

**Business Purpose:** Inventory of products/services offered by businesses.

**Key Features:**
- Product listing and details
- Link products to businesses
- Search and filter products
- Product categorization
- Price information
- Soft-delete products

**Typical Use Cases:**
- Physical goods (e.g., "Laptop Model X")
- Digital products (e.g., "Software License")
- Services (e.g., "Consulting Package")
- Subscriptions (e.g., "Monthly Plan")

**Business Rules:**
- Product must be linked to a business
- Product name is required
- Price is optional (some products may not have pricing)
- Products can be favorited by multiple users

### 4. User Management 👤

**Business Purpose:** Account and authentication system for platform users.

**Key Features:**
- User registration/signup
- User authentication (JWT-based)
- Profile management
- Password hashing (secure storage)
- Role-based access control
- User activity tracking

**User Types:**
- **Consumers** - Browse and favorite products
- **Business Owners** - Manage their business, campaigns, products
- **Administrators** - Platform moderation and management

**Business Rules:**
- Username must be unique
- Email must be valid and unique
- Passwords are hashed (never stored in plain text)
- Users can only modify their own data
- Account deletion is soft-delete (preserve history)

### 5. Favorites System ⭐

**Business Purpose:** User bookmarking/wishlist functionality.

**Key Features:**
- Add products to favorites
- View list of favorited products
- Remove products from favorites
- Soft-delete favorites (undo capability)

**Typical Use Cases:**
- "Save for later" - product wishlist
- "Compare items" - product comparison
- "Gift ideas" - shopping list
- "Follow products" - track price changes

**Business Rules:**
- User can favorite the same product only once
- Favorites are user-specific (private)
- Deleted products remain in favorites (show as unavailable)
- Favorites can be soft-deleted (removed but recoverable)

---

## Data Model & Relationships

### Entity-Relationship Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                        DATA MODEL                                 │
└──────────────────────────────────────────────────────────────────┘

┌────────────────────┐             ┌────────────────────┐
│    Business        │             │    Campaign        │
├────────────────────┤             ├────────────────────┤
│ id (PK)            │◄───────────┤│ id (PK)            │
│ name               │   1:N       │ name               │
│ description        │             │ business_id (FK)   │
│ contact_email      │             │ description        │
│ created_at         │             │ start_date         │
│ updated_at         │             │ end_date           │
│ deleted            │             │ category_id (FK)   │
│ last_update        │             │ created_at         │
└────────┬───────────┘             │ updated_at         │
         │                         │ deleted            │
         │ 1:N                     │ last_update        │
         │                         └────────────────────┘
         ↓
┌────────────────────┐             ┌────────────────────┐
│    Product         │             │    Category        │
├────────────────────┤             ├────────────────────┤
│ id (PK)            │             │ id (PK)            │
│ name               │             │ name               │
│ description        │             │ description        │
│ business_id (FK)   │─────────────│ parent_id (FK)     │
│ price              │             │ created_at         │
│ category_id (FK)   │             │ updated_at         │
│ created_at         │             └────────────────────┘
│ updated_at         │
│ deleted            │
│ last_update        │
└────────┬───────────┘
         │
         │ 1:N
         ↓
┌────────────────────┐             ┌────────────────────┐
│  UserFavorite      │◄───────────┤│      User          │
├────────────────────┤   1:N       ├────────────────────┤
│ id (PK)            │             │ id (PK)            │
│ user_id (FK)       │             │ username           │
│ product_id (FK)    │             │ email              │
│ created_at         │             │ password_hash      │
│ updated_at         │             │ first_name         │
│ deleted            │             │ last_name          │
│ last_update        │             │ roles              │
└────────────────────┘             │ created_at         │
                                   │ updated_at         │
                                   │ deleted            │
                                   │ last_update        │
                                   └────────┬───────────┘
                                            │
                                            │ 1:N
                                            ↓
                                   ┌────────────────────┐
                                   │ DigitalContact     │
                                   ├────────────────────┤
                                   │ id (PK)            │
                                   │ user_id (FK)       │
                                   │ contact_type (FK)  │
                                   │ value              │
                                   │ created_at         │
                                   │ updated_at         │
                                   └────────────────────┘
```

### Common Fields (All Entities)

Every entity in the system includes these standard fields:

| Field | Type | Purpose | Example |
|-------|------|---------|---------|
| `id` | UUID/Long | Primary key, unique identifier | `123e4567-e89b-12d3-a456-426614174000` |
| `created_at` | Timestamp | Record creation timestamp | `2026-03-26T10:30:00Z` |
| `updated_at` | Timestamp | Last modification timestamp | `2026-03-27T14:15:00Z` |
| `deleted` | Boolean | Soft-delete flag | `false` |
| `last_update` | Timestamp | Optimistic locking version | `2026-03-27T14:15:00Z` |

### Database Relationships

```
Business (1) ──→ (N) Campaign      One business has many campaigns
Business (1) ──→ (N) Product       One business has many products
Product  (1) ──→ (N) UserFavorite  One product can be favorited by many users
User     (1) ──→ (N) UserFavorite  One user can favorite many products
User     (1) ──→ (N) DigitalContact One user can have multiple contacts
Category (1) ──→ (N) Product       One category contains many products
Category (1) ──→ (N) Campaign      One category contains many campaigns
```

---

## API Specifications

### API Versioning

**Current Version:** `/api/v1/*`

**Versioning Strategy:** URI-based versioning  
**Backward Compatibility:** Maintained for all `/api/v1/*` endpoints unless explicitly communicated

### Campaign APIs

```
Base Path: /api/v1/campaigns
```

| Method | Endpoint | Purpose | Request Body | Response | Auth Required |
|--------|----------|---------|--------------|----------|---------------|
| GET | `/api/v1/campaigns` | List all campaigns (paginated) | - | 200 OK + Page<CampaignTO> | Yes |
| POST | `/api/v1/campaigns` | Create new campaign | CampaignTO | 201 Created + CampaignTO | Yes |
| GET | `/api/v1/campaigns/{id}` | Get specific campaign | - | 200 OK + CampaignTO | Yes |
| PUT | `/api/v1/campaigns/{id}` | Update campaign | CampaignTO | 202 Accepted + CampaignTO | Yes |
| DELETE | `/api/v1/campaigns/{id}` | Delete campaign | - | 204 No Content | Yes |

**Query Parameters (List):**
- `q` - Free-text search in name/description
- `businessId` - Filter by business (legacy: `business_id`)
- `categoryId` - Filter by category (legacy: `category_id`)
- `startDate_from` - Campaign start date range (from)
- `startDate_to` - Campaign start date range (to)
- `endDate_from` - Campaign end date range (from)
- `endDate_to` - Campaign end date range (to)
- `page` - Page number (0-based, default: 0)
- `size` - Page size (default: 20, max: 100)
- `sort` - Sort criteria (e.g., `name,asc`, `startDate,desc`)

### Business APIs

```
Base Path: /api/v1/businesses
```

| Method | Endpoint | Purpose | Request Body | Response | Auth Required |
|--------|----------|---------|--------------|----------|---------------|
| GET | `/api/v1/businesses` | List all businesses | - | 200 OK + Page<BusinessTO> | Yes |
| POST | `/api/v1/businesses` | Create new business | BusinessTO | 201 Created + BusinessTO | Yes |
| GET | `/api/v1/businesses/{id}` | Get specific business | - | 200 OK + BusinessTO | Yes |
| PUT | `/api/v1/businesses/{id}` | Update business | BusinessTO | 202 Accepted + BusinessTO | Yes |
| DELETE | `/api/v1/businesses/{id}` | Delete business | - | 204 No Content | Yes |

### Product APIs

```
Base Path: /api/v1/products
```

| Method | Endpoint | Purpose | Request Body | Response | Auth Required |
|--------|----------|---------|--------------|----------|---------------|
| GET | `/api/v1/products` | List all products | - | 200 OK + Page<ProductTO> | Yes |
| POST | `/api/v1/products` | Create new product | ProductTO | 201 Created + ProductTO | Yes |
| GET | `/api/v1/products/{id}` | Get specific product | - | 200 OK + ProductTO | Yes |
| PUT | `/api/v1/products/{id}` | Update product | ProductTO | 202 Accepted + ProductTO | Yes |
| DELETE | `/api/v1/products/{id}` | Delete product | - | 204 No Content | Yes |

### User APIs

```
Base Path: /api/v1/users
```

| Method | Endpoint | Purpose | Request Body | Response | Auth Required |
|--------|----------|---------|--------------|----------|---------------|
| POST | `/api/v1/users` | Register new user | UserTO | 201 Created + UserTO | No |
| GET | `/api/v1/users/{id}` | Get user profile | - | 200 OK + UserTO | Yes |
| PUT | `/api/v1/users/{id}` | Update user profile | UserTO | 202 Accepted + UserTO | Yes (own profile) |

### Favorites APIs

```
Base Path: /api/v1/favorites
```

| Method | Endpoint | Purpose | Request Body | Response | Auth Required |
|--------|----------|---------|--------------|----------|---------------|
| GET | `/api/v1/favorites` | Get user's favorites | - | 200 OK + List<FavoriteTO> | Yes |
| POST | `/api/v1/favorites` | Add to favorites | FavoriteTO | 201 Created + FavoriteTO | Yes |
| DELETE | `/api/v1/favorites/{id}` | Remove from favorites | - | 204 No Content | Yes |

### Authentication APIs

```
Base Path: /api/v1/auth
```

| Method | Endpoint | Purpose | Request Body | Response | Auth Required |
|--------|----------|---------|--------------|----------|---------------|
| POST | `/api/v1/auth/token` | Generate JWT token | LoginRequest | 200 OK + TokenResponse | No |
| POST | `/api/v1/auth/session-token` | Generate session token | SessionRequest | 200 OK + SessionResponse | No |

### Standard Response Formats

**Success Response (200 OK, 201 Created):**
```json
{
  "id": "campaign-123",
  "name": "Summer Sale 2026",
  "businessId": "business-456",
  "description": "Limited time offer",
  "startDate": "2026-06-01T00:00:00Z",
  "endDate": "2026-08-31T23:59:59Z",
  "createdAt": "2026-03-26T10:30:00Z",
  "updatedAt": "2026-03-26T10:30:00Z",
  "deleted": false,
  "lastUpdate": "2026-03-26T10:30:00Z"
}
```

**Paginated Response (List):**
```json
{
  "content": [
    { /* entity 1 */ },
    { /* entity 2 */ }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": ["name,asc"],
    "offset": 0
  },
  "totalElements": 150,
  "totalPages": 8,
  "last": false,
  "first": true,
  "numberOfElements": 20,
  "empty": false
}
```

**Error Response:**
```json
{
  "timestamp": "2026-03-27T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: name is required",
  "path": "/api/v1/campaigns"
}
```

### HTTP Status Codes

| Code | Status | Meaning | When Used |
|------|--------|---------|-----------|
| **200** | OK | Success | GET request succeeded |
| **201** | Created | Resource created | POST succeeded, new resource created |
| **202** | Accepted | Request accepted | PUT/PATCH accepted, async processing |
| **204** | No Content | Success, no body | DELETE succeeded |
| **400** | Bad Request | Invalid input | Validation failed, missing fields |
| **401** | Unauthorized | Not authenticated | Missing/invalid JWT token |
| **403** | Forbidden | Not authorized | User lacks permission |
| **404** | Not Found | Resource missing | Entity ID doesn't exist |
| **409** | Conflict | State conflict | Optimistic lock failed (stale update) |
| **500** | Internal Server Error | Unexpected error | Server-side failure |

### Response Headers

**Standard Headers:**
- `Content-Type: application/json`
- `X-Message-Header` - Custom message for client (per `DirectoryAppConstants.MESSAGE_HEADER`)
- `Location` - Resource URI (for 201 Created responses)

**Example:**
```
HTTP/1.1 201 Created
Content-Type: application/json
Location: /api/v1/campaigns/123
X-Message-Header: Campaign created successfully
```

---

## Business Process Flows

### 1. Create Campaign Flow

```
┌─────────────────────────────────────────────────────────────────┐
│              CREATE CAMPAIGN - BUSINESS FLOW                     │
└─────────────────────────────────────────────────────────────────┘

┌──────────────┐
│ Business     │ 
│ Owner        │ 
└──────┬───────┘
       │
       │ 1. Opens campaign form
       ↓
┌──────────────┐
│ Fill Form    │
│ • Name       │
│ • Business   │
│ • Start/End  │
│ • Description│
└──────┬───────┘
       │
       │ 2. Click "Create Campaign"
       ↓
┌──────────────────────────────────────────────────────┐
│ CLIENT VALIDATION                                     │
│ ✓ All required fields filled?                        │
│ ✓ Dates valid (start < end)?                         │
│ ✓ Business selected?                                 │
└──────────────┬───────────────────────────────────────┘
               │
               │ 3. POST /api/v1/campaigns
               ↓
┌──────────────────────────────────────────────────────┐
│ SERVER - CONTROLLER LAYER                            │
│ • Receive request                                    │
│ • Extract JWT token                                  │
│ • Extract request body                               │
└──────────────┬───────────────────────────────────────┘
               │
               ↓
┌──────────────────────────────────────────────────────┐
│ SERVER - AUTHENTICATION                              │
│ ✓ JWT token valid?                                   │
│ ✓ User has permission to create campaign?            │
└──────────────┬───────────────────────────────────────┘
               │
               ↓
┌──────────────────────────────────────────────────────┐
│ SERVER - SERVICE LAYER (Business Logic)              │
│ ✓ Business exists?                                   │
│ ✓ User owns the business?                            │
│ ✓ Name not empty?                                    │
│ ✓ Dates valid?                                       │
│ ✓ No duplicate campaign for same dates?              │
└──────────────┬───────────────────────────────────────┘
               │
               ↓ All validations pass
┌──────────────────────────────────────────────────────┐
│ MAPPER LAYER                                         │
│ • Transform CampaignTO → CampaignEntity              │
│ • Set system fields:                                 │
│   - id = generated                                   │
│   - createdAt = NOW()                                │
│   - updatedAt = NOW()                                │
│   - lastUpdate = NOW()                               │
│   - deleted = false                                  │
└──────────────┬───────────────────────────────────────┘
               │
               ↓
┌──────────────────────────────────────────────────────┐
│ REPOSITORY LAYER                                     │
│ • INSERT INTO campaigns (...)                        │
│ • PostgreSQL generates ID                            │
│ • Return saved entity                                │
└──────────────┬───────────────────────────────────────┘
               │
               ↓
┌──────────────────────────────────────────────────────┐
│ EXTERNAL NOTIFICATIONS (Async)                       │
│ • BackboneClient.notifyCreated(campaign)             │
│ • Kafka.publish(CampaignCreatedEvent)                │
│ • Email notification queued                          │
└──────────────┬───────────────────────────────────────┘
               │
               ↓
┌──────────────────────────────────────────────────────┐
│ RESPONSE MAPPING                                     │
│ • Transform entity → CampaignTO                      │
│ • Build ResponseEntity                               │
│   - Status: 201 Created                              │
│   - Header: Location: /api/v1/campaigns/123         │
│   - Header: X-Message-Header: "Campaign created"    │
│   - Body: CampaignTO                                 │
└──────────────┬───────────────────────────────────────┘
               │
               ↓
┌──────────────┐
│ HTTP 201     │
│ Created      │
│ + Campaign   │
│   details    │
└──────┬───────┘
       │
       ↓
┌──────────────┐
│ Client       │
│ Shows        │
│ Success      │
│ Message      │
└──────────────┘
```

### 2. Search/Filter Campaigns Flow

```
┌─────────────────────────────────────────────────────────────────┐
│          SEARCH & FILTER CAMPAIGNS - BUSINESS FLOW               │
└─────────────────────────────────────────────────────────────────┘

┌──────────────┐
│ User         │
└──────┬───────┘
       │
       │ 1. Navigate to Campaigns page
       ↓
┌──────────────────────────────┐
│ Apply Filters                │
│ • Search text: "sale"        │
│ • Business: "Acme Corp"      │
│ • Date range: June 2026      │
│ • Sort by: start date        │
└──────┬───────────────────────┘
       │
       │ 2. Click "Search"
       ↓
┌──────────────────────────────────────────────────────────┐
│ CLIENT - BUILD QUERY STRING                              │
│ GET /api/v1/campaigns?                                   │
│   q=sale&                                                │
│   businessId=123&                                        │
│   startDate_from=2026-06-01&                             │
│   startDate_to=2026-06-30&                               │
│   page=0&size=20&sort=startDate,asc                      │
└──────┬───────────────────────────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────────────────────────┐
│ SERVER - CONTROLLER                                      │
│ • Extract query parameters                               │
│ • Validate JWT token                                     │
│ • Parse pagination (page, size, sort)                    │
└──────┬───────────────────────────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────────────────────────┐
│ SERVICE - BUILD FILTER                                   │
│ • CampaignFilterParser.parse(params)                     │
│   → CampaignFilter object:                               │
│     - searchText = "sale"                                │
│     - businessId = 123                                   │
│     - startDateFrom = 2026-06-01                         │
│     - startDateTo = 2026-06-30                           │
└──────┬───────────────────────────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────────────────────────┐
│ SERVICE - BUILD SPECIFICATION                            │
│ • CampaignSpecifications.toSpec(filter)                  │
│   → JPA Specification (Predicate tree):                  │
│     WHERE (name LIKE '%sale%' OR description LIKE '%sale%')│
│       AND business_id = 123                              │
│       AND start_date >= '2026-06-01'                     │
│       AND start_date <= '2026-06-30'                     │
│       AND deleted = false                                │
└──────┬───────────────────────────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────────────────────────┐
│ REPOSITORY - EXECUTE QUERY                               │
│ • repository.findAll(spec, pageable)                     │
│                                                          │
│ SQL Generated:                                           │
│ SELECT * FROM campaigns c                                │
│ WHERE (c.name LIKE '%sale%' OR c.description LIKE '%sale%')│
│   AND c.business_id = 123                                │
│   AND c.start_date >= '2026-06-01'                       │
│   AND c.start_date <= '2026-06-30'                       │
│   AND c.deleted = false                                  │
│ ORDER BY c.start_date ASC                                │
│ LIMIT 20 OFFSET 0;                                       │
│                                                          │
│ Returns: Page<CampaignEntity>                            │
│   • content: [entity1, entity2, ...]                     │
│   • totalElements: 42                                    │
│   • totalPages: 3                                        │
│   • currentPage: 0                                       │
└──────┬───────────────────────────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────────────────────────┐
│ MAPPER - BATCH CONVERT                                   │
│ • For each entity in page:                               │
│   → Transform entity → CampaignTO                        │
│ • Return Page<CampaignTO>                                │
└──────┬───────────────────────────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────────────────────────┐
│ RESPONSE                                                 │
│ HTTP 200 OK                                              │
│ {                                                        │
│   "content": [                                           │
│     {                                                    │
│       "id": "1",                                         │
│       "name": "Summer Sale",                             │
│       "businessId": "123",                               │
│       "startDate": "2026-06-01"                          │
│     },                                                   │
│     { /* entity 2 */ }                                   │
│   ],                                                     │
│   "totalElements": 42,                                   │
│   "totalPages": 3,                                       │
│   "currentPage": 0,                                      │
│   "pageSize": 20                                         │
│ }                                                        │
└──────┬───────────────────────────────────────────────────┘
       │
       ↓
┌──────────────┐
│ Client       │
│ Displays     │
│ Results      │
│ (20 items)   │
│              │
│ [Pagination] │
│ « 1 2 3 »    │
└──────────────┘
```

### 3. Update Campaign with Optimistic Locking

```
┌─────────────────────────────────────────────────────────────────┐
│        UPDATE CAMPAIGN - OPTIMISTIC LOCKING FLOW                 │
└─────────────────────────────────────────────────────────────────┘

SCENARIO: Two users editing same campaign simultaneously

┌──────────────┐                           ┌──────────────┐
│ User A       │                           │ User B       │
└──────┬───────┘                           └──────┬───────┘
       │                                          │
       │ 10:30:00 GET /campaigns/123              │ 10:30:05 GET /campaigns/123
       ↓                                          ↓
┌──────────────┐                           ┌──────────────┐
│ Campaign:    │                           │ Campaign:    │
│ lastUpdate:  │                           │ lastUpdate:  │
│ 10:25:00     │                           │ 10:25:00     │
└──────┬───────┘                           └──────┬───────┘
       │                                          │
       │ User A edits name                        │ User B edits description
       ↓                                          ↓
┌──────────────┐                           ┌──────────────┐
│ New values:  │                           │ New values:  │
│ name: "New"  │                           │ desc: "..."  │
│ lastUpdate:  │                           │ lastUpdate:  │
│ 10:25:00     │                           │ 10:25:00     │
└──────┬───────┘                           └──────┬───────┘
       │                                          │
       │ 10:30:30 PUT /campaigns/123              │ 10:30:45 PUT /campaigns/123
       ↓                                          ↓
┌──────────────────────────────────────────────────────────┐
│ SERVER PROCESSING - USER A                               │
│                                                          │
│ 1. Fetch current campaign from DB                        │
│    → lastUpdate in DB: 10:25:00                          │
│                                                          │
│ 2. Compare with request lastUpdate                       │
│    DB: 10:25:00  ==  Request: 10:25:00  ✓ MATCH         │
│                                                          │
│ 3. Apply changes                                         │
│    • name = "New"                                        │
│    • updatedAt = 10:30:30                                │
│    • lastUpdate = 10:30:30 (NEW VERSION)                 │
│                                                          │
│ 4. Save to database                                      │
│    UPDATE campaigns                                      │
│    SET name='New', updated_at='10:30:30',                │
│        last_update='10:30:30'                            │
│    WHERE id=123                                          │
│                                                          │
│ 5. Notify external systems (Backbone, Kafka)            │
│                                                          │
│ 6. Return 202 Accepted                                   │
└──────┬───────────────────────────────────────────────────┘
       │
       ↓
┌──────────────┐
│ User A       │
│ SUCCESS ✓    │
│ Campaign     │
│ updated      │
└──────────────┘

                                          ┌──────────────────────────────────┐
                                          │ SERVER PROCESSING - USER B       │
                                          │                                  │
                                          │ 1. Fetch current campaign from DB│
                                          │    → lastUpdate in DB: 10:30:30  │
                                          │      (User A just updated it)    │
                                          │                                  │
                                          │ 2. Compare with request          │
                                          │    DB: 10:30:30  !=  Request: 10:25:00│
                                          │    ✗ MISMATCH                    │
                                          │                                  │
                                          │ 3. REJECT UPDATE                 │
                                          │                                  │
                                          │ 4. Return 409 Conflict           │
                                          │    {                             │
                                          │      "error": "Conflict",        │
                                          │      "message": "Resource has    │
                                          │       been modified by another   │
                                          │       user. Please refresh."     │
                                          │    }                             │
                                          └──────┬───────────────────────────┘
                                                 │
                                                 ↓
                                          ┌──────────────┐
                                          │ User B       │
                                          │ ERROR ✗      │
                                          │ "Conflict:   │
                                          │  Please      │
                                          │  refresh"    │
                                          └──────┬───────┘
                                                 │
                                                 │ User B clicks "Refresh"
                                                 ↓
                                          ┌──────────────┐
                                          │ GET fresh    │
                                          │ campaign     │
                                          │ lastUpdate:  │
                                          │ 10:30:30     │
                                          └──────┬───────┘
                                                 │
                                                 │ User B re-applies changes
                                                 ↓
                                          ┌──────────────┐
                                          │ PUT again    │
                                          │ with correct │
                                          │ lastUpdate   │
                                          │ SUCCESS ✓    │
                                          └──────────────┘

KEY TAKEAWAY:
✓ Optimistic locking prevents lost updates
✓ Last user to save wins
✓ First user must refresh and retry
✓ No data loss or corruption
```

### 4. User Favorite Product Flow

```
┌─────────────────────────────────────────────────────────────────┐
│              USER FAVORITES PRODUCT - BUSINESS FLOW              │
└─────────────────────────────────────────────────────────────────┘

┌──────────────┐
│ User         │
└──────┬───────┘
       │
       │ 1. Browse products
       ↓
┌──────────────────────────────┐
│ Product Listing Page         │
│ • Product A                  │
│ • Product B [♡ Favorite]     │
│ • Product C                  │
└──────┬───────────────────────┘
       │
       │ 2. Click "♡ Favorite" on Product B
       ↓
┌──────────────────────────────────────────────────────────┐
│ CLIENT VALIDATION                                        │
│ ✓ User logged in?                                        │
│ ✓ Product ID valid?                                      │
└──────┬───────────────────────────────────────────────────┘
       │
       │ 3. POST /api/v1/favorites
       │    { productId: "product-b", userId: "user-123" }
       ↓
┌──────────────────────────────────────────────────────────┐
│ SERVER - AUTHENTICATION                                  │
│ ✓ JWT token valid?                                       │
│ ✓ User ID matches token?                                 │
└──────┬───────────────────────────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────────────────────────┐
│ SERVER - SERVICE LAYER                                   │
│ ✓ Product exists?                                        │
│ ✓ Product not deleted?                                   │
│ ✓ User hasn't favorited this product already?            │
│   (Check for existing favorite)                          │
└──────┬───────────────────────────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────────────────────────┐
│ REPOSITORY - INSERT                                      │
│ INSERT INTO user_favorites (                             │
│   id, user_id, product_id,                               │
│   created_at, updated_at, deleted, last_update           │
│ ) VALUES (                                               │
│   'fav-456', 'user-123', 'product-b',                    │
│   NOW(), NOW(), false, NOW()                             │
│ )                                                        │
└──────┬───────────────────────────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────────────────────────┐
│ EXTERNAL NOTIFICATION (Optional)                         │
│ • Kafka.publish(UserFavoritedProductEvent)               │
│ • Analytics tracking                                     │
└──────┬───────────────────────────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────────────────────────┐
│ RESPONSE                                                 │
│ HTTP 201 Created                                         │
│ {                                                        │
│   "id": "fav-456",                                       │
│   "userId": "user-123",                                  │
│   "productId": "product-b",                              │
│   "createdAt": "2026-03-27T10:30:00Z"                    │
│ }                                                        │
└──────┬───────────────────────────────────────────────────┘
       │
       ↓
┌──────────────┐
│ Client       │
│ Updates UI   │
│ [♥ Favorited]│
│ (Filled      │
│  heart icon) │
└──────────────┘

LATER: User views their favorites
┌──────────────┐
│ User         │
└──────┬───────┘
       │
       │ GET /api/v1/favorites
       ↓
┌──────────────────────────────────────────────────────────┐
│ SERVER - QUERY FAVORITES                                 │
│ SELECT * FROM user_favorites                             │
│ WHERE user_id = 'user-123' AND deleted = false           │
│                                                          │
│ JOIN products p ON p.id = user_favorites.product_id      │
│ WHERE p.deleted = false                                  │
│                                                          │
│ Returns: List of favorited products with details         │
└──────┬───────────────────────────────────────────────────┘
       │
       ↓
┌──────────────┐
│ Client       │
│ Displays     │
│ Favorites    │
│ List         │
│ • Product B  │
│ • Product D  │
│ • Product F  │
└──────────────┘
```

---

## Key Business Concepts

### 1. Optimistic Locking 🔒

**Business Problem:**  
Two users editing the same record simultaneously can overwrite each other's changes, causing data loss.

**Example Scenario:**
```
10:30:00 - User A opens Campaign "Summer Sale" (lastUpdate: 10:25:00)
10:30:05 - User B opens same Campaign (lastUpdate: 10:25:00)
10:30:30 - User A changes name to "Summer Mega Sale" and saves
10:30:45 - User B changes description and tries to save
           ❌ User B's save is REJECTED (stale data)
```

**Technical Solution:**
- Every entity has a `lastUpdate` field (timestamp or version number)
- When updating, client sends the `lastUpdate` value it has
- Server compares: Does my DB copy match the client's copy?
  - **Match** ✓ → Update succeeds, generate new `lastUpdate`
  - **Mismatch** ✗ → Return 409 Conflict, user must refresh

**Business Benefit:**
- Prevents lost updates (no overwriting others' changes)
- Ensures data integrity
- User is notified and can merge changes consciously

**User Experience:**
1. User tries to save stale data
2. System responds: "This record was modified by another user. Please refresh."
3. User refreshes, sees latest data
4. User reapplies their changes (if still relevant)
5. Save succeeds

### 2. Soft Delete 🗑️

**Business Problem:**  
Hard-deleting records causes:
- Loss of historical data
- Broken referential integrity
- No audit trail
- Can't recover accidentally deleted data

**Technical Solution:**
- Add `deleted` boolean field to all entities (default: false)
- DELETE request sets `deleted = true` (doesn't remove row)
- All queries filter out deleted records: `WHERE deleted = false`
- Records remain in database permanently

**Business Benefit:**
- **Audit trail** - See what was deleted and when
- **Data recovery** - Can undelete if mistake
- **Referential integrity** - Foreign keys remain valid
- **Compliance** - Retain data for legal/regulatory requirements

**Example:**
```sql
-- Hard delete (don't do this)
DELETE FROM campaigns WHERE id = 123;  ❌

-- Soft delete (correct approach)
UPDATE campaigns SET deleted = true, updated_at = NOW()
WHERE id = 123;  ✓
```

### 3. Pagination 📄

**Business Problem:**  
Returning all records (thousands) in one response:
- Slow loading (network bandwidth)
- High memory usage (client & server)
- Poor user experience (wait time)
- Database strain

**Technical Solution:**
- Break results into "pages" of fixed size
- Client specifies: `page` number and `size` per page
- Server returns: requested page + pagination metadata

**Query Parameters:**
```
?page=0      First page (0-based index)
?size=20     20 items per page
?sort=name,asc  Sort by name ascending
```

**Response Structure:**
```json
{
  "content": [ /* 20 items */ ],
  "totalElements": 1500,    // Total matching records
  "totalPages": 75,         // 1500 / 20 = 75 pages
  "currentPage": 0,         // Current page number
  "pageSize": 20,           // Items per page
  "first": true,            // Is this the first page?
  "last": false             // Is this the last page?
}
```

**Business Benefit:**
- **Fast response** - Only 20 items returned, not 1500
- **Better UX** - Instant loading, paginated navigation
- **Scalability** - Handles millions of records gracefully

**Best Practices:**
- Default page size: 20
- Maximum page size: 100 (prevent abuse)
- Always provide pagination for list endpoints

### 4. Filtering & Search 🔍

**Business Problem:**  
Users need to find specific records among thousands without browsing all pages.

**Technical Solution:**

**A. Free-text Search**
```
?q=sale
```
Searches in multiple fields (name, description) using `LIKE '%sale%'`

**B. Structured Filters**
```
?businessId=123              Exact match on business
?categoryId=456              Exact match on category
?startDate_from=2026-01-01   Date range (from)
?startDate_to=2026-12-31     Date range (to)
```

**C. Combined Filters**
```
?q=sale&businessId=123&startDate_from=2026-06-01
```
All filters are AND-ed together: (search="sale") AND (business=123) AND (date >= 2026-06-01)

**D. Sorting**
```
?sort=startDate,desc    Sort by start date, newest first
?sort=name,asc          Sort by name, A-Z
```

**Implementation:**
- `CampaignFilterParser` - Parses query params into filter object
- `CampaignSpecifications` - Builds JPA Criteria API predicates
- Repository - Executes dynamic query with `WHERE` clause

**SQL Example:**
```sql
SELECT * FROM campaigns
WHERE (name LIKE '%sale%' OR description LIKE '%sale%')
  AND business_id = 123
  AND start_date >= '2026-06-01'
  AND deleted = false
ORDER BY start_date DESC
LIMIT 20 OFFSET 0;
```

**Business Benefit:**
- **Precision** - Find exactly what you need
- **Speed** - Database indexes make filtered queries fast
- **Flexibility** - Mix and match filters

### 5. Async Processing ⚡

**Business Problem:**  
Synchronous operations block user while waiting for:
- Email sending (slow)
- External API calls (network latency)
- Heavy computations (CPU intensive)

**Technical Solution:**
- Immediate response to user (201 Created / 202 Accepted)
- Background processing via Kafka message queue
- User doesn't wait for async tasks to complete

**Flow Example (Create Campaign):**
```
1. User submits campaign → POST /api/v1/campaigns
2. Server saves to database (fast)
3. Server returns 201 Created (instant)
4. User sees success message immediately
   
   [Meanwhile, in background:]
5. Kafka message published: CampaignCreatedEvent
6. Email service picks up message
7. Confirmation email sent to business owner
8. Backbone service notified
9. Mercury service updated
```

**Benefits:**
- **Fast response** - User gets immediate feedback
- **Reliability** - Kafka ensures messages aren't lost
- **Scalability** - Email service scales independently
- **Resilience** - If email fails, doesn't affect campaign creation

**HTTP Status Codes:**
- `202 Accepted` - Request accepted, processing continues async
- `201 Created` - Resource created immediately, notifications async

### 6. JWT Authentication 🔐

**Business Problem:**  
Need to authenticate users securely without storing sessions on server (stateless).

**Technical Solution: JSON Web Tokens (JWT)**

**How it Works:**
```
1. User logs in with username/password
   POST /api/v1/auth/token
   { username: "john_doe", password: "***" }

2. Server validates credentials
   ✓ User exists?
   ✓ Password matches hash?

3. Server generates JWT token
   {
     "uid": "user-123",
     "username": "john_doe",
     "roles": ["USER"],
     "exp": 1234567890  // Expiration timestamp
   }
   
   Signed with secret key → eyJhbGciOiJIUzI1NiJ9...

4. Server returns token
   { "token": "eyJhbGciOiJIUzI1NiJ9..." }

5. Client stores token (localStorage, cookie)

6. Every subsequent request includes token
   GET /api/v1/campaigns/123
   Headers: Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

7. Server validates token on each request
   ✓ Signature valid? (not tampered)
   ✓ Not expired?
   ✓ User still has permissions?

8. Request processed if valid
```

**Benefits:**
- **Stateless** - Server doesn't store sessions
- **Scalable** - Any server can validate token
- **Secure** - Cryptographically signed, can't be forged
- **Self-contained** - Token includes user info (no DB lookup)

**Security Notes:**
- Tokens expire (typically 1 hour)
- Refresh tokens for long-term sessions
- HTTPS required (prevent token theft)
- Passwords hashed with bcrypt (never plain text)

---

## Integration Points

### External Services

```
┌────────────────────────────────────────────────────────────┐
│          DIRECTORY-BACKEND INTEGRATION MAP                  │
└────────────────────────────────────────────────────────────┘

                   ┌──────────────────┐
                   │   DIRECTORY-     │
                   │   BACKEND        │
                   └────────┬─────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ↓                   ↓                   ↓
┌────────────────┐  ┌────────────────┐  ┌────────────────┐
│   Backbone     │  │    Mercury     │  │     Kafka      │
│   Service      │  │    Service     │  │   Messaging    │
├────────────────┤  ├────────────────┤  ├────────────────┤
│ • Business     │  │ • Email        │  │ • Async events │
│   data sync    │  │   notifications│  │ • Email queue  │
│ • Campaign     │  │ • SMS alerts   │  │ • Audit logs   │
│   notifications│  │ • Push notifs  │  │ • Integration  │
│ • External     │  │                │  │   events       │
│   system       │  │                │  │                │
│   integration  │  │                │  │                │
└────────────────┘  └────────────────┘  └────────────────┘

        ↓                   ↓                   ↓
┌────────────────┐  ┌────────────────┐  ┌────────────────┐
│ Feign Client   │  │ Feign Client   │  │ Kafka Producer │
├────────────────┤  ├────────────────┤  ├────────────────┤
│ Backbone       │  │ Mercury        │  │ Email          │
│ Client.java    │  │ Client.java    │  │ MessageProducer│
│                │  │                │  │ ServiceImpl    │
└────────────────┘  └────────────────┘  └────────────────┘

        ↓                   ↓
┌────────────────┐  ┌────────────────┐
│ Backbone       │  │ Mercury        │
│ Feign          │  │ Feign          │
│ Configurer     │  │ Configurer     │
└────────────────┘  └────────────────┘


        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ↓                   ↓                   ↓
┌────────────────┐  ┌────────────────┐  ┌────────────────┐
│  Spring Cloud  │  │   Vault        │  │  PostgreSQL    │
│  Config Server │  │   (Secrets)    │  │   Database     │
├────────────────┤  ├────────────────┤  ├────────────────┤
│ • Central      │  │ • API keys     │  │ • Primary      │
│   config       │  │ • DB passwords │  │   data store   │
│ • Environment  │  │ • Certificates │  │ • Persistence  │
│   specific     │  │ • Tokens       │  │                │
│   properties   │  │                │  │                │
└────────────────┘  └────────────────┘  └────────────────┘
```

### 1. Backbone Integration

**Purpose:** External business data synchronization and notifications

**Client:** `BackboneClient.java`  
**Configuration:** `BackboneFeignConfigurer.java`  
**Type:** HTTP REST API (via Spring Cloud OpenFeign)

**When Triggered:**
- Campaign created/updated/deleted
- Business created/updated/deleted
- Product created/updated/deleted

**Example Flow:**
```
1. User creates campaign in Directory Backend
2. Campaign saved to database
3. BackboneClient.notifyCreated(campaign) called (async)
4. HTTP POST to Backbone service
5. Backbone syncs data to external systems
```

**Configuration:**
```yaml
backbone:
  url: https://backbone.prx.com
  timeout: 5000ms
  retry: 3
```

### 2. Mercury Integration

**Purpose:** Communication and notification service

**Client:** `MercuryClient.java`  
**Configuration:** `MercuryFeignConfigurer.java`  
**Type:** HTTP REST API (via Spring Cloud OpenFeign)

**Notification Types:**
- Email notifications
- SMS alerts
- Push notifications
- In-app messages

**When Triggered:**
- User registration (welcome email)
- Campaign creation (confirmation email)
- Password reset
- Important system alerts

**Example Flow:**
```
1. User registers account
2. User saved to database
3. MercuryClient.sendEmail(emailRequest) called (async)
4. Email queued in Mercury
5. User receives welcome email
```

### 3. Kafka Messaging

**Purpose:** Asynchronous event streaming and message queue

**Producer:** `EmailMessageProducerServiceImpl.java`  
**Configuration:** `KafkaConfig.java`  
**Message Objects:** `com.prx.directory.kafka.to.*`

**Topics:**
- `campaigns.events` - Campaign lifecycle events
- `user.events` - User activity events
- `email.outbound` - Outgoing email messages
- `audit.log` - System audit trail

**Event Types:**
- `CampaignCreatedEvent`
- `CampaignUpdatedEvent`
- `CampaignDeletedEvent`
- `UserRegisteredEvent`
- `EmailNotificationEvent`

**Example Flow:**
```
1. Campaign updated
2. Service publishes CampaignUpdatedEvent to Kafka
3. Multiple consumers pick up event:
   - Email service sends notification
   - Analytics service logs activity
   - Audit service records change
```

### 4. Configuration Management

**Spring Cloud Config Server**

**Purpose:** Centralized configuration for all environments

**Bootstrap:** `src/main/resources/bootstrap.yml`

**Configuration Sources:**
- Git repository (version controlled)
- Vault (secrets)
- Local overrides (`application-local.yml`)

**Environment-Specific:**
```
config-server/
├── application.yml           # Default config
├── application-dev.yml       # Development
├── application-staging.yml   # Staging
├── application-prod.yml      # Production
└── directory-backend/
    ├── directory-backend.yml
    └── directory-backend-prod.yml
```

**Benefits:**
- Single source of truth
- Change config without rebuilding
- Environment-specific overrides
- Version control for config changes

### 5. Vault (Secrets Management)

**Purpose:** Secure storage of sensitive configuration

**Secrets Stored:**
- Database passwords
- API keys (Backbone, Mercury)
- JWT signing secrets
- Certificate passwords
- Kafka credentials

**Access Pattern:**
```
1. Application starts
2. Connects to Vault using AppRole
3. Retrieves secrets dynamically
4. Secrets injected into Spring context
5. No secrets in code or plain text config
```

**Configuration:**
```yaml
spring:
  cloud:
    vault:
      host: vault.prx.com
      port: 8200
      scheme: https
      authentication: APPROLE
      kv:
        enabled: true
        backend: secret
        profile-separator: '/'
        default-context: directory-backend
```

---

## Security & Authentication

### Authentication Flow

```
┌─────────────────────────────────────────────────────────────┐
│              AUTHENTICATION & AUTHORIZATION FLOW             │
└─────────────────────────────────────────────────────────────┘

┌──────────────┐
│ User         │
└──────┬───────┘
       │
       │ 1. Login with username/password
       ↓
┌──────────────────────────────────────────────────────────────┐
│ POST /api/v1/auth/token                                      │
│ {                                                            │
│   "username": "john_doe",                                    │
│   "password": "SecurePass123!"                               │
│ }                                                            │
└──────┬───────────────────────────────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────────────────────────────┐
│ AuthServiceImpl.authenticate()                               │
│ 1. Find user by username                                     │
│ 2. Verify password (bcrypt hash comparison)                  │
│ 3. Check account status (not deleted, not locked)            │
└──────┬───────────────────────────────────────────────────────┘
       │
       ↓ Valid credentials
┌──────────────────────────────────────────────────────────────┐
│ SessionJwtServiceImpl.generateToken()                        │
│ • Build JWT claims:                                          │
│   - uid: user ID                                             │
│   - username: john_doe                                       │
│   - roles: [USER, BUSINESS_OWNER]                            │
│   - iat: issued at timestamp                                 │
│   - exp: expiration timestamp (1 hour)                       │
│ • Sign with HMAC-SHA256 secret                               │
└──────┬───────────────────────────────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────────────────────────────┐
│ Response                                                     │
│ {                                                            │
│   "token": "eyJhbGciOiJIUzI1NiJ9.eyJ1aWQiOi...",             │
│   "type": "Bearer",                                          │
│   "expiresIn": 3600                                          │
│ }                                                            │
└──────┬───────────────────────────────────────────────────────┘
       │
       ↓
┌──────────────┐
│ Client       │
│ Stores token │
│ (localStorage│
│ or cookie)   │
└──────┬───────┘
       │
       │ 2. Every subsequent request
       ↓
┌──────────────────────────────────────────────────────────────┐
│ GET /api/v1/campaigns/123                                    │
│ Headers:                                                     │
│   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...               │
└──────┬───────────────────────────────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────────────────────────────┐
│ Spring Security Filter Chain                                 │
│ • JwtAuthenticationFilter extracts token                     │
│ • SessionJwtServiceImpl.validateToken()                      │
│   ✓ Signature valid?                                         │
│   ✓ Not expired?                                             │
│   ✓ Claims present?                                          │
└──────┬───────────────────────────────────────────────────────┘
       │
       ↓ Valid token
┌──────────────────────────────────────────────────────────────┐
│ Extract user from token                                      │
│ • User ID: extracted from "uid" claim                        │
│ • Roles: extracted from "roles" claim                        │
│ • Set SecurityContext with user details                      │
└──────┬───────────────────────────────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────────────────────────────┐
│ Controller - Authorization Check                             │
│ • @PreAuthorize("hasRole('USER')")                           │
│ • Check if user owns resource (business/campaign)            │
└──────┬───────────────────────────────────────────────────────┘
       │
       ↓ Authorized
┌──────────────────────────────────────────────────────────────┐
│ Service Layer - Business Logic                               │
│ Process request                                              │
└──────┬───────────────────────────────────────────────────────┘
       │
       ↓
┌──────────────┐
│ Response     │
│ 200 OK       │
│ + Data       │
└──────────────┘
```

### Role-Based Access Control (RBAC)

| Role | Permissions | Examples |
|------|-------------|----------|
| **GUEST** | Read-only public content | Browse campaigns, view products |
| **USER** | Create favorites, manage own profile | Add to favorites, update profile |
| **BUSINESS_OWNER** | Manage own business/campaigns/products | Create campaign, edit business |
| **ADMIN** | Full access, moderation | Delete any campaign, manage users |

### Security Features

✅ **Password Hashing** - bcrypt algorithm, salted  
✅ **JWT Tokens** - Signed with HMAC-SHA256  
✅ **HTTPS** - TLS/SSL encryption in transit  
✅ **CORS** - Cross-origin resource sharing configuration  
✅ **SQL Injection Prevention** - Parameterized queries (JPA)  
✅ **XSS Protection** - Output encoding  
✅ **CSRF Protection** - Token-based for state-changing operations  
✅ **Rate Limiting** - Prevent abuse (if configured)

---

## Quality & Testing

### Quality Gates

| Metric | Target | Tool | Enforcement |
|--------|--------|------|-------------|
| **Line Coverage** | ≥ 80% | JaCoCo | Build fails if below |
| **Branch Coverage** | ≥ 50% (package level) | JaCoCo | Build fails if below |
| **PMD Violations** | 0 (priority ≥ 5) | PMD | Build fails if violations |
| **Code Smells** | Grade A | SonarCloud | Advisory |
| **Duplicated Code** | < 3% | SonarCloud | Advisory |
| **Bugs** | 0 | SonarCloud | Advisory |
| **Vulnerabilities** | 0 | SonarCloud | Blocking |

### Testing Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                      TEST PYRAMID                            │
└─────────────────────────────────────────────────────────────┘

                           ┌──────┐
                           │ E2E  │  End-to-End Tests
                           │Tests │  (Manual/Automated UI)
                           └──┬───┘
                              │
                    ┌─────────┴─────────┐
                    │  Integration Tests │  API-level tests
                    │   (Controller +    │  with H2 database
                    │    Service +       │
                    │    Repository)     │
                    └─────────┬──────────┘
                              │
          ┌───────────────────┴───────────────────┐
          │        Unit Tests                     │  JUnit 5 + Mockito
          │  • Service layer tests                │  Mock dependencies
          │  • Mapper tests                       │  Fast execution
          │  • Util class tests                   │  High coverage
          │  • Validator tests                    │
          └───────────────────────────────────────┘

VOLUME:
• Unit Tests: ~80% of all tests (fast, many)
• Integration Tests: ~15% (slower, fewer)
• E2E Tests: ~5% (slowest, critical paths only)
```

### Test Configuration

**Test Profile:** `application-test.yml`

**Disabled in Tests:**
- Vault (mocked secrets)
- Config Server (local config)
- Kafka (embedded or mocked)
- Security (optional, can be enabled per test)

**Test Database:** H2 in-memory (PostgreSQL-compatible mode)

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn -Dtest=CampaignControllerTest test

# Run specific test method
mvn -Dtest=CampaignControllerTest#listCampaignsReturnsOk test

# Generate coverage report
mvn clean test jacoco:report
# Open: target/site/jacoco/index.html

# Run with coverage enforcement
mvn clean verify
```

### CI/CD Pipeline

```
┌─────────────────────────────────────────────────────────────┐
│                    CI/CD PIPELINE STAGES                     │
└─────────────────────────────────────────────────────────────┘

PR OPENED / PUSH TO MAIN
         │
         ↓
┌────────────────────┐
│ 1. BUILD           │
│ mvn clean package  │
│ (skip tests)       │
└────────┬───────────┘
         │
         ↓
┌────────────────────┐
│ 2. UNIT TESTS      │
│ mvn test           │
│ JUnit 5 execution  │
└────────┬───────────┘
         │
         ↓
┌────────────────────┐
│ 3. CODE COVERAGE   │
│ JaCoCo analysis    │
│ ✓ 80% line         │
│ ✓ 50% branch       │
└────────┬───────────┘
         │
         ↓
┌────────────────────┐
│ 4. PMD CHECK       │
│ Static analysis    │
│ ✓ No violations    │
└────────┬───────────┘
         │
         ↓
┌────────────────────┐
│ 5. SONARCLOUD      │
│ Upload coverage    │
│ Quality gate       │
└────────┬───────────┘
         │
         ↓
┌────────────────────┐
│ 6. QODANA          │
│ Code inspection    │
│ SARIF report       │
└────────┬───────────┘
         │
         ↓
┌────────────────────┐
│ 7. BUILD ARTIFACT  │
│ Create JAR file    │
│ directory-backend  │
│ .jar               │
└────────┬───────────┘
         │
         ↓
      SUCCESS ✓
```

---

## Configuration & Deployment

### Environment Variables

Key environment variables (from `default.env`):

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=directory_db
DB_USER=postgres
DB_PASSWORD=***

# Vault
VAULT_ENABLED=true
VAULT_HOST=vault.prx.com
VAULT_PORT=8200

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_GROUP_ID=directory-backend

# External Services
BACKBONE_URL=https://backbone.prx.com
MERCURY_URL=https://mercury.prx.com

# JWT
JWT_SECRET=***  # Stored in Vault
JWT_EXPIRATION=3600  # 1 hour

# Application
SPRING_PROFILES_ACTIVE=production
SERVER_PORT=8080
```

### Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   PRODUCTION DEPLOYMENT                      │
└─────────────────────────────────────────────────────────────┘

                     ┌────────────────┐
                     │  Load Balancer │
                     │  (NGINX/ALB)   │
                     └───────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ↓                    ↓                    ↓
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│  Instance 1   │    │  Instance 2   │    │  Instance 3   │
│  (Docker)     │    │  (Docker)     │    │  (Docker)     │
│  directory-   │    │  directory-   │    │  directory-   │
│  backend:1.0  │    │  backend:1.0  │    │  backend:1.0  │
└───────┬───────┘    └───────┬───────┘    └───────┬───────┘
        │                    │                    │
        └────────────────────┼────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ↓                    ↓                    ↓
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│  PostgreSQL   │    │     Kafka     │    │  Config Srv   │
│  (Primary +   │    │   (Cluster)   │    │  + Vault      │
│   Replica)    │    │               │    │               │
└───────────────┘    └───────────────┘    └───────────────┘
```

### Docker Deployment

**Dockerfile:**
```dockerfile
FROM amazoncorretto:21-alpine3.23

WORKDIR /app

COPY target/directory-backend.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Build & Run:**
```bash
# Build application
mvn -DskipTests package

# Build Docker image
docker build -t directory-backend:1.0 .

# Run container
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e DB_HOST=postgres \
  --env-file .env \
  directory-backend:1.0
```

---

## Common Scenarios & Use Cases

### Scenario 1: Business Launches New Campaign

**Actors:** Business Owner

**Steps:**
1. Business owner logs in
2. Navigate to "Campaigns" section
3. Click "Create New Campaign"
4. Fill form:
   - Name: "Summer Sale 2026"
   - Business: Select from dropdown
   - Start Date: 2026-06-01
   - End Date: 2026-08-31
   - Description: "Big discounts on all products"
5. Click "Create"
6. System validates and saves campaign
7. Confirmation email sent (async)
8. Campaign appears in listing
9. External systems notified (Backbone)

**API Call:**
```
POST /api/v1/campaigns
{
  "name": "Summer Sale 2026",
  "businessId": "biz-123",
  "startDate": "2026-06-01",
  "endDate": "2026-08-31",
  "description": "Big discounts on all products"
}
```

### Scenario 2: User Searches for Products

**Actors:** End User

**Steps:**
1. User navigates to product search page
2. Enter search criteria:
   - Keyword: "laptop"
   - Category: "Electronics"
   - Price range: $500-$1000
3. Click "Search"
4. System queries database with filters
5. Results displayed (20 per page)
6. User clicks "Next Page" for more results

**API Call:**
```
GET /api/v1/products?
  q=laptop&
  categoryId=electronics&
  priceMin=500&
  priceMax=1000&
  page=0&size=20&
  sort=price,asc
```

### Scenario 3: Concurrent Campaign Update (Optimistic Locking)

**Actors:** Marketing Manager A, Marketing Manager B

**Steps:**
1. Both managers open same campaign at 10:30 AM
2. Manager A changes campaign name (10:32 AM)
3. Manager A saves successfully (202 Accepted)
4. Manager B changes campaign description (10:35 AM)
5. Manager B tries to save
6. System detects conflict (lastUpdate mismatch)
7. System returns 409 Conflict
8. Manager B sees error: "Campaign was modified by another user"
9. Manager B clicks "Refresh"
10. Manager B sees latest changes (including Manager A's name change)
11. Manager B re-applies description change
12. Manager B saves successfully

**Technical Flow:**
```
Manager A: lastUpdate = 10:25:00 → Save → Success (new lastUpdate = 10:32:00)
Manager B: lastUpdate = 10:25:00 → Save → Conflict (DB has 10:32:00)
Manager B: Refresh → lastUpdate = 10:32:00 → Save → Success
```

### Scenario 4: User Favorites Product

**Actors:** End User

**Steps:**
1. User browses product catalog
2. Finds interesting product
3. Clicks "♡ Add to Favorites"
4. System saves favorite to database
5. Heart icon changes to "♥ Favorited" (filled)
6. Later, user navigates to "My Favorites"
7. System displays all favorited products
8. User can remove products from favorites

**API Calls:**
```
# Add to favorites
POST /api/v1/favorites
{ "productId": "product-456", "userId": "user-123" }

# View favorites
GET /api/v1/favorites

# Remove from favorites
DELETE /api/v1/favorites/fav-789
```

---

## Troubleshooting Guide

### Common Issues

| Issue | Symptoms | Cause | Solution |
|-------|----------|-------|----------|
| **401 Unauthorized** | "Unauthorized" error on API call | Missing or invalid JWT token | 1. Check if user logged in<br>2. Verify token not expired<br>3. Check Authorization header format |
| **403 Forbidden** | "Access denied" | User lacks permission | 1. Verify user role<br>2. Check resource ownership<br>3. Review authorization rules |
| **404 Not Found** | "Resource not found" | ID doesn't exist | 1. Verify ID is correct<br>2. Check if resource deleted<br>3. Confirm entity in database |
| **409 Conflict** | "Resource lastUpdate mismatch" | Optimistic lock failed | 1. Refresh page<br>2. Get latest data<br>3. Reapply changes<br>4. Save again |
| **400 Bad Request** | "Validation failed" | Invalid input | 1. Check required fields<br>2. Verify data formats<br>3. Review error message |
| **500 Internal Error** | "Server error" | Unexpected failure | 1. Check server logs<br>2. Contact support<br>3. Report bug with details |

### Debug Checklist

**For API Integration Issues:**
1. ✅ Is the request URL correct?
2. ✅ Are all required headers present (Authorization, Content-Type)?
3. ✅ Is the request body valid JSON?
4. ✅ Are all required fields included?
5. ✅ Is the JWT token valid and not expired?
6. ✅ Does the user have permission for this operation?
7. ✅ Check API documentation for correct schema

**For Optimistic Lock Conflicts:**
1. ✅ Always send current `lastUpdate` value
2. ✅ On 409 Conflict, fetch fresh data (GET request)
3. ✅ Merge changes if needed
4. ✅ Retry with new `lastUpdate`

**For Performance Issues:**
1. ✅ Always use pagination (don't fetch all records)
2. ✅ Use filters to narrow results
3. ✅ Check database indexes
4. ✅ Monitor server resources (CPU, memory)

---

## Glossary

| Term | Definition |
|------|------------|
| **API** | Application Programming Interface - contract for communication between systems |
| **DTO (TO)** | Data Transfer Object (Transfer Object) - data structure for API payloads |
| **Entity** | Database-mapped Java class (JPA model) |
| **Feign** | Declarative HTTP client library (Spring Cloud OpenFeign) |
| **JaCoCo** | Java Code Coverage tool |
| **JPA** | Java Persistence API - ORM standard |
| **JWT** | JSON Web Token - token-based authentication |
| **Kafka** | Distributed event streaming platform |
| **MapStruct** | Java bean mapping library (DTO ↔ Entity) |
| **Optimistic Locking** | Concurrency control using version field |
| **Pageable** | Spring Data abstraction for pagination |
| **PMD** | Static code analysis tool |
| **REST** | Representational State Transfer - HTTP API style |
| **Soft Delete** | Marking records as deleted without physical removal |
| **Specification** | JPA Criteria API wrapper for dynamic queries |
| **Spring Boot** | Java application framework |
| **Swagger/OpenAPI** | API documentation standard |
| **Vault** | HashiCorp secret management tool |

---

## Quick Reference

### Essential Commands

```bash
# Build (skip tests)
mvn -DskipTests package

# Full CI check
mvn -B -V -e clean verify

# Run tests
mvn test

# Coverage report
mvn clean test jacoco:report

# Run application
java -jar target/directory-backend.jar

# Run with profile
java -jar target/directory-backend.jar --spring.profiles.active=dev
```

### Key Files

| File | Purpose |
|------|---------|
| `pom.xml` | Maven configuration, dependencies |
| `bootstrap.yml` | Bootstrap configuration (Vault, Config Server) |
| `application.yml` | Application configuration |
| `default.env` | Environment variables template |
| `src/main/resources/api/index.yaml` | OpenAPI specification |
| `src/main/java/.../*Api.java` | API interface definitions |
| `src/main/java/.../*Controller.java` | Controller implementations |
| `src/main/java/.../*ServiceImpl.java` | Service layer business logic |
| `src/main/java/.../*Repository.java` | Data access layer |

### Important URLs

| Environment | URL | Purpose |
|------------|-----|---------|
| **Local** | http://localhost:8080 | Local development |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | API documentation |
| **Actuator** | http://localhost:8080/actuator | Health checks, metrics |
| **SonarCloud** | https://sonarcloud.io/dashboard?id=umdc-directory-backend | Code quality metrics |
| **GitHub** | https://github.com/um-dev-creative/directory-backend | Source code repository |

### HTTP Status Code Quick Reference

| Code | Meaning | Action |
|------|---------|--------|
| 200 | OK | Success |
| 201 | Created | New resource created |
| 202 | Accepted | Async processing |
| 204 | No Content | Delete success |
| 400 | Bad Request | Fix input |
| 401 | Unauthorized | Login required |
| 403 | Forbidden | Permission denied |
| 404 | Not Found | Check ID |
| 409 | Conflict | Refresh & retry |
| 500 | Server Error | Contact support |

---

## Contact & Support

**Development Team:** um-dev-creative  
**Project Manager:** [Contact via YouTrack]  
**Documentation:** `docs/` folder  
**Issue Tracking:** GitHub Issues + YouTrack  

**For Questions:**
- Technical: Check `CLAUDE.md` and `PROJECT_DOCUMENTATION.md`
- Business: Check `BA_QUICK_REFERENCE.md` and `BA_VISUAL_SUMMARY.md`
- Architecture: Check `ARCHITECTURE_DIAGRAMS.md`

---

**Document Version:** 1.0  
**Last Updated:** March 27, 2026  
**Generated For:** Business Analyst Team  
**Status:** ✅ Complete


