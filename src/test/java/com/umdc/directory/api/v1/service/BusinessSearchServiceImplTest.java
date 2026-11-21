package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.CityTO;
import com.umdc.directory.api.v1.to.LocationTO;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.CategoryEntity;
import com.umdc.directory.jpa.entity.LocationEntity;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.LocationRepository;
import com.umdc.directory.mapper.LocationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessSearchServiceImplTest {

    @Mock
    private BusinessRepository businessRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private LocationMapper locationMapper;

    private BusinessSearchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BusinessSearchServiceImpl(businessRepository, locationRepository, locationMapper);
    }

    private static BusinessEntity business(String name) {
        var category = new CategoryEntity();
        category.setId(UUID.randomUUID());
        category.setName("Food");
        var b = new BusinessEntity();
        b.setId(UUID.randomUUID());
        b.setName(name);
        b.setDescription("d");
        b.setCategoryFk(category);
        b.setVerified(true);
        b.setProfileImageRef("https://img/x.png");
        return b;
    }

    private static LocationEntity location(BusinessEntity business) {
        var l = new LocationEntity();
        l.setBusiness(business);
        return l;
    }

    @Test
    void invalidPaginationIsA400WithoutTouchingTheDatabase() {
        for (int[] args : new int[][]{{0, 10}, {-1, 10}, {1, 0}, {1, 101}}) {
            var response = service.search(null, null, null, null, args[0], args[1]);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals(DirectoryAppConstants.SEARCH_INVALID_PAGINATION, response.getHeaders().getFirst(DirectoryAppConstants.MESSAGE_ERROR_HEADER));
        }
        verifyNoInteractions(businessRepository, locationRepository);
    }

    @Test
    void defaultsToTheFirstPageOfTenAndOrdersByNameThenId() {
        when(businessRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        var response = service.search(null, null, null, null, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        var captor = ArgumentCaptor.forClass(Pageable.class);
        verify(businessRepository).findAll(any(Specification.class), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
        assertEquals("name: ASC,id: ASC", captor.getValue().getSort().toString());
        assertEquals(0, response.getBody().totalCount());
        assertTrue(response.getBody().data().isEmpty());
        verify(locationRepository, never()).findByBusinessIdIn(any());
    }

    @Test
    void returnsTheBusinessesWithTheirLocationsAndThePageMetadata() {
        var a = business("A");
        var b = business("B");
        var l1 = location(a);
        var l2 = location(a);
        var cityTo = new CityTO(UUID.randomUUID(), "Toronto", "Ontario", "CA");
        var to1 = new LocationTO(UUID.randomUUID(), a.getId(), cityTo, null, "1", null, null, null, null, true, null, null);
        var to2 = new LocationTO(UUID.randomUUID(), a.getId(), cityTo, null, "2", null, null, null, null, false, null, null);
        when(businessRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(a, b), org.springframework.data.domain.PageRequest.of(1, 2), 5));
        when(locationRepository.findByBusinessIdIn(List.of(a.getId(), b.getId()))).thenReturn(List.of(l1, l2));
        when(locationMapper.toTO(l1)).thenReturn(to1);
        when(locationMapper.toTO(l2)).thenReturn(to2);

        var response = service.search(UUID.randomUUID(), "Toronto", UUID.randomUUID(), "pan", 2, 2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        var body = response.getBody();
        assertEquals(5, body.totalCount());
        assertEquals(2, body.page());
        assertEquals(2, body.perPage());
        assertEquals(3, body.totalPages());
        var items = List.copyOf(body.data());
        assertEquals(List.of(to1, to2), items.get(0).locations());
        assertEquals(List.of(), items.get(1).locations());
        assertEquals("A", items.get(0).name());
        assertEquals("Food", items.get(0).categoryName());
        assertTrue(items.get(0).verified());
        assertEquals("https://img/x.png", items.get(0).profileImageRef());
    }
}
