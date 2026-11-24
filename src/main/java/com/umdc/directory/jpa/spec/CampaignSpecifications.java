package com.umdc.directory.jpa.spec;

import com.umdc.directory.constant.CampaignStatus;
import com.umdc.directory.jpa.entity.CampaignEntity;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class CampaignSpecifications {

    private static final String ACTIVE = "active";
    private static final String START_DATE = "startDate";
    private static final String END_DATE = "endDate";

    private CampaignSpecifications() {}

    public static Specification<CampaignEntity> byFilters(String name,
                                                          UUID categoryId,
                                                          UUID businessId,
                                                          CampaignCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            addNamePredicate(predicates, root, cb, name);
            addCategoryPredicate(predicates, root, categoryId);
            addBusinessPredicate(predicates, root, businessId);
            addCriteriaPredicates(predicates, root, cb, criteria);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addNamePredicate(List<Predicate> predicates,
                                         Root<CampaignEntity> root,
                                         CriteriaBuilder cb,
                                         String name) {
        if (name != null && !name.isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("title")), "%" + name.toLowerCase(Locale.ROOT) + "%"));
        }
    }

    private static void addCategoryPredicate(List<Predicate> predicates,
                                             Root<CampaignEntity> root,
                                             UUID categoryId) {
        if (categoryId != null) {
            predicates.add(root.get("categoryFk").get("id").in(categoryId));
        }
    }

    private static void addBusinessPredicate(List<Predicate> predicates,
                                             Root<CampaignEntity> root,
                                             UUID businessId) {
        if (businessId != null) {
            predicates.add(root.get("businessFk").get("id").in(businessId));
        }
    }

    private static void addCriteriaPredicates(List<Predicate> predicates,
                                              Root<CampaignEntity> root,
                                              CriteriaBuilder cb,
                                              CampaignCriteria criteria) {
        if (criteria == null) return;
        addActivePredicate(predicates, root, cb, criteria.getActive());
        addDatePredicates(predicates, root, cb, criteria);
    }

    private static void addActivePredicate(List<Predicate> predicates,
                                           Root<CampaignEntity> root,
                                           CriteriaBuilder cb,
                                           Boolean active) {
        if (active != null) {
            predicates.add(cb.equal(root.get(ACTIVE), active));
        }
    }

    private static void addDatePredicates(List<Predicate> predicates,
                                          Root<CampaignEntity> root,
                                          CriteriaBuilder cb,
                                          CampaignCriteria criteria) {
        if (criteria.getStartFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get(START_DATE), criteria.getStartFrom()));
        }
        if (criteria.getStartTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get(START_DATE), criteria.getStartTo()));
        }
        if (criteria.getEndFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get(END_DATE), criteria.getEndFrom()));
        }
        if (criteria.getEndTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get(END_DATE), criteria.getEndTo()));
        }
    }

    /// Campaigns whose computed lifecycle status is `status` at `now` (same rules as {@link CampaignStatus#compute}).
    public static Specification<CampaignEntity> byStatus(CampaignStatus status, LocalDateTime now) {
        return (root, query, cb) -> statusPredicate(root, cb, status, now);
    }

    /// Campaigns of the given type (exact, case-insensitive).
    public static Specification<CampaignEntity> byType(String type) {
        return (root, query, cb) -> cb.equal(cb.upper(root.get("type")), type.toUpperCase(Locale.ROOT));
    }

    /// Visibility rule: a campaign is visible when it is scheduled or active (public), or when `userId` owns its
    /// business. A null `userId` sees public campaigns only.
    public static Specification<CampaignEntity> visibleTo(UUID userId, LocalDateTime now) {
        return (root, query, cb) -> {
            Predicate publicOnes = cb.and(notDraft(root, cb),
                    cb.greaterThanOrEqualTo(root.get(END_DATE), now),
                    cb.isTrue(root.get(ACTIVE)));
            if (userId == null) {
                return publicOnes;
            }
            Predicate own = cb.equal(root.get("businessFk").get("userFk"), userId);
            return cb.or(own, publicOnes);
        };
    }

    private static Predicate notDraft(Root<CampaignEntity> root, CriteriaBuilder cb) {
        return cb.or(cb.isNull(root.get("status")), cb.notEqual(root.get("status"), CampaignStatus.STORED_DRAFT));
    }

    private static Predicate statusPredicate(Root<CampaignEntity> root, CriteriaBuilder cb, CampaignStatus status,
                                             LocalDateTime now) {
        if (status == CampaignStatus.DRAFT) {
            return cb.equal(root.get("status"), CampaignStatus.STORED_DRAFT);
        }
        Predicate notDraft = notDraft(root, cb);
        Predicate expired = cb.lessThan(root.get(END_DATE), now);
        if (status == CampaignStatus.EXPIRED) {
            return cb.and(notDraft, expired);
        }
        Predicate running = cb.and(notDraft, cb.not(expired));
        if (status == CampaignStatus.PAUSED) {
            return cb.and(running, cb.isFalse(root.get(ACTIVE)));
        }
        Predicate live = cb.and(running, cb.isTrue(root.get(ACTIVE)));
        if (status == CampaignStatus.SCHEDULED) {
            return cb.and(live, cb.greaterThan(root.get(START_DATE), now));
        }
        return cb.and(live, cb.lessThanOrEqualTo(root.get(START_DATE), now));
    }
}
