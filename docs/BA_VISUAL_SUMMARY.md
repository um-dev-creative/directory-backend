# Directory Backend - BA Visual Summary & Cheat Sheet

**One-page reference for Business Analysts**

---

## 📊 System at a Glance

```
                        CLIENT (Web/Mobile/API)
                                │
                                ↓
                          REST API Gateway
                                │
        ┌───────────────────────┼───────────────────────┐
        ↓                       ↓                       ↓
    [Campaigns]           [Businesses]            [Products]
     (Marketing)          (Companies)           (Catalog Items)
        │                   │                      │
        └───────────────────┼──────────────────────┘
                            ↓
                      [PostgreSQL DB]
                            │
        ┌───────────────────┼───────────────────┐
        ↓                   ↓                   ↓
    [Users]          [Favorites]         [External Sync]
    (Accounts)       (Bookmarks)      (Backbone/Mercury)
        │               │
        └───────────────┴──→ [Kafka] → [Email Service]
                              (Async Queue)
```

---

## 🔄 Request Flow (5 Steps)

```
1️⃣  CLIENT SENDS REQUEST
    GET /api/v1/campaigns/123
    Authorization: Bearer <JWT>

2️⃣  SPRING SECURITY + CONTROLLER
    ✓ Verify JWT token
    ✓ Extract user & permissions
    ✓ Route to service

3️⃣  SERVICE LAYER (Business Logic)
    ✓ Validate input
    ✓ Check authorization
    ✓ Apply business rules

4️⃣  REPOSITORY LAYER (Database)
    ✓ Execute JPA query
    ✓ Fetch from PostgreSQL
    ✓ Return entity

5️⃣  RESPONSE
    ✓ Map entity → DTO
    ✓ Set HTTP status
    ✓ Return JSON

    200 OK
    {
      "id": "123",
      "name": "Summer Sale",
      "lastUpdate": "2026-03-26T10:30:00Z"
    }
```

---

## 📋 Core Entities (What They Are)

| Entity | Purpose | Key Fields | Example |
|--------|---------|-----------|---------|
| **Campaign** 📢 | Marketing promotion | name, businessId, startDate, endDate | "Summer Sale 2026" |
| **Business** 🏢 | Organization | name, description, email | "Acme Corp" |
| **Product** 📦 | Catalog item | name, businessId, price | "Widget A-100" |
| **User** 👤 | Account holder | username, email, password | "john_doe@email.com" |
| **Favorite** ⭐ | Saved product | userId, productId | User 123 saved Product 456 |

---

## 🔐 Key Concepts Explained

### **Optimistic Locking** 🔒
**Problem:** Two users edit same record simultaneously
```
User A (10:30) ──┐  lastUpdate: "10:30:00Z"
                 ├──→ Both want to update campaign
User B (10:30) ──┘  lastUpdate: "10:30:00Z"

Timeline:
10:30:30 → User B saves first ✓
10:30:45 → User A tries to save ✗ (409 Conflict - stale data)
         → User A refreshes and retries ✓
```

### **Soft Delete** 🗑️
**Keeps historical data:**
```
DELETE /api/v1/campaigns/123
  ↓
Database: Set deleted = true (NOT removed)
  ↓
Future queries: Auto-exclude deleted records
  ↓
Benefit: Audit trail + data recovery
```

### **Async Processing** ⚡
**Don't wait for everything:**
```
POST /api/v1/campaigns/new
  ↓
1. Save to database ✓ (fast)
2. Return 201 Created ✓ (instant)
3. Send email in background ✓ (async)
User doesn't wait for email!
```

---

## 🚀 Common Operations

### Create Campaign
```
POST /api/v1/campaigns
{
  "name": "Summer Sale",
  "businessId": "biz-123",
  "startDate": "2026-06-01",
  "endDate": "2026-08-31"
}

↓ Response (201 Created)
{
  "id": "camp-456",
  "name": "Summer Sale",
  "lastUpdate": "2026-03-26T10:30:00Z"
}
```

### Search Campaigns
```
GET /api/v1/campaigns?q=sale&businessId=123&page=0&size=20

↓ Response (200 OK)
{
  "content": [
    { "id": "1", "name": "Summer Sale", ... },
    { "id": "2", "name": "Spring Sale", ... }
  ],
  "totalElements": 25,
  "totalPages": 2,
  "currentPage": 0
}
```

