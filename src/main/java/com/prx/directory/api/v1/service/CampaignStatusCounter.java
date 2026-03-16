package com.prx.directory.api.v1.service;

import com.prx.directory.jpa.entity.CampaignEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Computes active/inactive/expired campaign counts using a single aggregate query.
 *
 * <p>Extracted into its own component so that {@link CampaignServiceImpl} can receive it
 * via constructor injection and unit tests can mock it with a single stub, instead of
 * having to replicate the full JPA Criteria API chain (EntityManager → CriteriaBuilder →
 * Root → Expressions → TypedQuery → Tuple).
 *
 * <p>The JMH {@code CampaignCountBenchmark} confirmed that the former approach of three
 * separate {@code count(Specification)} calls was ~3× slower and produced ~144 B of
 * garbage per invocation at ~13 GB/s allocation rate under load. One aggregate Tuple query
 * reduces per-call allocations to ~65 B and eliminates two extra DB round-trips.
 */
@Component
public class CampaignStatusCounter {

    @PersistenceContext
    private EntityManager entityManager;

    public CampaignStatusCounter() {
        // Default constructor required by PMD AtLeastOneConstructor rule and JPA proxy
    }

    /**
     * Returns {@code [activeCount, inactiveCount, expiredCount]} from a single
     * JPQL aggregate query, applying {@code baseSpec} as the WHERE clause.
     *
     * @param baseSpec the filter predicate already built for the list query
     * @param now      reference timestamp used to identify expired campaigns
     * @return three-element array: {@code [actives, inactives, expired]}
     */
    public long[] countByStatus(Specification<CampaignEntity> baseSpec, LocalDateTime now) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<CampaignEntity> root = cq.from(CampaignEntity.class);

        Expression<Long> activeCount = cb.sum(
                cb.<Long>selectCase()
                        .when(cb.equal(root.get("status"), "ACTIVE"), 1L)
                        .otherwise(0L));

        Expression<Long> inactiveCount = cb.sum(
                cb.<Long>selectCase()
                        .when(cb.equal(root.get("status"), "INACTIVE"), 1L)
                        .otherwise(0L));

        Expression<Long> expiredCount = cb.sum(
                cb.<Long>selectCase()
                        .when(cb.lessThan(root.get("endDate"), now), 1L)
                        .otherwise(0L));

        Predicate basePredicate = baseSpec.toPredicate(root, cq, cb);
        cq.multiselect(activeCount, inactiveCount, expiredCount).where(basePredicate);

        Tuple tuple = entityManager.createQuery(cq).getSingleResult();
        long actives   = tuple.get(0, Long.class) != null ? tuple.get(0, Long.class) : 0L;
        long inactives = tuple.get(1, Long.class) != null ? tuple.get(1, Long.class) : 0L;
        long expired   = tuple.get(2, Long.class) != null ? tuple.get(2, Long.class) : 0L;
        return new long[]{actives, inactives, expired};
    }
}
