# Requirement: Partner Profile Image Upload & Cloudflare Integration

## Summary
Create a new operation to allow uploading a partner's profile image, store the image in a Cloudflare bucket, and reference the image in the business record.

## Background
Currently, the system does not support uploading or associating a profile image for partners (businesses). This feature will enable partners to have a profile image, improving the user experience and business branding.

## Requirements

### 1. API Operation
- Implement a new API endpoint to upload a partner's profile image.
- The endpoint should:
  - Accept image files (JPG, PNG, etc.)
  - Validate file type and size (configurable limit)
  - Store the image in a designated Cloudflare bucket
  - Return the image reference (URL or key) upon success

### 2. Cloudflare Bucket Integration
- Configure the application to upload images to a Cloudflare bucket.
- Store necessary credentials/configuration securely (Vault/Config Server).
- Ensure uploaded images are accessible via a public or signed URL as appropriate.

### 3. Database Schema Change
- Add a new column `profile_image_ref` to the `business` table in the `directory_site` schema.
  - Type: VARCHAR(512) (or appropriate for image URL/key)
  - Nullable: Yes (existing records remain valid)
- Update the Business JPA entity and record/DTO to include this field.

### 4. Backward Compatibility
- Ensure all existing `/api/v1/*` contracts remain backward compatible.
- Existing business records without a profile image must continue to function as before.

### 5. Documentation
- Update OpenAPI spec (`index.yaml`) to document the new endpoint and field.
- Update relevant JavaDoc and in-code comments.

### 6. Testing
- Add/extend unit and integration tests for the new upload operation and entity changes.
- Validate image upload, storage, and retrieval flows.

## Acceptance Criteria
- [ ] New API endpoint for profile image upload is available and documented
- [ ] Images are uploaded and stored in Cloudflare bucket
- [ ] `profile_image_ref` column exists in `business` table and is mapped in the entity/DTO
- [ ] Existing business records remain unaffected
- [ ] All tests pass and coverage is maintained
- [ ] Documentation is updated

## Risks & Mitigations
- **Cloudflare integration complexity**: Use official SDKs and test in a non-prod environment first.
- **Security**: Ensure only authorized users can upload images; validate file types and sizes.
- **Migration**: Schema change must be backward compatible; provide migration script.

## References
- [CLAUDE.md](../CLAUDE.md)
- [AGENTS.md](../AGENTS.md)
- [OpenAPI Spec](../src/main/resources/api/index.yaml)

---
*Created: 2026-04-10*
*Type: Requirement*
*Status: Open*

