package com.umdc.directory.mapper;

import com.umdc.commons.services.config.mapper.MapperAppConfig;
import com.umdc.directory.api.v1.to.LocationTO;
import com.umdc.directory.jpa.entity.LocationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/// Converts a location entity to its transfer object (its city included).
@Mapper(config = MapperAppConfig.class, uses = CityMapper.class)
public interface LocationMapper {

    /// @param entity the location, with its city loaded
    /// @return the transfer object
    @Mapping(target = "businessId", source = "business.id")
    @Mapping(target = "createdAt", source = "createdDate")
    @Mapping(target = "lastUpdate", source = "lastUpdate")
    LocationTO toTO(LocationEntity entity);
}
