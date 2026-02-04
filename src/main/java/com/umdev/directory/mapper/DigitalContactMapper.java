package com.umdev.directory.mapper;

import com.umdev.commons.services.config.mapper.MapperAppConfig;
import com.umdev.directory.api.v1.to.DigitalContactTO;
import com.umdev.directory.jpa.entity.DigitalContactEntity;
import org.mapstruct.*;

/**
 * Mapper for converting DigitalContactEntity to DigitalContactTO.
 */
@Mapper(
        // Specifies that the mapper should be a Spring bean.
        uses = {DigitalContactTO.class, DigitalContactEntity.class},
        config = MapperAppConfig.class
)
public interface DigitalContactMapper {
    /**
     * Converts a DigitalContactEntity to a DigitalContactTO.
     *
     * @param entity the DigitalContactEntity
     * @return the DigitalContactTO
     */
    @Mapping(target = "lastUpdate", source = "lastUpdate")
    @Mapping(target = "createdAt", source = "createdDate")
    @Mapping(target = "businessId", source = "business.id")
    @Mapping(target = "contactTypeId", source = "contactType.id")
    DigitalContactTO toTO(DigitalContactEntity entity);
}

