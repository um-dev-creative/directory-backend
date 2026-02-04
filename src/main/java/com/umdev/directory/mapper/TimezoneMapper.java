package com.umdev.directory.mapper;

import com.umdev.commons.services.config.mapper.MapperAppConfig;
import com.umdev.directory.api.v1.to.TimezoneResumeTO;
import com.umdev.directory.api.v1.to.TimezoneTO;
import com.umdev.directory.jpa.entity.TimezoneEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for converting TimezoneEntity to TimezoneTO.
 */
@Mapper(
        // Specifies the configuration class to use for this mapper.
        config = MapperAppConfig.class
)
public interface TimezoneMapper {

    /**
     * Converts a TimezoneEntity to a TimezoneTO.
     *
     * @param timezoneEntity the entity to convert
     * @return the converted TimezoneTO
     */
    @Mapping(target= "createdAt", source = "createdAt")
    @Mapping(target = "lastUpdate", source = "lastUpdated")
    TimezoneTO toTimezoneTO(TimezoneEntity timezoneEntity);

    /**
     * Converts a TimezoneEntity to a TimezoneResumeTO.
     *
     * @param timezoneEntity the TimezoneEntity to be converted
     * @return a TimezoneResumeTO containing the id, name, and abbreviation from the given entity
     */
    @Mapping(target = "name", source = "name")
    @Mapping(target = "abbreviation", source = "abbreviation")
    TimezoneResumeTO toTimezoneResume(TimezoneEntity timezoneEntity);
}

