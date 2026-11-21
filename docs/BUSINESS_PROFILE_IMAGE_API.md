# Business Profile Image API — Technical Documentation

**Controller:** `BusinessProfileImageController`  
**Base Path:** `/api/v1/businesses/images`  
**Version:** v1  
**Last Updated:** April 12, 2026

---

## Overview

The Business Profile Image API exposes endpoints for uploading and retrieving images associated
with business profiles in the Directory platform. Images are stored in **Cloudflare R2** object
storage; only the resulting public CDN URL is persisted in the `business.profile_image_ref` column.

The controller implements the shared `ImageApi` interface (from `com.umdc.commons.services.cloudflare`)
which provides the generic upload and reference-retrieval operations. It extends those capabilities
with a business-scoped upload that automatically updates the business entity.

**Authentication:** All endpoints require a valid session token supplied in the `session-token`
request header.

---

## Data Transfer Objects

### `ImageUploadRequest` *(inbound — multipart)*

| Field         | Type     | Required | Description |
|---------------|----------|----------|-------------|
| `objectKey`   | `string` | No       | Storage key for the object. Auto-generated under `businesses/` prefix when omitted. |
| `image`       | `bytes`  | Yes      | Raw image bytes sent as a multipart part. |
| `contentType` | `string` | Yes      | MIME type of the image (e.g. `image/jpeg`, `image/png`). |

### `ImageUploadResponse` *(outbound)*

| Field         | Type      | Description |
|---------------|-----------|-------------|
| `objectKey`   | `string`  | The storage key under which the image was saved in R2. |
| `publicUrl`   | `string`  | Publicly accessible CDN URL of the uploaded image. |
| `contentType` | `string`  | MIME type of the stored image. |
| `size`        | `integer` | Size of the uploaded image in bytes. |

**Example:**
```json
{
  "objectKey": "businesses/3c50b50b-d117-44f8-b350-b441518e11c9/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg",
  "publicUrl": "https://cdn.example.com/businesses/3c50b50b-d117-44f8-b350-b441518e11c9/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg",
  "contentType": "image/jpeg",
  "size": 204800
}
```

### `ImageReferenceResponse` *(outbound)*

| Field         | Type     | Description |
|---------------|----------|-------------|
| `objectKey`   | `string` | The storage key of the object in R2. |
| `publicUrl`   | `string` | Publicly accessible CDN URL for the referenced image. |
| `contentType` | `string` | MIME type — may be `null` for reference-only lookups. |

**Example:**
```json
{
  "objectKey": "businesses/photo.jpg",
  "publicUrl": "https://cdn.example.com/businesses/photo.jpg",
  "contentType": null
}
```

---

## Endpoints

### 1. Upload Image (Generic)

Uploads a raw image to Cloudflare R2 without associating it with a specific business entity.
When `objectKey` is not provided the service auto-generates a key under the `businesses/` prefix.

| Attribute    | Value |
|--------------|-------|
| **Method**   | `POST` |
| **Path**     | `/api/v1/businesses/images` |
| **Auth**     | `session-token` header (required) |
| **Consumes** | `multipart/form-data` |
| **Produces** | `application/json` |
| **Source**   | Inherited from `com.umdc.commons.services.cloudflare.controller.ImageApi` |

#### Request Headers

| Header          | Type     | Required | Description |
|-----------------|----------|----------|-------------|
| `session-token` | `string` | Yes      | Valid session JWT token. |

#### Request Parts (multipart/form-data)

| Part          | Type     | Required | Description |
|---------------|----------|----------|-------------|
| `objectKey`   | `string` | No       | Explicit storage key. If omitted, key is auto-generated as `businesses/{uuid}.{ext}`. |
| `image`       | `bytes`  | Yes      | Binary image data. |
| `contentType` | `string` | No       | MIME type. Required for correct file extension resolution. |

#### Storage Key Generation

When `objectKey` is `null` the service generates: `businesses/{random-uuid}.{ext}`

The file extension is resolved from the MIME type:

| MIME Type | Extension |
|-----------|-----------|
| `image/png`  | `.png`  |
| `image/webp` | `.webp` |
| `image/gif`  | `.gif`  |
| `image/jpeg` *(or any other)* | `.jpg` |

#### Responses

| Status | Condition | Body |
|--------|-----------|------|
| `201 Created` | Upload succeeded | `ImageUploadResponse` |
| `400 Bad Request` | Validation failed (`ImageValidationException`) | Error message |
| `401 Unauthorized` | Missing or invalid session token | — |
| `500 Internal Server Error` | R2 storage failure (`ImageUploadException`) | Error message |

#### Example Request

