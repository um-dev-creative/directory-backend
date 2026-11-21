---
name: MapStruct Mapping
description: Skill for MapStruct-based DTO-entity mapping
applies-to:
  - Developer
---

# MapStruct Mapping Skill

## Scope

This skill covers MapStruct mapper patterns used to convert between JPA entities
and DTOs in the **directory-backend** project.

## Mapper Configuration

All mappers use the shared config from `com.umdc.commons.services.config.mapper.MapperAppConfig`:

```java
@Mapper(config = MapperAppConfig.class)
public interface CampaignMapper {
    CampaignTO toTO(CampaignEntity entity);
    CampaignEntity toEntity(CampaignTO to);
    List<CampaignResumeTO> toResumeList(List<CampaignEntity> entities);
}
```

## Current Mappers

- `BusinessMapper` — Business entity to/from DTO
- `BusinessProductMapper` — Business-product link mapping
- `CampaignMapper` — Campaign entity to/from DTO
- `CategoryMapper` — Category entity to/from DTO
- `ConfirmCodeMapper` — Confirmation code mapping
- `DigitalContactMapper` — Digital contact mapping
- `FavoriteMapper` — Favorite entity mapping
- `GetUserMapper` — User read mapping
- `ProductMapper` — Product entity to/from DTO
- `PutUserMapper` — User update mapping
- `TimezoneMapper` — Timezone entity mapping
- `UserCreateMapper` — User creation mapping

## Conventions

- Mapper interfaces live in `com.umdc.directory.mapper`.
- MapStruct generates implementations at compile time via `maven-compiler-plugin` annotation processor.
- Use `@Mapping` annotations for field name mismatches.
- Use `default` methods for complex transformations.
- Never use MapStruct for entity-to-entity — only entity-to-DTO and vice versa.
