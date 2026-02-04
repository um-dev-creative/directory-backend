package com.umdev.directory.jpa.spec;

import com.umdev.directory.jpa.entity.CampaignEntity;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class CampaignSpecifications {

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
            predicates.add(cb.equal(root.get("active"), active));
        }
    }

    private static void addDatePredicates(List<Predicate> predicates,
                                          Root<CampaignEntity> root,
                                          CriteriaBuilder cb,
                                          CampaignCriteria criteria) {
        if (criteria.getStartFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), criteria.getStartFrom()));
        }
        if (criteria.getStartTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), criteria.getStartTo()));
        }
        if (criteria.getEndFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), criteria.getEndFrom()));
        }
        if (criteria.getEndTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), criteria.getEndTo()));
        }
    }
}
