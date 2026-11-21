package com.umdc.directory.mapper;

import com.umdc.commons.services.config.mapper.MapperAppConfig;
import com.umdc.directory.api.v1.to.CityTO;
import com.umdc.directory.jpa.entity.CityEntity;
import org.mapstruct.Mapper;

/// Converts a city entity to its transfer object.
@Mapper(config = MapperAppConfig.class)
public interface CityMapper {

    /// @param entity the city
    /// @return the transfer object
    CityTO toTO(CityEntity entity);
}
