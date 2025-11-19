package com.prx.directory.jpa.spec;

import com.prx.directory.jpa.entity.CampaignEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CampaignSpecificationsTest {

    @Test
    void byFilters_noFilters_callsAndOnly() {
        Specification<CampaignEntity> spec = CampaignSpecifications.byFilters(null, null, null, null);

        CriteriaBuilder cb = Mockito.mock(CriteriaBuilder.class);
        Root<CampaignEntity> root = Mockito.mock(Root.class);
        CriteriaQuery<?> query = Mockito.mock(CriteriaQuery.class);

        // stub and to avoid null
        when(cb.and(any(Predicate[].class))).thenReturn(Mockito.mock(Predicate.class));

        Predicate p = spec.toPredicate(root, query, cb);
        assertNotNull(p);

        // no title/category/business/criteria predicates should be invoked
        verify(cb, never()).like(any(), anyString());
        verify(cb, never()).equal(any(), anyBoolean());
        verify(cb, never()).greaterThanOrEqualTo(any(), any(Instant.class));
        verify(cb, never()).lessThanOrEqualTo(any(), any(Instant.class));
    }

    @Test
    void byFilters_nameProvided_invokesLike() {
        String name = "CampaignName";
        Specification<CampaignEntity> spec = CampaignSpecifications.byFilters(name, null, null, null);

        CriteriaBuilder cb = Mockito.mock(CriteriaBuilder.class);
        Root<CampaignEntity> root = Mockito.mock(Root.class);
        CriteriaQuery<?> query = Mockito.mock(CriteriaQuery.class);

        Path<Object> namePath = Mockito.mock(Path.class);
        when(root.get("title")).thenReturn(namePath);
        Expression lowered = Mockito.mock(Expression.class);
        when(cb.lower(any(Expression.class))).thenReturn(lowered);
        when(cb.like(any(Expression.class), anyString())).thenReturn(Mockito.mock(Predicate.class));
        when(cb.and(any(Predicate[].class))).thenReturn(Mockito.mock(Predicate.class));

        Predicate p = spec.toPredicate(root, query, cb);
        assertNotNull(p);

        verify(cb, times(1)).like(any(Expression.class), contains(name.toLowerCase()));
    }

    @Test
    void byFilters_nameBlank_noLike() {
        Specification<CampaignEntity> spec = CampaignSpecifications.byFilters("  ", null, null, null);

        CriteriaBuilder cb = Mockito.mock(CriteriaBuilder.class);
        Root<CampaignEntity> root = Mockito.mock(Root.class);
        CriteriaQuery<?> query = Mockito.mock(CriteriaQuery.class);

        when(cb.and(any(Predicate[].class))).thenReturn(Mockito.mock(Predicate.class));

        Predicate p = spec.toPredicate(root, query, cb);
        assertNotNull(p);

        verify(cb, never()).like(any(), anyString());
    }

    @Test
    void byFilters_categoryProvided_invokesIn() {
        UUID categoryId = UUID.randomUUID();
        Specification<CampaignEntity> spec = CampaignSpecifications.byFilters(null, categoryId, null, null);

        CriteriaBuilder cb = Mockito.mock(CriteriaBuilder.class);
        Root<CampaignEntity> root = Mockito.mock(Root.class);
        CriteriaQuery<?> query = Mockito.mock(CriteriaQuery.class);

        Path<Object> categoryFk = Mockito.mock(Path.class);
        Path<Object> categoryIdPath = Mockito.mock(Path.class);
        when(root.get("categoryFk")).thenReturn(categoryFk);
        when(categoryFk.get("id")).thenReturn(categoryIdPath);
        when(categoryIdPath.in(any(UUID.class))).thenReturn(Mockito.mock(Predicate.class));
        when(cb.and(any(Predicate[].class))).thenReturn(Mockito.mock(Predicate.class));

        Predicate p = spec.toPredicate(root, query, cb);
        assertNotNull(p);

        verify(categoryIdPath, times(1)).in(eq(categoryId));
    }

    @Test
    void byFilters_businessProvided_invokesIn() {
        UUID businessId = UUID.randomUUID();
        Specification<CampaignEntity> spec = CampaignSpecifications.byFilters(null, null, businessId, null);

        CriteriaBuilder cb = Mockito.mock(CriteriaBuilder.class);
        Root<CampaignEntity> root = Mockito.mock(Root.class);
        CriteriaQuery<?> query = Mockito.mock(CriteriaQuery.class);

        Path<Object> businessFk = Mockito.mock(Path.class);
        Path<Object> businessIdPath = Mockito.mock(Path.class);
        when(root.get("businessFk")).thenReturn(businessFk);
        when(businessFk.get("id")).thenReturn(businessIdPath);
        when(businessIdPath.in(any(UUID.class))).thenReturn(Mockito.mock(Predicate.class));
        when(cb.and(any(Predicate[].class))).thenReturn(Mockito.mock(Predicate.class));

        Predicate p = spec.toPredicate(root, query, cb);
        assertNotNull(p);

        verify(businessIdPath, times(1)).in(eq(businessId));
    }

    @Test
    void byFilters_withCriteriaActive_invokesEqualTrue() {
        CampaignCriteria criteria = CampaignCriteria.of(Boolean.TRUE, null, null, null, null);
        Specification<CampaignEntity> spec = CampaignSpecifications.byFilters(null, null, null, criteria);

        CriteriaBuilder cb = Mockito.mock(CriteriaBuilder.class);
        Root<CampaignEntity> root = Mockito.mock(Root.class);
        CriteriaQuery<?> query = Mockito.mock(CriteriaQuery.class);

        Path<Boolean> activePath = Mockito.mock(Path.class);
        when(root.get("active")).thenReturn((Path) activePath);
        when(cb.equal(any(), anyBoolean())).thenReturn(Mockito.mock(Predicate.class));
        when(cb.and(any(Predicate[].class))).thenReturn(Mockito.mock(Predicate.class));

        Predicate p = spec.toPredicate(root, query, cb);
        assertNotNull(p);

        verify(cb, times(1)).equal(any(), eq(Boolean.TRUE));
    }

    @Test
    void byFilters_withCriteriaActive_invokesEqualFalse() {
        CampaignCriteria criteria = CampaignCriteria.of(Boolean.FALSE, null, null, null, null);
        Specification<CampaignEntity> spec = CampaignSpecifications.byFilters(null, null, null, criteria);

        CriteriaBuilder cb = Mockito.mock(CriteriaBuilder.class);
        Root<CampaignEntity> root = Mockito.mock(Root.class);
        CriteriaQuery<?> query = Mockito.mock(CriteriaQuery.class);

        Path<Boolean> activePath = Mockito.mock(Path.class);
        when(root.get("active")).thenReturn((Path) activePath);
        when(cb.equal(any(), anyBoolean())).thenReturn(Mockito.mock(Predicate.class));
        when(cb.and(any(Predicate[].class))).thenReturn(Mockito.mock(Predicate.class));

        Predicate p = spec.toPredicate(root, query, cb);
        assertNotNull(p);

        verify(cb, times(1)).equal(any(), eq(Boolean.FALSE));
    }

    @Test
    void byFilters_withDateRanges_invokesDatePredicates() {
        LocalDateTime startFrom = LocalDateTime.parse("2025-11-01T00:00:00");
        LocalDateTime startTo = LocalDateTime.parse("2025-11-05T00:00:00");
        LocalDateTime endFrom = LocalDateTime.parse("2025-11-10T00:00:00");
        LocalDateTime endTo = LocalDateTime.parse("2025-11-15T00:00:00");

        CampaignCriteria criteria = CampaignCriteria.of(null, startFrom, startTo, endFrom, endTo);
        Specification<CampaignEntity> spec = CampaignSpecifications.byFilters(null, null, null, criteria);

        CriteriaBuilder cb = Mockito.mock(CriteriaBuilder.class);
        Root<CampaignEntity> root = Mockito.mock(Root.class);
        CriteriaQuery<?> query = Mockito.mock(CriteriaQuery.class);

        Path<Object> startPath = Mockito.mock(Path.class);
        Path<Object> endPath = Mockito.mock(Path.class);
        when(root.get("startDate")).thenReturn((Path) startPath);
        when(root.get("endDate")).thenReturn((Path) endPath);

        when(cb.greaterThanOrEqualTo(any(), any(LocalDateTime.class))).thenReturn(Mockito.mock(Predicate.class));
        when(cb.lessThanOrEqualTo(any(), any(LocalDateTime.class))).thenReturn(Mockito.mock(Predicate.class));
        when(cb.and(any(Predicate[].class))).thenReturn(Mockito.mock(Predicate.class));

        Predicate p = spec.toPredicate(root, query, cb);
        assertNotNull(p);

        // greaterThanOrEqualTo called twice (startFrom and endFrom)
        verify(cb, times(2)).greaterThanOrEqualTo(any(), any(LocalDateTime.class));
        // lessThanOrEqualTo called twice (startTo and endTo)
        verify(cb, times(2)).lessThanOrEqualTo(any(), any(LocalDateTime.class));
    }

    @Test
    void byFilters_startFromOnly_invokesSingleGreater() {
        LocalDateTime startFrom = LocalDateTime.parse("2025-11-01T00:00:00");
        CampaignCriteria criteria = CampaignCriteria.of(null, startFrom, null, null, null);
        Specification<CampaignEntity> spec = CampaignSpecifications.byFilters(null, null, null, criteria);

        CriteriaBuilder cb = Mockito.mock(CriteriaBuilder.class);
        Root<CampaignEntity> root = Mockito.mock(Root.class);
        CriteriaQuery<?> query = Mockito.mock(CriteriaQuery.class);

        when(root.get("startDate")).thenReturn((Path) Mockito.mock(Path.class));
        when(cb.greaterThanOrEqualTo(any(), any(Instant.class))).thenReturn(Mockito.mock(Predicate.class));
        when(cb.and(any(Predicate[].class))).thenReturn(Mockito.mock(Predicate.class));

        Predicate p = spec.toPredicate(root, query, cb);
        assertNotNull(p);
        verify(cb, times(1)).greaterThanOrEqualTo(any(), eq(startFrom));
    }

    @Test
    void byFilters_startToOnly_invokesSingleLess() {
        LocalDateTime startTo = LocalDateTime.parse("2025-11-05T00:00:00");
        CampaignCriteria criteria = CampaignCriteria.of(null, null, startTo, null, null);
        Specification<CampaignEntity> spec = CampaignSpecifications.byFilters(null, null, null, criteria);

        CriteriaBuilder cb = Mockito.mock(CriteriaBuilder.class);
        Root<CampaignEntity> root = Mockito.mock(Root.class);
        CriteriaQuery<?> query = Mockito.mock(CriteriaQuery.class);

        when(root.get("startDate")).thenReturn((Path) Mockito.mock(Path.class));
        when(cb.lessThanOrEqualTo(any(), any(Instant.class))).thenReturn(Mockito.mock(Predicate.class));
        when(cb.and(any(Predicate[].class))).thenReturn(Mockito.mock(Predicate.class));

        Predicate p = spec.toPredicate(root, query, cb);
        assertNotNull(p);
        verify(cb, times(1)).lessThanOrEqualTo(any(), eq(startTo));
    }

    @Test
    void byFilters_endFromOnly_invokesSingleGreater() {
        LocalDateTime endFrom = LocalDateTime.parse("2025-11-10T00:00:00");
        CampaignCriteria criteria = CampaignCriteria.of(null, null, null, endFrom, null);
        Specification<CampaignEntity> spec = CampaignSpecifications.byFilters(null, null, null, criteria);

        CriteriaBuilder cb = Mockito.mock(CriteriaBuilder.class);
        Root<CampaignEntity> root = Mockito.mock(Root.class);
        CriteriaQuery<?> query = Mockito.mock(CriteriaQuery.class);

        when(root.get("endDate")).thenReturn((Path) Mockito.mock(Path.class));
        when(cb.greaterThanOrEqualTo(any(), any(LocalDateTime.class))).thenReturn(Mockito.mock(Predicate.class));
        when(cb.and(any(Predicate[].class))).thenReturn(Mockito.mock(Predicate.class));

        Predicate p = spec.toPredicate(root, query, cb);
        assertNotNull(p);
        verify(cb, times(1)).greaterThanOrEqualTo(any(), eq(endFrom));
    }

    @Test
    void byFilters_endToOnly_invokesSingleLess() {
        LocalDateTime endTo = LocalDateTime.parse("2025-11-15T00:00:00");
        CampaignCriteria criteria = CampaignCriteria.of(null, null, null, null, endTo);
        Specification<CampaignEntity> spec = CampaignSpecifications.byFilters(null, null, null, criteria);

        CriteriaBuilder cb = Mockito.mock(CriteriaBuilder.class);
        Root<CampaignEntity> root = Mockito.mock(Root.class);
        CriteriaQuery<?> query = Mockito.mock(CriteriaQuery.class);

        when(root.get("endDate")).thenReturn((Path) Mockito.mock(Path.class));
        when(cb.lessThanOrEqualTo(any(), any(Instant.class))).thenReturn(Mockito.mock(Predicate.class));
        when(cb.and(any(Predicate[].class))).thenReturn(Mockito.mock(Predicate.class));

        Predicate p = spec.toPredicate(root, query, cb);
        assertNotNull(p);
        verify(cb, times(1)).lessThanOrEqualTo(any(), eq(endTo));
    }
}
