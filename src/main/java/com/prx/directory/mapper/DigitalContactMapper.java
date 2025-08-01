package com.prx.directory.mapper;

import com.prx.directory.api.v1.to.DigitalContactTO;
import com.prx.directory.jpa.entity.DigitalContactEntity;
import org.mapstruct.*;

/**
 * Mapper for converting DigitalContactEntity to DigitalContactTO.
 */
@Mapper(
        // Specifies that the mapper should be a Spring bean.
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {DigitalContactTO.class, DigitalContactEntity.class}
)
@MapperConfig(
        // Specifies that the mapper should fail if there are any unmapped properties.
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        // Specifies that the mapper should fail if there are any unmapped properties.
        unmappedTargetPolicy = ReportingPolicy.IGNORE
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