```http
POST /api/v1/businesses/images HTTP/1.1
session-token: <session-jwt>
Content-Type: multipart/form-data; boundary=----boundary

------boundary
Content-Disposition: form-data; name="objectKey"

businesses/custom/logo.jpg
------boundary
Content-Disposition: form-data; name="image"; filename="logo.jpg"
Content-Type: image/jpeg

<binary image data>
------boundary
Content-Disposition: form-data; name="contentType"

image/jpeg
------boundary--
```

#### Example Response — 201 Created

```json
{
  "objectKey": "businesses/custom/logo.jpg",
  "publicUrl": "https://cdn.example.com/businesses/custom/logo.jpg",
  "contentType": "image/jpeg",
  "size": 51200
}
```

---

### 2. Get Image Reference

Returns the public CDN URL for an image stored in R2 identified by its object key.
Image bytes are **not** downloaded — only the URL is resolved.

| Attribute    | Value |
|--------------|-------|
| **Method**   | `GET` |
| **Path**     | `/api/v1/businesses/images` |
| **Auth**     | `session-token` header (required) |
| **Produces** | `application/json` |
| **Source**   | Inherited from `com.umdc.commons.services.cloudflare.controller.ImageApi` |

#### Request Headers

| Header          | Type     | Required | Description |
|-----------------|----------|----------|-------------|
| `session-token` | `string` | Yes      | Valid session JWT token. |

#### Request Parameters

| Parameter   | In    | Type     | Required | Description |
|-------------|-------|----------|----------|-------------|
| `objectKey` | query | `string` | Yes      | The storage key of the object to resolve (e.g. `businesses/logo.jpg`). |

#### Responses

| Status | Condition | Body |
|--------|-----------|------|
| `200 OK` | Reference resolved successfully | `ImageReferenceResponse` |
| `401 Unauthorized` | Missing or invalid session token | — |
| `404 Not Found` | Object key does not exist in R2 | — |
| `500 Internal Server Error` | R2 client failure (`ImageStorageException`) | Error message |

#### Example Request

```http
GET /api/v1/businesses/images?objectKey=businesses%2F3c50b50b%2Flogo.jpg HTTP/1.1
session-token: <session-jwt>
```

#### Example Response — 200 OK

```json
{
  "objectKey": "businesses/3c50b50b/logo.jpg",
  "publicUrl": "https://cdn.example.com/businesses/3c50b50b/logo.jpg",
  "contentType": null
}
```

---

### 3. Upload Profile Image for Business *(Business-Scoped)*

Uploads a profile image for a specific business, stores it in Cloudflare R2, and persists the
resulting public URL in `business.profile_image_ref`. The storage key is automatically scoped to
the business:

```
businesses/{businessId}/{random-uuid}.{ext}
```

This is the **recommended** upload endpoint when associating an image to a business entity.

| Attribute    | Value |
|--------------|-------|
| **Method**   | `POST` |
| **Path**     | `/api/v1/businesses/images/{businessId}` |
| **Auth**     | `session-token` header (required) |
| **Consumes** | `multipart/form-data` |
| **Produces** | `application/json` |
| **Source**   | Defined directly in `BusinessProfileImageController` |

#### Path Parameters

| Parameter    | Type   | Required | Description |
|--------------|--------|----------|-------------|
| `businessId` | `UUID` | Yes      | UUID of the business to associate the image with. |

#### Request Headers

| Header          | Type     | Required | Description |
|-----------------|----------|----------|-------------|
| `session-token` | `string` | Yes      | Valid session JWT token. |

#### Request Parts (multipart/form-data)

| Part          | Type     | Required | Description |
|---------------|----------|----------|-------------|
| `image`       | `bytes`  | Yes      | Raw binary image data. |
| `contentType` | `string` | No       | MIME type of the image. Defaults to `image/jpeg` when omitted. |

#### Side Effects

- The business entity's `profile_image_ref` field is updated in the database with the generated
  public CDN URL.
- No previous image is deleted from R2. Callers are responsible for cleaning up stale objects.

#### Responses

| Status | Condition | Body |
|--------|-----------|------|
| `201 Created` | Upload succeeded, business updated | `ImageUploadResponse` |
| `400 Bad Request` | Validation failed (`ImageValidationException`) | Error message |
| `401 Unauthorized` | Missing or invalid session token | — |
| `404 Not Found` | No business entity found for `businessId` | *(empty body)* |
| `500 Internal Server Error` | R2 storage or DB failure (`ImageUploadException`) | Error message |

#### Example Request

