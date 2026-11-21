# Directory Backend - BA Quick Reference Guide

A condensed guide for Business Analysts covering key concepts, entities, and operations.

---

## One-Minute Overview

**Directory Backend** is a REST API service that manages:
- 📢 **Campaigns** - Marketing promotions linked to businesses
- 🏢 **Businesses** - Company profiles and organizations
- 📦 **Products** - Catalog items linked to businesses
- 👤 **Users** - User accounts with authentication
- ⭐ **Favorites** - User bookmarks of products

**Tech Stack:** Java 21 + Spring Boot 3.5.8 + PostgreSQL + Kafka

**Architecture:** Controller → Service → Repository (3-layer)

---

## Key Entities & Fields

### Campaign 📢
```
id (UUID)              - Unique identifier
name (String)          - Campaign name
businessId (FK)        - Parent business
description (Text)     - Campaign details
startDate (Date)       - Campaign start
endDate (Date)         - Campaign end
categoryId (FK)        - Product category
lastUpdate (Timestamp) - For optimistic locking
deleted (Boolean)      - Soft-delete flag
createdAt (Timestamp)  - Created date
updatedAt (Timestamp)  - Last modified date
```

### Business 🏢
```
id (UUID)              - Unique identifier
name (String)          - Business name
description (Text)     - Business description
contactEmail (Email)   - Primary contact
lastUpdate (Timestamp) - For optimistic locking
deleted (Boolean)      - Soft-delete flag
createdAt, updatedAt   - Timestamps
```

### Product 📦
```
id (UUID)
name (String)
description (Text)
businessId (FK)        - Owner business
price (Decimal)        - Product price (if applicable)
lastUpdate (Timestamp)
deleted (Boolean)
createdAt, updatedAt
```

### User 👤
```
id (UUID)
username (String)      - Unique login
email (Email)          - Contact email
passwordHash (String)  - Hashed password (never plain text)
roles (List)           - User roles
lastUpdate (Timestamp)
deleted (Boolean)
createdAt, updatedAt
```

### User Favorite ⭐
```
id (UUID)
userId (FK)            - User who favorited
productId (FK)         - Favorited product
lastUpdate (Timestamp)
deleted (Boolean)      - Soft-delete (can remove favorite)
createdAt, updatedAt
```

---

## API Endpoints Summary

### Campaigns
```
GET    /api/v1/campaigns                    List all campaigns (paginated, filterable)
POST   /api/v1/campaigns                    Create campaign
GET    /api/v1/campaigns/{id}               Get specific campaign
PUT    /api/v1/campaigns/{id}               Update campaign (202 Accepted)
DELETE /api/v1/campaigns/{id}               Delete campaign (soft-delete)

Search Parameters:
  ?q=keyword                     Free-text search in name/description
  ?businessId=123                Filter by business
  ?categoryId=456                Filter by category
  ?startDate_from=2026-01-01     Start date range
  ?startDate_to=2026-12-31
  ?page=0&size=20&sort=name,asc  Pagination & sorting
```

### Businesses
```
GET    /api/v1/businesses                   List all
POST   /api/v1/businesses                   Create
GET    /api/v1/businesses/{id}              Get
PUT    /api/v1/businesses/{id}              Update
DELETE /api/v1/businesses/{id}              Delete
```

### Products
```
GET    /api/v1/products                     List all
POST   /api/v1/products                     Create
GET    /api/v1/products/{id}                Get
PUT    /api/v1/products/{id}                Update
DELETE /api/v1/products/{id}                Delete
```

### Users
```
POST   /api/v1/users                        Register/Create user
GET    /api/v1/users/{id}                   Get user profile
PUT    /api/v1/users/{id}                   Update profile
```

### Favorites
```
GET    /api/v1/favorites                    Get user's favorites
POST   /api/v1/favorites                    Add to favorites
DELETE /api/v1/favorites/{id}               Remove from favorites
```

---

## HTTP Status Codes Explained

| Code | Meaning | When | Example |
|------|---------|------|---------|
| **200** | OK | Successful GET request | Campaign retrieved |
| **201** | Created | New resource created | Campaign created successfully |
| **202** | Accepted | Change accepted, processing async | Campaign update queued |
| **204** | No Content | Success, no data back | Campaign deleted |
| **400** | Bad Request | Invalid input | Missing required field |
| **401** | Unauthorized | No valid JWT token | Need to login |
| **403** | Forbidden | User lacks permission | Can't edit others' campaigns |
| **404** | Not Found | Resource doesn't exist | Campaign ID not found |
| **409** | Conflict | Optimistic lock failed | Stale update attempt |
| **500** | Server Error | Unexpected error | Internal failure |

---

## Important Concepts

### 1. **Optimistic Locking (lastUpdate)**

**Purpose:** Prevent conflicting updates when multiple users edit simultaneously.

**How it works:**
- Every entity has a `lastUpdate` field (timestamp)
- When updating, send the current `lastUpdate` value
- Server checks: Does my DB copy match?
  - ✅ **Match** → Update succeeds, new `lastUpdate` generated
  - ❌ **Mismatch** → Return 409 Conflict (someone else updated)