### Update Campaign
```
PUT /api/v1/campaigns/123
{
  "name": "Updated Name",
  "lastUpdate": "2026-03-26T10:30:00Z"  ← Must send current version!
}

↓ Response (202 Accepted) or (409 Conflict)
```

### Delete Campaign
```
DELETE /api/v1/campaigns/123

↓ Response (204 No Content - empty)
Campaign marked as deleted (soft delete)
```

---

## 📊 HTTP Status Codes

```
✅ SUCCESS
   200 OK              - GET succeeded
   201 Created         - POST succeeded (created)
   202 Accepted        - PUT/PATCH accepted (async)
   204 No Content      - DELETE succeeded

❌ CLIENT ERROR
   400 Bad Request     - Invalid input
   401 Unauthorized    - No valid JWT
   403 Forbidden       - Lack permission
   404 Not Found       - Resource doesn't exist
   409 Conflict        - Stale update (lastUpdate mismatch)

💥 SERVER ERROR
   500 Internal Error  - Unexpected failure
```

---

## 🔍 Search & Filter Examples

```
1️⃣  Free-text Search
    ?q=sale
    ↳ Searches name + description for "sale"

2️⃣  Structured Filter
    ?businessId=123&categoryId=456
    ↳ Only campaigns from business 123 in category 456

3️⃣  Date Range
    ?startDate_from=2026-01-01&startDate_to=2026-12-31
    ↳ Campaigns between dates

4️⃣  Pagination
    ?page=0&size=20&sort=name,asc
    ↳ First page, 20 items per page, sorted by name

5️⃣  Combined
    ?q=sale&businessId=123&page=0&size=10
    ↳ Search for "sale" in business 123, first 10 results
```

---

## 🔄 Complete Create-Read-Update-Delete Cycle

```
┌──────────────────────────────────────────────────────────────┐
│                    CAMPAIGN LIFECYCLE                         │
└──────────────────────────────────────────────────────────────┘

1. CREATE (POST)
   Request: { name: "Sale" }
   ↓
   Server: Validate → Save to DB → Notify Backbone/Kafka
   ↓
   Response: 201 Created + ID

2. READ (GET)
   Request: /campaigns/123
   ↓
   Server: Fetch from DB → Map to DTO
   ↓
   Response: 200 OK + Campaign data

3. SEARCH (GET with filters)
   Request: /campaigns?q=sale&page=0
   ↓
   Server: Parse filters → Query DB → Paginate
   ↓
   Response: 200 OK + List + Pagination info

4. UPDATE (PUT with lastUpdate)
   Request: { name: "New Sale", lastUpdate: "10:30:00Z" }
   ↓
   Server: Check version match → Merge → Save → Notify
   ↓
   Response: 202 Accepted OR 409 Conflict

5. DELETE (DELETE)
   Request: /campaigns/123
   ↓
   Server: Mark deleted=true → Save → Notify
   ↓
   Response: 204 No Content

6. RESTORE (Undelete - if needed)
   Server-side operation: Set deleted=false
   Response: Campaign restored
```

---

## 🏗️ Architecture Layers

```
┌─────────────────────────────────────┐
│  PRESENTATION LAYER                 │
│  (Controllers + Swagger/OpenAPI)    │
│  ├─ CampaignController              │
│  ├─ BusinessController              │
│  └─ UserController                  │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│  SERVICE LAYER (Business Logic)     │
│  ├─ CampaignServiceImpl              │
│  ├─ Validation                      │
│  ├─ Authorization                   │
│  └─ External notifications          │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│  DATA ACCESS LAYER                  │
│  (Spring Data JPA)                  │
│  ├─ CampaignRepository              │
│  ├─ BusinessRepository              │
│  └─ UserRepository                  │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│  DATABASE LAYER                     │
│  (PostgreSQL)                       │
│  ├─ campaigns table                 │
│  ├─ businesses table                │
│  └─ users table                     │
└─────────────────────────────────────┘
```

---

## 🔐 Authentication & Authorization

