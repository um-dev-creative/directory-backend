package com.prx.directory.mapper;

import com.prx.directory.api.v1.to.ConfirmCodeRequest;
import com.prx.directory.client.mercury.to.VerificationCodeRequest;
import org.mapstruct.*;

import java.util.UUID;

@Mapper(
        // Specifies that the mapper should be a Spring bean.
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {ConfirmCodeRequest.class, VerificationCodeRequest.class}
)
@MapperConfig(
        // Specifies that the mapper should fail if there are any unmapped properties.
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        // Specifies that the mapper should fail if there are any unmapped properties.
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ConfirmCodeMapper {

    /// Maps a ConfirmCodeRequest to a VerificationCodeRequest.
    ///
    /// @param confirmCodeRequest the ConfirmCodeRequest to map
    /// @return the VerificationCodeRequest
    /// @see ConfirmCodeRequest
    /// @see VerificationCodeRequest
    /// @see ConfirmCodeMapper
    @Mapping(target = "userId", source = "confirmCodeRequest.userId")
    @Mapping(target = "code", source = "confirmCodeRequest.verificationCode")
    @Mapping(target = "applicationId", source = "applicationId")
    VerificationCodeRequest toVerificationCodeRequest(ConfirmCodeRequest confirmCodeRequest, UUID applicationId);

}