**Client Example:**
```
Current campaign: lastUpdate = "2026-03-26T10:30:00Z"
User edits name → POST {name: "New Name", lastUpdate: "2026-03-26T10:30:00Z"}

Server response:
- If DB lastUpdate == sent lastUpdate → SUCCESS, return 202 Accepted
- If DB lastUpdate != sent lastUpdate → CONFLICT, return 409
```

### 2. **Soft Delete**

**Purpose:** Keep historical data, mark as deleted rather than erase.

**How it works:**
- Every entity has a `deleted` boolean field (default: false)
- DELETE request → Sets `deleted = true` in database
- Entity remains in database (not removed)
- Future queries automatically exclude deleted records
- Can be recovered if needed

**Advantage:** Audit trail, referential integrity, data recovery

### 3. **Pagination**

**Purpose:** Avoid returning thousands of records at once.

**Query Parameters:**
```
page=0      Starts at page 0 (first page)
size=20     Return 20 items per page
sort=name,asc  Sort by name, ascending
```

**Response:**
```json
{
  "content": [ /* array of entities */ ],
  "totalElements": 1500,
  "totalPages": 75,
  "currentPage": 0,
  "pageSize": 20,
  "last": false
}
```

### 4. **Filtering & Search**

**Free-text search:**
```
?q=sale     Searches name and description for "sale"
```

**Structured filters:**
```
?businessId=123              Only campaigns from business 123
?categoryId=456              Only products in category 456
?startDate_from=2026-01-01   Date range filters
?startDate_to=2026-12-31
```

### 5. **Async Notifications**

**Purpose:** Don't block the user's request while sending emails/notifications.

**Flow:**
1. User creates campaign → Database updated
2. Immediately return `201 Created` to user
3. In background, send email/notification via Kafka
4. User doesn't wait for email

**Benefit:** Faster response times, reliable async processing

---

## Common Workflows

### Creating a New Campaign

```
1. Client sends:
   POST /api/v1/campaigns
   {
     "name": "Summer Sale 2026",
     "businessId": "biz-123",
     "startDate": "2026-06-01",
     "endDate": "2026-08-31",
     "description": "Big summer discount"
   }

2. Server validates:
   ✓ All required fields present
   ✓ User owns the business
   ✓ Start date < end date
   ✓ Valid business ID exists

3. Server creates:
   - Campaign entity in database
   - Sets ID, timestamps, lastUpdate
   - Sends soft-copy to Backbone service
   - Publishes event to Kafka (async email)

4. Returns 201 Created:
   {
     "id": "camp-456",
     "name": "Summer Sale 2026",
     "businessId": "biz-123",
     "createdAt": "2026-03-26T10:30:00Z",
     "lastUpdate": "2026-03-26T10:30:00Z"
   }

5. In background:
   - Confirmation email sent
   - Backbone notified
   - Other services updated
```

### Searching for Campaigns

```
1. User enters:
   - Search text: "sale"
   - Business filter: "PRX Corporation"
   - Date range: June 2026

2. Generated URL:
   GET /api/v1/campaigns?q=sale&businessId=123&startDate_from=2026-06-01&page=0&size=20

3. Server:
   - Parses filters
   - Builds SQL WHERE clause
   - Executes paginated query
   - Returns first 20 matching campaigns

4. Response:
   {
     "content": [
       { "id": "1", "name": "Summer Sale", ... },
       { "id": "2", "name": "Flash Sale", ... },
       ...
     ],
     "totalElements": 42,
     "totalPages": 3,
     "currentPage": 0
   }
```

### Updating a Campaign (Handle Conflicts)

```
Scenario: Two users edit same campaign simultaneously

User A (opens campaign at 10:30):
  lastUpdate: "2026-03-26T10:30:00Z"
  Changes: name only

User B (opens campaign at 10:30):
  lastUpdate: "2026-03-26T10:30:00Z"
  Changes: dates

Timeline:
  10:30:30 → User B saves first
            Server updates lastUpdate to "2026-03-26T10:30:30Z"

  10:30:45 → User A tries to save
            Sends: lastUpdate: "2026-03-26T10:30:00Z" (stale!)
            Server checks: DB has "2026-03-26T10:30:30Z"
            MISMATCH → Return 409 Conflict

  User A sees: "This campaign was updated by someone else"
  User A clicks: Refresh
  User A sees: Updated values from User B
  User A can: Re-apply their changes and save again
```

---

## Performance Considerations

### Response Times
- **Simple GET:** 50-100ms
- **Create/Update:** 100-200ms (includes DB write)
- **Search with filters:** 200-500ms (depends on result count)
- **Async operations:** Return immediately (background processing)

### Pagination Best Practices
✓ Always use pagination (never fetch all records)
✓ Default page size: 20 items
✓ Maximum recommended: 100 items per page
✓ For very large datasets: Add more filters

