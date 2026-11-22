# Locations (sedes) and search by city (technical guide)

```mermaid
erDiagram
    BUSINESS ||--o{ LOCATION : "business_fk (ON DELETE CASCADE)"
    CITY ||--o{ LOCATION : "city_fk"
    CATEGORY ||--o{ BUSINESS : "category_fk"
    CATEGORY ||--o{ CATEGORY : "parent"
    CITY { uuid id string name string province string country }
    LOCATION { uuid id string address_line string postal_code string phone numeric latitude numeric longitude boolean main }
```

```mermaid
sequenceDiagram
    participant C as Client (BFF)
    participant S as BusinessSearchController
    participant R as BusinessRepository (Specification)
    participant L as LocationRepository
    C->>S: GET /api/v1/businesses/search?city_id&category_id&q&page&per_page
    S->>R: findAll(BusinessSpecifications.from(criteria), PageRequest(name, id))
    R-->>S: Page of businesses (category fetched by EntityGraph)
    S->>L: findByBusinessIdIn(ids of the page)
    L-->>S: locations with their city (JOIN FETCH)
    S-->>C: { total_count, page, per_page, total_pages, data }
```

## Schema (`V4__create_city_and_location.sql`)
- `city(name, province, country)` unique; index on `lower(name)`; seeded with Toronto and eight Ontario cities (`ON CONFLICT DO NOTHING`).
- `location`: `business_fk` (cascade) and `city_fk`, both indexed; checks on latitude, longitude and that they come as a pair; **partial unique index** `(business_fk) WHERE main` = one main per business.
- Applied by hand (Flyway is not configured), like `V2` and `V3`. Entities: `CityEntity`, `LocationEntity`.

## Code
| Piece | Role |
| --- | --- |
| `BusinessSpecifications` | `inCity` (`EXISTS` sub-query on location+city, one row per business), `inCategory` (category or its parent, left join), `matchesText` (lower-case `LIKE` with `ESCAPE`); absent filters are `Specification.unrestricted()` (Spring Data JPA 4 rejects `null`) |
| `CitySpecifications` | Country and name filter of the catalogue |
| `BusinessSearchServiceImpl` | Validates `page`/`per_page` (1-based, max `MAX_PER_PAGE`), stable order `name, id`, one query for the locations of the page (no N+1) |
| `LocationServiceImpl` | Owner check as `deleteBusiness`: `JwtUtil.getUidFromToken` vs the business owner (401/404/403); `LocationValidator` holds the field rules; the old main is demoted and flushed **before** the new one is saved because of the unique index |
| Controllers | `CityController` `/api/v1/cities`, `LocationController` `/api/v1/businesses/{businessId}/locations`, `BusinessSearchController` `/api/v1/businesses/search` (a literal path wins over `/businesses/{id}`; covered by `LocationAndSearchRoutesTest`) |

Errors carry the reason in the `message-error` header. Contract: `src/main/resources/api/index.yaml`.

Tests: `BusinessSearchRepositoryTest` (`@DataJpaTest`, H2 in PostgreSQL mode with `NON_KEYWORDS=USER` and aliases for `uuid_generate_v4`), `LocationServiceImplTest`, `CityServiceImplTest`, `BusinessSearchServiceImplTest`, `LocationAndSearchRoutesTest`. Not covered: real PostgreSQL, query plans on a large data set.
