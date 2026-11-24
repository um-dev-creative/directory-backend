package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.CityTO;
import com.umdc.directory.api.v1.to.LocationCreateRequest;
import com.umdc.directory.api.v1.to.LocationTO;
import com.umdc.directory.api.v1.to.LocationUpdateRequest;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.CityEntity;
import com.umdc.directory.jpa.entity.LocationEntity;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.CityRepository;
import com.umdc.directory.jpa.repository.LocationRepository;
import com.umdc.directory.mapper.LocationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceImplTest {

    @Mock
    private LocationRepository locationRepository;
    @Mock
    private BusinessRepository businessRepository;
    @Mock
    private CityRepository cityRepository;
    @Mock
    private LocationMapper locationMapper;

    private LocationServiceImpl service;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID businessId = UUID.randomUUID();
    private final UUID cityId = UUID.randomUUID();
    private BusinessEntity business;
    private CityEntity city;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        service = new LocationServiceImpl(locationRepository, businessRepository, cityRepository, locationMapper);
        business = new BusinessEntity();
        business.setId(businessId);
        business.setUserFk(ownerId);
        city = new CityEntity();
        city.setId(cityId);
        city.setName("Toronto");
        city.setProvince("Ontario");
        city.setCountry("CA");
        ownerToken = token(ownerId);
        lenient().when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));
        lenient().when(locationMapper.toTO(any(LocationEntity.class))).thenAnswer(invocation -> {
            LocationEntity l = invocation.getArgument(0);
            return new LocationTO(l.getId(), businessId, new CityTO(cityId, "Toronto", "Ontario", "CA"), l.getName(), l.getAddressLine(),
                    l.getPostalCode(), l.getPhone(), l.getLatitude(), l.getLongitude(), l.isMain(), l.getCreatedDate(), l.getLastUpdate());
        });
        lenient().when(locationRepository.save(any(LocationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(locationRepository.saveAndFlush(any(LocationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /// A session token whose payload carries the `uid` (the signature is not checked by `JwtUtil`).
    private static String token(UUID uid) {
        var encoder = Base64.getUrlEncoder().withoutPadding();
        String payload = encoder.encodeToString(("{\"uid\":\"" + uid + "\"}").getBytes(StandardCharsets.UTF_8));
        return "header." + payload + ".signature";
    }

    private LocationEntity existing(boolean main) {
        var l = new LocationEntity();
        l.setId(UUID.randomUUID());
        l.setBusiness(business);
        l.setCity(city);
        l.setAddressLine("1 Main St");
        l.setMain(main);
        l.setCreatedDate(LocalDateTime.now());
        l.setLastUpdate(LocalDateTime.now());
        return l;
    }

    private static LocationCreateRequest create(UUID cityId, String address) {
        return new LocationCreateRequest(cityId, "Sucursal", address, " M5V 2T6 ", "416-555-0100", null, null, null);
    }

    private static LocationUpdateRequest update(String name, UUID cityId, String address, BigDecimal lat, BigDecimal lon, Boolean main) {
        return new LocationUpdateRequest(cityId, name, address, null, null, lat, lon, main);
    }

    // ---- list

    @Test
    void listAnswers404WhenTheBusinessDoesNotExist() {
        when(businessRepository.existsById(businessId)).thenReturn(false);
        assertEquals(HttpStatus.NOT_FOUND, service.list(businessId).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, service.list(null).getStatusCode());
    }

    @Test
    void listReturnsTheLocationsOfTheBusiness() {
        when(businessRepository.existsById(businessId)).thenReturn(true);
        when(locationRepository.findByBusinessId(businessId)).thenReturn(List.of(existing(true), existing(false)));
        var response = service.list(businessId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    // ---- authorization (shared by create, update and delete)

    @Test
    void changesWithoutAUidInTheTokenAre401() {
        assertEquals(HttpStatus.UNAUTHORIZED, service.create(businessId, create(cityId, "x"), "garbage").getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, service.update(businessId, UUID.randomUUID(), update(null, null, null, null, null, null), null).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, service.delete(businessId, UUID.randomUUID(), "").getStatusCode());
    }

    @Test
    void changesOnAMissingBusinessAre404() {
        var other = UUID.randomUUID();
        when(businessRepository.findById(other)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, service.create(other, create(cityId, "x"), ownerToken).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, service.create(null, create(cityId, "x"), ownerToken).getStatusCode());
    }

    @Test
    void changesByAnotherUserAre403() {
        var stranger = token(UUID.randomUUID());
        assertEquals(HttpStatus.FORBIDDEN, service.create(businessId, create(cityId, "x"), stranger).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, service.delete(businessId, UUID.randomUUID(), stranger).getStatusCode());
        verify(locationRepository, never()).save(any());
    }

    @Test
    void aBusinessWithoutAnOwnerCannotBeChanged() {
        business.setUserFk(null);
        assertEquals(HttpStatus.FORBIDDEN, service.create(businessId, create(cityId, "x"), ownerToken).getStatusCode());
    }

    // ---- create

    @Test
    void createRejectsAMissingBodyCityOrAddress() {
        assertEquals(HttpStatus.BAD_REQUEST, service.create(businessId, null, ownerToken).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.create(businessId, create(null, "x"), ownerToken).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.create(businessId, create(cityId, null), ownerToken).getStatusCode());
        var blank = service.create(businessId, create(cityId, "   "), ownerToken);
        assertEquals(HttpStatus.BAD_REQUEST, blank.getStatusCode());
        assertEquals(DirectoryAppConstants.LOCATION_ADDRESS_REQUIRED, blank.getHeaders().getFirst(DirectoryAppConstants.MESSAGE_ERROR_HEADER));
    }

    @Test
    void createRejectsTooLongFields() {
        var response = service.create(businessId, create(cityId, "a".repeat(201)), ownerToken);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(DirectoryAppConstants.LOCATION_FIELD_TOO_LONG, response.getHeaders().getFirst(DirectoryAppConstants.MESSAGE_ERROR_HEADER));
        var longName = new LocationCreateRequest(cityId, "n".repeat(121), "x", null, null, null, null, null);
        assertEquals(HttpStatus.BAD_REQUEST, service.create(businessId, longName, ownerToken).getStatusCode());
        var longPostal = new LocationCreateRequest(cityId, null, "x", "p".repeat(11), null, null, null, null);
        assertEquals(HttpStatus.BAD_REQUEST, service.create(businessId, longPostal, ownerToken).getStatusCode());
        var longPhone = new LocationCreateRequest(cityId, null, "x", null, "1".repeat(31), null, null, null);
        assertEquals(HttpStatus.BAD_REQUEST, service.create(businessId, longPhone, ownerToken).getStatusCode());
    }

    @Test
    void createRejectsHalfOrOutOfRangeCoordinates() {
        var onlyLat = new LocationCreateRequest(cityId, null, "x", null, null, BigDecimal.ONE, null, null);
        var r1 = service.create(businessId, onlyLat, ownerToken);
        assertEquals(DirectoryAppConstants.LOCATION_COORDINATES_PAIR, r1.getHeaders().getFirst(DirectoryAppConstants.MESSAGE_ERROR_HEADER));
        var onlyLon = new LocationCreateRequest(cityId, null, "x", null, null, null, BigDecimal.ONE, null);
        assertEquals(HttpStatus.BAD_REQUEST, service.create(businessId, onlyLon, ownerToken).getStatusCode());
        var badLat = new LocationCreateRequest(cityId, null, "x", null, null, BigDecimal.valueOf(90.1), BigDecimal.ZERO, null);
        var r2 = service.create(businessId, badLat, ownerToken);
        assertEquals(DirectoryAppConstants.LOCATION_COORDINATES_RANGE, r2.getHeaders().getFirst(DirectoryAppConstants.MESSAGE_ERROR_HEADER));
        var badLon = new LocationCreateRequest(cityId, null, "x", null, null, BigDecimal.ZERO, BigDecimal.valueOf(-180.5), null);
        assertEquals(HttpStatus.BAD_REQUEST, service.create(businessId, badLon, ownerToken).getStatusCode());
    }

    @Test
    void createRejectsAnUnknownCity() {
        when(cityRepository.findById(cityId)).thenReturn(Optional.empty());
        var response = service.create(businessId, create(cityId, "x"), ownerToken);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(DirectoryAppConstants.CITY_NOT_FOUND, response.getHeaders().getFirst(DirectoryAppConstants.MESSAGE_ERROR_HEADER));
    }

    @Test
    void theFirstLocationIsAlwaysTheMainOneAndFieldsAreTrimmed() {
        when(cityRepository.findById(cityId)).thenReturn(Optional.of(city));
        when(locationRepository.countByBusinessId(businessId)).thenReturn(0L);
        var response = service.create(businessId, new LocationCreateRequest(cityId, "  ", " 1 Main St ", " M5V 2T6 ", "", BigDecimal.valueOf(43.65), BigDecimal.valueOf(-79.38), false), ownerToken);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        var captor = ArgumentCaptor.forClass(LocationEntity.class);
        verify(locationRepository).save(captor.capture());
        var saved = captor.getValue();
        assertTrue(saved.isMain());
        assertEquals("1 Main St", saved.getAddressLine());
        assertEquals("M5V 2T6", saved.getPostalCode());
        assertEquals(null, saved.getName());
        assertEquals(null, saved.getPhone());
        assertEquals(business, saved.getBusiness());
        assertNotNull(saved.getCreatedDate());
        verify(locationRepository, never()).findMainByBusinessId(any());
    }

    @Test
    void aLaterLocationIsNotMainByDefault() {
        when(cityRepository.findById(cityId)).thenReturn(Optional.of(city));
        when(locationRepository.countByBusinessId(businessId)).thenReturn(1L);
        var response = service.create(businessId, create(cityId, "2 Side St"), ownerToken);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertFalse(response.getBody().main());
        verify(locationRepository, never()).saveAndFlush(any());
    }

    @Test
    void mainTrueOnALaterLocationReplacesTheCurrentMain() {
        var current = existing(true);
        when(cityRepository.findById(cityId)).thenReturn(Optional.of(city));
        when(locationRepository.countByBusinessId(businessId)).thenReturn(1L);
        when(locationRepository.findMainByBusinessId(businessId)).thenReturn(Optional.of(current));
        var response = service.create(businessId, new LocationCreateRequest(cityId, null, "2 Side St", null, null, null, null, true), ownerToken);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().main());
        assertFalse(current.isMain());
        verify(locationRepository).saveAndFlush(current);
    }

    // ---- update

    @Test
    void updateAnswers404ForAnUnknownLocationAndRejectsAMissingBody() {
        var locationId = UUID.randomUUID();
        when(locationRepository.findByIdAndBusinessId(locationId, businessId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, service.update(businessId, locationId, update("n", null, null, null, null, null), ownerToken).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, service.update(businessId, null, update("n", null, null, null, null, null), ownerToken).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.update(businessId, locationId, null, ownerToken).getStatusCode());
    }

    @Test
    void updateChangesOnlyTheFieldsThatAreSent() {
        var location = existing(false);
        location.setName("Old");
        location.setPhone("111");
        when(locationRepository.findByIdAndBusinessId(location.getId(), businessId)).thenReturn(Optional.of(location));
        var request = new LocationUpdateRequest(null, "New", null, "  ", "", BigDecimal.valueOf(45), BigDecimal.valueOf(-75), null);
        var response = service.update(businessId, location.getId(), request, ownerToken);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("New", location.getName());
        assertEquals("1 Main St", location.getAddressLine());
        assertEquals(null, location.getPostalCode());
        assertEquals(null, location.getPhone());
        assertEquals(BigDecimal.valueOf(45), location.getLatitude());
        assertEquals(BigDecimal.valueOf(-75), location.getLongitude());
    }

    @Test
    void updateRejectsABlankAddressAndInvalidFields() {
        var location = existing(false);
        when(locationRepository.findByIdAndBusinessId(location.getId(), businessId)).thenReturn(Optional.of(location));
        assertEquals(HttpStatus.BAD_REQUEST, service.update(businessId, location.getId(), update(null, null, "  ", null, null, null), ownerToken).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.update(businessId, location.getId(), update(null, null, null, BigDecimal.ONE, null, null), ownerToken).getStatusCode());
        assertEquals("1 Main St", location.getAddressLine());
    }

    @Test
    void updateChangesTheCityAndRejectsAnUnknownOne() {
        var location = existing(false);
        var otherCityId = UUID.randomUUID();
        var other = new CityEntity();
        other.setId(otherCityId);
        when(locationRepository.findByIdAndBusinessId(location.getId(), businessId)).thenReturn(Optional.of(location));
        when(cityRepository.findById(otherCityId)).thenReturn(Optional.of(other));
        assertEquals(HttpStatus.OK, service.update(businessId, location.getId(), update(null, otherCityId, null, null, null, null), ownerToken).getStatusCode());
        assertEquals(other, location.getCity());

        var missing = UUID.randomUUID();
        when(cityRepository.findById(missing)).thenReturn(Optional.empty());
        var response = service.update(businessId, location.getId(), update(null, missing, null, null, null, null), ownerToken);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(DirectoryAppConstants.CITY_NOT_FOUND, response.getHeaders().getFirst(DirectoryAppConstants.MESSAGE_ERROR_HEADER));
    }

    @Test
    void theMainLocationCannotBeUnsetButANonMainOneCanBeSentFalse() {
        var main = existing(true);
        var other = existing(false);
        when(locationRepository.findByIdAndBusinessId(main.getId(), businessId)).thenReturn(Optional.of(main));
        when(locationRepository.findByIdAndBusinessId(other.getId(), businessId)).thenReturn(Optional.of(other));
        var rejected = service.update(businessId, main.getId(), update(null, null, null, null, null, false), ownerToken);
        assertEquals(HttpStatus.BAD_REQUEST, rejected.getStatusCode());
        assertEquals(DirectoryAppConstants.LOCATION_MAIN_CANNOT_BE_UNSET, rejected.getHeaders().getFirst(DirectoryAppConstants.MESSAGE_ERROR_HEADER));
        assertTrue(main.isMain());
        assertEquals(HttpStatus.OK, service.update(businessId, other.getId(), update(null, null, null, null, null, false), ownerToken).getStatusCode());
        assertFalse(other.isMain());
    }

    @Test
    void makingALocationMainDemotesTheCurrentOne() {
        var current = existing(true);
        var other = existing(false);
        when(locationRepository.findByIdAndBusinessId(other.getId(), businessId)).thenReturn(Optional.of(other));
        when(locationRepository.findMainByBusinessId(businessId)).thenReturn(Optional.of(current));
        var response = service.update(businessId, other.getId(), update(null, null, null, null, null, true), ownerToken);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(other.isMain());
        assertFalse(current.isMain());
        verify(locationRepository).saveAndFlush(current);
    }

    @Test
    void sendingMainTrueToTheMainLocationChangesNothingElse() {
        var main = existing(true);
        when(locationRepository.findByIdAndBusinessId(main.getId(), businessId)).thenReturn(Optional.of(main));
        assertEquals(HttpStatus.OK, service.update(businessId, main.getId(), update(null, null, null, null, null, true), ownerToken).getStatusCode());
        verify(locationRepository, never()).findMainByBusinessId(any());
    }

    // ---- delete

    @Test
    void deleteAnswers404ForAnUnknownLocation() {
        var locationId = UUID.randomUUID();
        when(locationRepository.findByIdAndBusinessId(locationId, businessId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, service.delete(businessId, locationId, ownerToken).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, service.delete(businessId, null, ownerToken).getStatusCode());
    }

    @Test
    void deletingANonMainLocationLeavesTheMainAlone() {
        var other = existing(false);
        when(locationRepository.findByIdAndBusinessId(other.getId(), businessId)).thenReturn(Optional.of(other));
        assertEquals(HttpStatus.NO_CONTENT, service.delete(businessId, other.getId(), ownerToken).getStatusCode());
        verify(locationRepository).delete(other);
        verify(locationRepository, never()).findByBusinessId(any());
    }

    @Test
    void deletingTheMainLocationPromotesTheOldestRemainingOne() {
        var main = existing(true);
        var next = existing(false);
        when(locationRepository.findByIdAndBusinessId(main.getId(), businessId)).thenReturn(Optional.of(main));
        when(locationRepository.findByBusinessId(businessId)).thenReturn(List.of(next));
        assertEquals(HttpStatus.NO_CONTENT, service.delete(businessId, main.getId(), ownerToken).getStatusCode());
        assertTrue(next.isMain());
        verify(locationRepository).flush();
        verify(locationRepository).save(next);
    }

    @Test
    void deletingTheOnlyLocationLeavesNoMain() {
        var main = existing(true);
        when(locationRepository.findByIdAndBusinessId(main.getId(), businessId)).thenReturn(Optional.of(main));
        when(locationRepository.findByBusinessId(businessId)).thenReturn(List.of());
        assertEquals(HttpStatus.NO_CONTENT, service.delete(businessId, main.getId(), ownerToken).getStatusCode());
        verify(locationRepository, never()).save(any());
    }
}