### Search Performance
✓ Free-text search (`q=...`) is slower than structured filters
✓ Combine filters when possible
✓ Use date ranges to narrow results
✓ Indexes on `businessId`, `categoryId`, `createdAt` help

---

## Troubleshooting Guide

### ❌ "401 Unauthorized"
**Cause:** Missing or invalid JWT token
**Fix:** User needs to login first → Get fresh token → Include in requests

### ❌ "403 Forbidden"
**Cause:** User lacks permission (e.g., editing another's campaign)
**Fix:** Check user roles/permissions; ensure user owns resource

### ❌ "404 Not Found"
**Cause:** Resource doesn't exist
**Fix:** Verify ID is correct; check if resource was deleted

### ❌ "409 Conflict"
**Cause:** Optimistic lock mismatch (stale update)
**Fix:** Refresh, fetch latest `lastUpdate` value, try again

### ❌ "400 Bad Request"
**Cause:** Invalid input
**Fix:** Check error message; verify all required fields present

### ❌ "500 Internal Server Error"
**Cause:** Unexpected server error
**Fix:** Contact support; check server logs

---

## Integration Points

### External Services

| Service | Purpose | Type | When Used |
|---------|---------|------|-----------|
| **Backbone** | Partner/campaign sync | HTTP/Feign | When campaign created/updated |
| **Mercury** | Notifications | HTTP/Feign | For alerts and updates |
| **Kafka** | Email messaging | Async queue | Background email sends |
| **PostgreSQL** | Data storage | Database | Every read/write |
| **Vault** | Secrets | Config | DB credentials, API keys |

---

## Environment Variables

```bash
# Database Connection
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/directory
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# Authentication
AUTH_JWT_SECRET=<secret-key>
AUTH_JWT_EXPIRATION=3600000  # 1 hour in milliseconds

# External Services
BACKBONE_SERVICE_URL=https://backbone.internal
MERCURY_SERVICE_URL=https://mercury.internal

# Kafka
SPRING_KAFKA_BROKERS=localhost:9092

# Server
SERVER_PORT=8080
SERVER_SERVLET_CONTEXT_PATH=/
```

---

## Database Schema Overview

```sql
-- Main Tables
CREATE TABLE campaigns (
  id UUID PRIMARY KEY,
  business_id UUID,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  start_date TIMESTAMP,
  end_date TIMESTAMP,
  category_id UUID,
  deleted BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  last_update TIMESTAMP DEFAULT NOW(),
  FOREIGN KEY (business_id) REFERENCES businesses(id)
);

CREATE TABLE businesses (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  deleted BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  last_update TIMESTAMP DEFAULT NOW()
);

CREATE TABLE products (
  id UUID PRIMARY KEY,
  business_id UUID NOT NULL,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  deleted BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  last_update TIMESTAMP DEFAULT NOW(),
  FOREIGN KEY (business_id) REFERENCES businesses(id)
);

CREATE TABLE users (
  id UUID PRIMARY KEY,
  username VARCHAR(255) UNIQUE NOT NULL,
  email VARCHAR(255) UNIQUE,
  password_hash VARCHAR(255),
  deleted BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  last_update TIMESTAMP DEFAULT NOW()
);

CREATE TABLE user_favorites (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  product_id UUID NOT NULL,
  deleted BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  last_update TIMESTAMP DEFAULT NOW(),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Indexes for performance
CREATE INDEX idx_campaigns_business_id ON campaigns(business_id);
CREATE INDEX idx_campaigns_created_at ON campaigns(created_at);
CREATE INDEX idx_products_business_id ON products(business_id);
CREATE INDEX idx_user_favorites_user_id ON user_favorites(user_id);
```

---

## Testing & Quality Gates

### Code Coverage
- **Target:** 80% line coverage (JaCoCo)
- **Enforcement:** Maven build fails if below 80%
- **Tools:** JUnit 5 + Mockito

### Quality Tools
- **SonarCloud** - Cloud-based code analysis
- **Qodana** - JetBrains IntelliJ inspection
- **PMD** - Code smell detection

### Build Commands
```bash
# Full build with all tests & quality checks
mvn -B -V -e clean verify

# Generate coverage report
mvn clean test jacoco:report
# Open: target/site/jacoco/index.html
```

---

## Support & Documentation

**Documentation:**
- Full technical docs: `PROJECT_DOCUMENTATION.md`
- Architecture diagrams: `ARCHITECTURE_DIAGRAMS.md`
- Development guide: `CLAUDE.md`
- Knowledge base: https://prx.myjetbrains.com/youtrack/articles/DS-A-8/Directory-Backend

**Issues & Support:**
- GitHub Issues: um-dev-creative/directory-backend
- Team: um-dev-creative
- Primary Language: English

---

**Quick Links:**
- Repository: GitHub um-dev-creative/directory-backend
- CI/CD: GitHub Actions workflows in `.github/workflows/`
- Code Quality: SonarCloud dashboard
- Slack/Teams: Tag @um-dev-creative for urgent issues

---

**Last Updated:** March 2026
**Version:** 0.0.1