```http
POST /api/v1/businesses/images/3c50b50b-d117-44f8-b350-b441518e11c9 HTTP/1.1
session-token: <session-jwt>
Content-Type: multipart/form-data; boundary=----boundary

------boundary
Content-Disposition: form-data; name="image"; filename="profile.jpg"
Content-Type: image/jpeg

<binary image data>
------boundary
Content-Disposition: form-data; name="contentType"

image/jpeg
------boundary--
```

#### Example Response — 201 Created

```json
{
  "objectKey": "businesses/3c50b50b-d117-44f8-b350-b441518e11c9/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg",
  "publicUrl": "https://cdn.example.com/businesses/3c50b50b-d117-44f8-b350-b441518e11c9/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg",
  "contentType": "image/jpeg",
  "size": 204800
}
```

#### Example Response — 404 Not Found

```http
HTTP/1.1 404 Not Found
```

*(No response body — business entity was not found for the provided ID.)*

---

## Error Handling

| Exception | HTTP Status | Description |
|-----------|-------------|-------------|
| `ImageValidationException` | `400 Bad Request` | Image failed validation (unsupported format, size exceeded, etc.). |
| `ImageUploadException` | `500 Internal Server Error` | Upload to Cloudflare R2 failed. |
| `ImageStorageException` | `500 Internal Server Error` | R2 storage client error during read or reference resolution. |
| Business not found | `404 Not Found` | No business entity exists for the given `businessId`. |

---

## Supported MIME Types

| MIME Type    | Extension | Notes |
|--------------|-----------|-------|
| `image/jpeg` | `.jpg`    | Default when `contentType` is omitted. |
| `image/png`  | `.png`    | |
| `image/webp` | `.webp`   | |
| `image/gif`  | `.gif`    | |
| *(other)*    | `.jpg`    | Falls back to JPEG; provide the correct MIME type for proper extension resolution. |

---

## Storage Key Conventions

| Scenario | Key Pattern |
|----------|-------------|
| Generic upload — no key supplied | `businesses/{random-uuid}.{ext}` |
| Generic upload — explicit key supplied | `{provided-objectKey}` (used as-is) |
| Business-scoped upload | `businesses/{businessId}/{random-uuid}.{ext}` |

---

## Service Architecture

```
BusinessProfileImageController  (REST layer — /api/v1/businesses/images)
        │
        ▼
BusinessProfileImageService     (interface — extends ImageService)
        │
        ▼
BusinessProfileImageServiceImpl (Spring @Service)
        ├── CloudflareR2StorageClient   uploadImage() / getPublicUrl()
        └── BusinessRepository          findById() / save()
```

---

## API Review Report

### ✅ Passed Checks
- Three operations correctly mapped: generic upload, reference lookup, and business-scoped upload.
- HTTP methods follow REST conventions (`POST` for mutations, `GET` for reads).
- Status codes align with service behaviour: `201 Created`, `200 OK`, `404 Not Found`, `400 Bad Request`, `500 Internal Server Error`.
- Authentication requirement (`session-token` header) applied uniformly across all endpoints.
- Content types correct: `multipart/form-data` for requests, `application/json` for responses.
- Default `contentType → image/jpeg` fallback is documented and covered by unit tests.
- Storage key scoping convention is consistent and documented.
- Business entity side-effect (`profile_image_ref` update) clearly called out.
- Exception-to-HTTP-status mapping is complete.

### ⚠️ Warnings
- `getService()` is a delegation bridge required by the `ImageApi` interface contract — it is
  **not** an HTTP endpoint. No consumer should invoke it directly over HTTP.
- Stale/orphaned images in R2 are **not** cleaned up when a new profile image is uploaded.
  Consider a lifecycle/cleanup strategy to control storage costs.
- `contentType` silently defaults to `image/jpeg`; callers should always supply the correct MIME
  type to avoid mismatched file extensions in the stored key.

### ❌ Failed Checks
- **`index.yaml` gap:** None of the three endpoints — `POST /api/v1/businesses/images`,
  `GET /api/v1/businesses/images`, or `POST /api/v1/businesses/images/{businessId}` — are present
  in `src/main/resources/api/index.yaml`. All three must be added before merge to keep the OpenAPI
  spec in sync with the implementation.

### 📋 Recommendations
- Add `@Operation`, `@ApiResponses`, and `@Tag(name = "business-images")` Swagger annotations to
  the controller methods so Springdoc can auto-generate the spec.
- Consider a `DELETE /api/v1/businesses/images/{businessId}/profile` endpoint to explicitly
  remove a profile image and clear `profile_image_ref`.
- Enforce and document a maximum image size at the Spring layer
  (`spring.servlet.multipart.max-file-size`) and document the accepted limit in this file.
- Verify the exact sub-paths exposed by `ImageApi` in `commons-services:0.0.2` and reconcile
  them with the base-path mapping applied by `BusinessProfileImageController`.

