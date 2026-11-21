# Database migration scripts V2 and V3 (user guide)

Two SQL scripts add columns that the application already expects. Run them **once per environment, before** deploying a version that reads or writes those columns.

| Script | Adds | Used for |
| --- | --- | --- |
| `V2__add_deleted_fields_user_favorite.sql` | `user_favorite.deleted_at`, `deleted_by` | Audit of removed favorites |
| `V3__add_profile_image_ref_to_business.sql` | `business.profile_image_ref` | Business profile image (Cloudflare R2 URL) |

## How to apply
1. Back up the database (or check that your platform keeps point-in-time recovery).
2. Run `V2` then `V3` with `psql` against the Directory database, as a user allowed to alter `directory_site` tables.
3. Both scripts can be run again safely (`IF NOT EXISTS`); columns are nullable, so existing rows and running instances are not affected.

## What to expect
| Situation | Result |
| --- | --- |
| Scripts not applied and the code reads `profile_image_ref` or `deleted_at` | SQL error "column does not exist" |
| Scripts applied | Image upload and soft-delete audit work; nothing changes for existing data |
