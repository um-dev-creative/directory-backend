package com.umdev.directory.api.v1.service;

import com.umdev.directory.api.v1.to.DigitalContactTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public interface DigitalContactService {

    /**
     * Retrieves a paginated list of DigitalContactTO objects.
     *
     * @param pageable pagination information
     * @return a page of DigitalContactTO
     */
    default Page<DigitalContactTO> getAllDigitalContacts(Pageable pageable) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Retrieves a DigitalContactTO by its unique identifier.
     *
     * @param id the unique identifier of the digital contact
     * @return an Optional containing the DigitalContactTO if found, or empty if not found
     */
    default Optional<DigitalContactTO> getDigitalContactById(UUID id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Retrieves a list of DigitalContactTO objects associated with the specified business ID.
     *
     * @param businessId the unique identifier of the business
     * @return a list of DigitalContactTO objects associated with the given business ID
     */
    default List <DigitalContactTO> getDigitalContactsByBusinessId(UUID businessId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