```
1️⃣ USER LOGS IN
   POST /auth/login
   { username, password }
   ↓
   Server generates JWT token

2️⃣ CLIENT STORES TOKEN
   localStorage.setItem('token', jwt)

3️⃣ EVERY REQUEST INCLUDES TOKEN
   GET /api/v1/campaigns/123
   Headers: Authorization: Bearer <JWT>

4️⃣ SERVER VALIDATES TOKEN
   ✓ Signature valid?
   ✓ Expired?
   ✓ Claims present?

5️⃣ AUTHORIZATION CHECK
   ✓ User owns resource?
   ✓ User has role?

6️⃣ RESPONSE
   ✓ 200 OK (all good)
   ✗ 401 Unauthorized (invalid token)
   ✗ 403 Forbidden (no permission)
```

---

## 📈 Database Relationships

```
┌─────────────────┐         ┌──────────────────┐
│    Business     │◄────1:N─┤    Campaign      │
│  (company)      │         │  (promotion)     │
├─────────────────┤         ├──────────────────┤
│ id              │         │ id               │
│ name            │         │ name             │
│ email           │         │ business_id (FK) │
└─────────────────┘         │ start_date       │
        │                   │ end_date         │
        │ 1:N              └──────────────────┘
        ↓
┌─────────────────┐
│    Product      │
│  (catalog)      │
├─────────────────┤
│ id              │
│ name            │
│ business_id (FK)│
└─────────────────┘
        │
        │ 1:N
        ↓
┌──────────────────┐         ┌──────────────────┐
│ UserFavorite     │────N:1──┤      User        │
│ (bookmark)       │         │  (account)       │
├──────────────────┤         ├──────────────────┤
│ id               │         │ id               │
│ user_id (FK)     │         │ username         │
│ product_id (FK)  │         │ email            │
└──────────────────┘         └──────────────────┘
```

---

## ⏱️ Performance Notes

```
RESPONSE TIMES:
├─ Simple GET          : 50-100ms
├─ Create/Update       : 100-200ms
├─ Search (paginated)  : 200-500ms
└─ Async operations    : Instant return

OPTIMIZATION TIPS:
✓ Always paginate (max ~100 items/page)
✓ Use filters to narrow results
✓ Combine filters for speed
✓ Sort by indexed columns
✗ Never fetch all records at once
✗ Don't use free-text search alone
```

---

## 🚨 Common Issues & Solutions

| Problem | Cause | Solution |
|---------|-------|----------|
| **401 Unauthorized** | No valid JWT | User logs in → get token |
| **403 Forbidden** | No permission | Check user role/ownership |
| **404 Not Found** | Resource missing | Verify ID; refresh data |
| **409 Conflict** | Stale update | Refresh; send current lastUpdate |
| **400 Bad Request** | Invalid input | Check required fields |
| **500 Error** | Server crash | Contact support; check logs |

---

## 📚 Documentation Quick Links

| Document | Purpose | For |
|----------|---------|-----|
| **PROJECT_DOCUMENTATION.md** | Complete technical details | Architects, Developers |
| **ARCHITECTURE_DIAGRAMS.md** | Visual flows & diagrams | Everyone |
| **BA_QUICK_REFERENCE.md** | Detailed BA guide | Business Analysts |
| **CLAUDE.md** | Development guide | Developers |
| **README.md** | Project overview | Everyone |

---

## 🎯 Key Takeaways

✅ **Simple 3-Layer Architecture:** Controller → Service → Repository

✅ **RESTful APIs:** Standard GET/POST/PUT/DELETE operations

✅ **Optimistic Locking:** Prevents conflicting concurrent updates

✅ **Soft Delete:** Keep data, just mark as deleted

✅ **Async Processing:** User gets immediate response, background work continues

✅ **PostgreSQL Database:** Reliable data persistence

✅ **Kafka Messaging:** Reliable async notifications

✅ **JWT Authentication:** Token-based security

✅ **Pagination:** Handle large datasets efficiently

✅ **External Integration:** Backbone & Mercury services notified

---

## 🔗 Quick Command Reference

```bash
# Build application
mvn -DskipTests package

# Run tests
mvn test

# Generate test coverage report
mvn clean test jacoco:report

# Run full CI check
mvn -B -V -e clean verify

# Run application
java -jar target/directory-backend.jar

# Run specific test class
mvn -Dtest=CampaignControllerTest test
```

---

**For detailed information, see: PROJECT_DOCUMENTATION.md**

**Last Updated:** March 2026 | **Version:** 0.0.1 | **Team:** um-dev-creative
