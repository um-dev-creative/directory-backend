# TASK-012: Create Comprehensive Database Migration Scripts with Flyway

## Priority
**High**

## Status
Open

## Component
- Database Migrations
- Tool: Flyway
- Location: `src/main/resources/db/migration`

## Description
The project uses Flyway for database versioning, but comprehensive migration scripts for all entities and relationships may be incomplete or missing. Create a complete set of migration scripts that match the documented data model.

## Current State
- Flyway is detected in project
- Migration scripts may exist but need verification
- Need scripts for all entities:
  - Business
  - Campaign
  - Product
  - User
  - UserFavorite
  - Category
  - DigitalContact
  - Timezone

## Expected Outcome
- Complete set of Flyway migration scripts in proper order
- All entities, relationships, and constraints defined
- Indexes for performance optimization
- Seed data for categories and timezones
- Repeatable migrations for reference data

## Acceptance Criteria
1. **All entity tables created**
   - Each entity has a corresponding CREATE TABLE script
   - All columns match entity definitions
   - Primary keys defined
   - Foreign keys defined with proper constraints
   - Indexes for query optimization

2. **Migration naming convention**
   - Format: `V{version}__{description}.sql`
   - Example: `V001__create_business_table.sql`
   - Versioned migrations execute in order
   - Repeatable migrations for reference data: `R__seed_categories.sql`

3. **Data integrity constraints**
   - NOT NULL on required fields
   - UNIQUE constraints on unique fields
   - CHECK constraints for valid values
   - Foreign key cascade rules (ON DELETE, ON UPDATE)

4. **Performance indexes**
   - Index on foreign key columns
   - Index on frequently queried columns
   - Composite indexes for common query patterns
   - Text search indexes where applicable

5. **Seed data**
   - Categories (business categories, product categories)
   - Timezones (world timezones)
   - Contact types
   - Role types

## Migration Script Order

### V001__create_category_table.sql
```sql
CREATE TABLE category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    parent_id UUID,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    last_update TIMESTAMP,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) 
        REFERENCES category(id) ON DELETE SET NULL
);

CREATE INDEX idx_category_parent_id ON category(parent_id);
CREATE INDEX idx_category_name ON category(name);
```

### V002__create_user_table.sql
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    roles TEXT,  -- JSON array or comma-separated
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    last_update TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
```

### V003__create_business_table.sql
### V004__create_campaign_table.sql
### V005__create_product_table.sql
### V006__create_user_favorite_table.sql
### V007__create_digital_contact_table.sql
### V008__create_timezone_table.sql
### V009__create_indexes.sql
### R__seed_categories.sql (repeatable)
### R__seed_timezones.sql (repeatable)

## Technical Tasks
- [ ] Audit existing migration scripts
- [ ] Create missing entity table migrations
- [ ] Add foreign key constraints
- [ ] Add performance indexes
- [ ] Create seed data scripts for categories
- [ ] Create seed data scripts for timezones
- [ ] Add rollback scripts (if needed)
- [ ] Test migrations on clean database
- [ ] Test migrations on existing database (if applicable)
- [ ] Document migration dependencies
- [ ] Add migration validation to CI/CD

## Testing Requirements
1. **Clean database migration**
   - Drop all tables
   - Run all migrations
   - Verify all tables created
   - Verify all constraints in place

2. **Incremental migration**
   - Start from V001
   - Apply each migration sequentially
   - Verify flyway_schema_history table
   - Check for migration errors

3. **Rollback testing**
   - Test rollback scripts (if implemented)
   - Verify data integrity after rollback

4. **Seed data verification**
   - Verify categories are populated
   - Verify timezones are populated
   - Check for duplicate seeds on re-run (idempotent)

## Flyway Configuration
```yaml
# application.yml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
    out-of-order: false
```

## Migration Best Practices
- **One change per migration** - easier to debug
- **Never modify applied migrations** - create new migration instead
- **Use transactions** - ensure atomicity
- **Test on production-like data** - find issues early
- **Document breaking changes** - help teams prepare
- **Version format** - V{major}{minor}{patch}__{description}

## Dependencies
- Flyway (already in project)
- PostgreSQL JDBC driver (already in project)

## Estimated Effort
**12-16 hours**
- Audit existing migrations: 2 hours
- Create entity migrations (8 tables): 4 hours
- Add indexes and constraints: 2 hours
- Create seed data: 2 hours
- Testing and validation: 3 hours
- Documentation: 1 hour

## References
- Entities: `src/main/java/com/prx/directory/jpa/entity/*.java`
- Migrations: `src/main/resources/db/migration/`
- Flyway docs: https://flywaydb.org/documentation/

## Quality Gates
- [ ] All migrations execute successfully on clean database
- [ ] All entity tables created with correct schema
- [ ] All foreign keys and constraints in place
- [ ] Seed data populated correctly
- [ ] flyway_schema_history table shows all migrations
- [ ] No migration errors in CI pipeline

## Database Schema Documentation
After migrations are complete, generate schema documentation:
- Use SchemaSpy or similar tool
- Document all tables, columns, relationships
- Include ER diagram
- Add to `docs/DATABASE_SCHEMA.md`

## Rollback Strategy
Document rollback approach:
- Flyway doesn't support automatic rollback
- Manual rollback scripts for critical migrations
- Database backup before major migrations
- Ability to restore from backup

## Production Migration Checklist
- [ ] Backup production database
- [ ] Test migrations on staging
- [ ] Schedule maintenance window
- [ ] Run migrations with monitoring
- [ ] Verify application starts successfully
- [ ] Verify critical queries work
- [ ] Monitor for errors
- [ ] Have rollback plan ready

## Business Value
Proper database migrations ensure:
- **Data integrity** - constraints prevent bad data
- **Version control** - schema changes tracked
- **Reproducibility** - same schema in all environments
- **Team coordination** - clear migration history
- **Production safety** - tested migrations reduce risk

## Notes
This is a foundational task. Proper migrations are critical for production deployments and team collaboration. Invest time to get this right.

