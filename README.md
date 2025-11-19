# Directory Backend

## Quality Gate Status
[![Quality gate](https://sonarcloud.io/api/project_badges/quality_gate?project=prx-dev_directory-backend&token=30babbee984be4a21f7e4627f90e80c5b47330fa)](https://sonarcloud.io/summary/new_code?id=prx-dev_directory-backend)

## Knowledge Base
[Official Documentation](https://prx.myjetbrains.com/youtrack/articles/DS-A-8/Directory-Backend)

## Example: Get Campaign (GET /api/v1/campaigns/{id})

Successful response now includes category details, status, and terms:

```json
{
  "id": "11111111-2222-3333-4444-555555555555",
  "name": "Holiday Sale",
  "description": "Seasonal discount campaign",
  "startDate": "2025-11-25T00:00:00Z",
  "endDate": "2025-12-31T23:59:59Z",
  "categoryId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "categoryName": "Retail",
  "businessId": "99999999-8888-7777-6666-555555555555",
  "createdDate": "2025-11-01T10:00:00Z",
  "lastUpdate": "2025-11-10T12:00:00Z",
  "discount": 15.0,
  "active": true,
  "status": "active",
  "terms": "Offer valid while supplies last. See store for details."
}
```
