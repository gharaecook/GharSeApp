package com.gharse.cook;

import com.gharse.availability.Batch;
import com.gharse.availability.BatchStatus;
import com.gharse.cuisine.Cuisine;
import com.gharse.menu.Dish;
import com.gharse.user.UserStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Discovery search on plain PostgreSQL. Each filter is an independent predicate so new ones
 * (or a swap to a search engine later) stay local to this class.
 */
public final class CookSpecifications {

    private CookSpecifications() {}

    public static Specification<Cook> discover(CookSearch s, LocalDate today, Instant now) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            var user = root.join("user");
            ps.add(cb.equal(root.get("approvalStatus"), ApprovalStatus.APPROVED));
            ps.add(cb.equal(user.get("status"), UserStatus.ACTIVE));

            if (has(s.q())) {
                String like = "%" + escape(s.q().trim().toLowerCase()) + "%";
                Subquery<Integer> cuisineSub = query.subquery(Integer.class);
                Root<Cook> cr = cuisineSub.from(Cook.class);
                var cj = cr.<Cook, Cuisine>join("cuisines");
                cuisineSub.select(cb.literal(1)).where(cb.equal(cr.get("id"), root.get("id")),
                        cb.like(cb.lower(cj.get("name")), like, '\\'));
                Subquery<Integer> dishSub = query.subquery(Integer.class);
                Root<Dish> d = dishSub.from(Dish.class);
                dishSub.select(cb.literal(1)).where(cb.equal(d.get("cook"), root), cb.isTrue(d.get("active")),
                        cb.like(cb.lower(d.get("name")), like, '\\'));
                ps.add(cb.or(
                        cb.like(cb.lower(user.get("name")), like, '\\'),
                        cb.like(cb.lower(root.get("locality")), like, '\\'),
                        cb.like(cb.lower(root.get("headline")), like, '\\'),
                        cb.exists(cuisineSub),
                        cb.exists(dishSub)));
            }
            if (has(s.cuisine())) {
                Subquery<Integer> sub = query.subquery(Integer.class);
                Root<Cook> cr = sub.from(Cook.class);
                var cj = cr.<Cook, Cuisine>join("cuisines");
                sub.select(cb.literal(1)).where(cb.equal(cr.get("id"), root.get("id")),
                        cb.equal(cb.lower(cj.get("name")), s.cuisine().trim().toLowerCase()));
                ps.add(cb.exists(sub));
            }
            if (s.category() != null || s.vegetarian() != null) {
                Subquery<Integer> sub = query.subquery(Integer.class);
                Root<Dish> d = sub.from(Dish.class);
                List<Predicate> dp = new ArrayList<>(List.of(cb.equal(d.get("cook"), root), cb.isTrue(d.get("active"))));
                if (s.category() != null) dp.add(cb.equal(d.get("category"), s.category()));
                if (Boolean.TRUE.equals(s.vegetarian())) dp.add(cb.isTrue(d.get("vegetarian")));
                sub.select(cb.literal(1)).where(dp.toArray(Predicate[]::new));
                ps.add(cb.exists(sub));
            }
            if (s.minRating() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("rating"), s.minRating()));
            }
            if (has(s.locality())) {
                ps.add(cb.like(cb.lower(root.get("locality")), "%" + escape(s.locality().trim().toLowerCase()) + "%", '\\'));
            }
            if (Boolean.TRUE.equals(s.availableToday())) {
                Subquery<Integer> sub = query.subquery(Integer.class);
                Root<Batch> b = sub.from(Batch.class);
                sub.select(cb.literal(1)).where(cb.equal(b.get("cook"), root),
                        cb.equal(b.get("availableDate"), today),
                        cb.equal(b.get("status"), BatchStatus.OPEN),
                        cb.lessThan(b.get("reservedQuantity"), b.<Integer>get("capacity")),
                        cb.greaterThan(b.<Instant>get("orderCutoff"), now));
                ps.add(cb.exists(sub));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
    }

    private static boolean has(String s) {
        return s != null && !s.isBlank();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
