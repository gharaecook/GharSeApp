package com.gharse.cook;

import com.gharse.auth.AuthUser;
import com.gharse.availability.BatchRepository;
import com.gharse.availability.BatchStatus;
import com.gharse.common.ApiException;
import com.gharse.common.PageResponse;
import com.gharse.cook.CookDtos.*;
import com.gharse.cuisine.Cuisine;
import com.gharse.cuisine.CuisineRepository;
import com.gharse.kitchen.Kitchen;
import com.gharse.kitchen.KitchenRepository;
import com.gharse.order.OrderRepository;
import com.gharse.user.User;
import com.gharse.user.UserRepository;
import com.gharse.user.UserStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cook discovery (public) and cook profile management (the cook themselves). */
@Service
@Transactional(readOnly = true)
public class CookService {

    private final CookRepository cooks;
    private final KitchenRepository kitchens;
    private final CookDocumentRepository documents;
    private final BatchRepository batches;
    private final FavoriteRepository favorites;
    private final CuisineRepository cuisines;
    private final UserRepository users;
    private final OrderRepository orders;
    private final Clock clock;
    private final ZoneId zone;

    public CookService(CookRepository cooks, KitchenRepository kitchens, CookDocumentRepository documents,
                       BatchRepository batches, FavoriteRepository favorites, CuisineRepository cuisines,
                       UserRepository users, OrderRepository orders, Clock clock, ZoneId zone) {
        this.cooks = cooks;
        this.kitchens = kitchens;
        this.documents = documents;
        this.batches = batches;
        this.favorites = favorites;
        this.cuisines = cuisines;
        this.users = users;
        this.orders = orders;
        this.clock = clock;
        this.zone = zone;
    }

    // ---------------------------------------------------------------- discovery

    public PageResponse<CookSummary> search(CookSearch criteria, String sort, Double lat, Double lng, int page, int size) {
        LocalDate today = LocalDate.now(clock.withZone(zone));
        var spec = CookSpecifications.discover(criteria, today, clock.instant());
        if (lat != null && lng != null) {
            // Distance ordering needs the kitchen coordinates, so rank in memory (fine at MVP scale).
            List<CookSummary> all = summaries(cooks.findAll(spec), lat, lng).stream()
                    .sorted(Comparator.comparing(CookSummary::distanceKm, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            return PageResponse.of(all, page, size);
        }
        var result = cooks.findAll(spec, PageRequest.of(page, size, sortFor(sort)));
        List<CookSummary> content = summaries(result.getContent(), null, null);
        return new PageResponse<>(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public List<CookSummary> topCooks() {
        List<Cook> approved = cooks.findByApprovalStatus(ApprovalStatus.APPROVED).stream()
                .filter(c -> c.getUser().getStatus() == UserStatus.ACTIVE)
                .toList();
        Map<UUID, Long> completedOrders = completedOrderCounts(approved);
        Comparator<Cook> ranking = Comparator
                .<Cook>comparingLong(c -> completedOrders.getOrDefault(c.getId(), 0L))
                .reversed()
                .thenComparing(Comparator.comparingInt(Cook::getReviewCount).reversed())
                .thenComparing(Comparator.comparing(Cook::getRating).reversed())
                .thenComparing(Cook::getCreatedAt)
                .thenComparing(Cook::getId);
        List<Cook> top = approved.stream().sorted(ranking).limit(3).toList();
        return summaries(top, null, null);
    }

    public CookDetail detail(UUID id, AuthUser viewer) {
        Cook c = getApproved(id);
        CookSummary summary = summaries(List.of(c), null, null).get(0);
        Kitchen k = kitchens.findByCookId(id).orElse(null);
        Boolean favorite = viewer == null ? null : favorites.existsById(new FavoriteCook.Key(viewer.id(), id));
        return new CookDetail(summary, c.getBio(), k == null ? null : k.getDescription(),
                k == null ? List.of() : List.copyOf(k.getPhotos()), fssaiApproved(id),
                k != null && k.getVerificationStatus() == ApprovalStatus.APPROVED, favorite);
    }

    public Cook getApproved(UUID id) {
        return cooks.findById(id).filter(Cook::isApproved)
                .filter(c -> c.getUser().getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> ApiException.notFound("Cook"));
    }

    public List<CookSummary> summariesOf(List<Cook> list) {
        return summaries(list, null, null);
    }

    private List<CookSummary> summaries(List<Cook> list, Double lat, Double lng) {
        if (list.isEmpty()) return List.of();
        Set<UUID> ids = list.stream().map(Cook::getId).collect(Collectors.toSet());
        Map<UUID, Long> completedOrders = completedOrderCounts(list);
        LocalDate today = LocalDate.now(clock.withZone(zone));
        Set<UUID> availableToday = batches.findCookIdsWithBatchToday(ids, today, BatchStatus.OPEN, clock.instant());
        Map<UUID, Kitchen> kitchenByCook = kitchens.findByCookIdIn(ids).stream()
                .collect(Collectors.toMap(k -> k.getCook().getId(), Function.identity()));
        Set<UUID> fssai = documents.findByCookIdInAndDocumentType(ids, DocumentType.FSSAI).stream()
                .filter(d -> d.getStatus() == ApprovalStatus.APPROVED).map(d -> d.getCook().getId()).collect(Collectors.toSet());
        List<CookSummary> out = new ArrayList<>();
        for (Cook c : list) {
            Kitchen k = kitchenByCook.get(c.getId());
            Double distance = null;
            if (lat != null && lng != null && k != null && k.getLatitude() != null && k.getLongitude() != null) {
                distance = Math.round(haversineKm(lat, lng, k.getLatitude(), k.getLongitude()) * 10) / 10.0;
            }
            boolean verified = fssai.contains(c.getId()) || (k != null && k.getVerificationStatus() == ApprovalStatus.APPROVED);
            out.add(new CookSummary(c.getId(), c.getUser().getName(), c.getHeadline(), c.getLocality(), c.getCity(),
                    c.getProfilePhotoUrl(), c.getYearsOfCooking(), c.getRating(), c.getReviewCount(),
                    completedOrders.getOrDefault(c.getId(), 0L),
                    c.getCuisines().stream().map(Cuisine::getName).sorted().toList(), splitDishes(c.getSignatureDishes()),
                    snippet(c.getBio()), availableToday.contains(c.getId()), distance, verified, c.getCreatedAt()));
        }
        return out;
    }

    private Map<UUID, Long> completedOrderCounts(List<Cook> list) {
        if (list.isEmpty()) return Map.of();
        Set<UUID> ids = list.stream().map(Cook::getId).collect(Collectors.toSet());
        return orders.completedOrderCounts(ids).stream()
                .collect(Collectors.toMap(OrderRepository.CookOrderCount::getCookId, OrderRepository.CookOrderCount::getOrderCount));
    }

    private boolean fssaiApproved(UUID cookId) {
        return documents.findByCookIdInAndDocumentType(Set.of(cookId), DocumentType.FSSAI).stream()
                .anyMatch(d -> d.getStatus() == ApprovalStatus.APPROVED);
    }

    private static Sort sortFor(String sort) {
        return switch (sort == null ? "rating" : sort) {
            case "newest" -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));
            case "popular" -> Sort.by(Sort.Order.desc("reviewCount"), Sort.Order.desc("rating"), Sort.Order.asc("id"));
            default -> Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("reviewCount"), Sort.Order.asc("id"));
        };
    }

    static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }

    private static List<String> splitDishes(String s) {
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).toList();
    }

    private static String snippet(String bio) {
        if (bio == null) return null;
        return bio.length() <= 140 ? bio : bio.substring(0, 137).trim() + "...";
    }

    // ---------------------------------------------------------------- the cook's own profile

    public Cook requireCook(UUID userId) {
        return cooks.findByUserId(userId).orElseThrow(() -> ApiException.forbidden("No cook profile for this account"));
    }

    /** Only approved cooks can publish food (dishes, batches). */
    public Cook requireApprovedCook(UUID userId) {
        Cook c = requireCook(userId);
        if (!c.isApproved()) {
            throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "COOK_NOT_APPROVED",
                    "Your profile must be approved by CUOQO before you can publish food");
        }
        return c;
    }

    public CookProfileResponse myProfile(UUID userId) {
        return profileOf(requireCook(userId));
    }

    public CookProfileResponse profileOf(Cook c) {
        return CookProfileResponse.of(c, kitchens.findByCookId(c.getId()).orElse(null),
                documents.findByCookIdOrderByCreatedAtDesc(c.getId()));
    }

    /** Creates the cook record and an empty kitchen for a freshly registered cook account. */
    @Transactional
    public Cook createForUser(User user, String locality) {
        Cook c = new Cook();
        c.setUser(user);
        c.setLocality(locality);
        c = cooks.save(c);
        Kitchen k = new Kitchen();
        k.setCook(c);
        k.setLocality(locality);
        kitchens.save(k);
        return c;
    }

    @Transactional
    public CookProfileResponse updateProfile(UUID userId, UpdateCookProfileRequest r) {
        Cook c = requireCook(userId);
        User u = c.getUser();
        if (r.name() != null && !r.name().isBlank()) u.setName(r.name().trim());
        if (r.phone() != null) u.setPhone(r.phone().trim());
        if (r.address() != null) u.setAddress(r.address());
        if (r.headline() != null) c.setHeadline(r.headline());
        if (r.bio() != null) c.setBio(r.bio());
        if (r.yearsOfCooking() != null) c.setYearsOfCooking(r.yearsOfCooking());
        if (r.locality() != null) c.setLocality(r.locality());
        if (r.city() != null && !r.city().isBlank()) c.setCity(r.city());
        if (r.profilePhotoUrl() != null) c.setProfilePhotoUrl(r.profilePhotoUrl());
        if (r.signatureDishes() != null) c.setSignatureDishes(r.signatureDishes());
        if (r.cuisineIds() != null) {
            Set<Cuisine> found = new HashSet<>(cuisines.findAllById(r.cuisineIds()));
            if (found.size() != new HashSet<>(r.cuisineIds()).size()) {
                throw ApiException.badRequest("UNKNOWN_CUISINE", "One or more cuisines do not exist");
            }
            c.getCuisines().clear();
            c.getCuisines().addAll(found);
        }
        Kitchen k = kitchens.findByCookId(c.getId()).orElseGet(() -> {
            Kitchen nk = new Kitchen();
            nk.setCook(c);
            return nk;
        });
        if (r.kitchenAddress() != null) k.setAddress(r.kitchenAddress());
        if (r.kitchenLocality() != null) k.setLocality(r.kitchenLocality());
        if (r.latitude() != null) k.setLatitude(r.latitude());
        if (r.longitude() != null) k.setLongitude(r.longitude());
        if (r.kitchenDescription() != null) k.setDescription(r.kitchenDescription());
        if (r.kitchenPhotos() != null) {
            k.getPhotos().clear();
            k.getPhotos().addAll(r.kitchenPhotos());
        }
        kitchens.save(k);
        if (c.getApprovalStatus() == ApprovalStatus.REJECTED) { // resubmission after fixing the profile
            c.setApprovalStatus(ApprovalStatus.PENDING);
            c.setRejectionReason(null);
        }
        users.save(u);
        cooks.save(c);
        return profileOf(c);
    }

    @Transactional
    public DocumentResponse addDocument(UUID userId, DocumentRequest r) {
        Cook c = requireCook(userId);
        CookDocument d = new CookDocument();
        d.setCook(c);
        d.setDocumentType(r.documentType());
        d.setDocumentNumber(r.documentNumber());
        d.setDocumentUrl(r.documentUrl());
        d.setExpiryDate(r.expiryDate());
        return DocumentResponse.of(documents.save(d));
    }

    @Transactional
    public void applyRating(UUID cookId, java.math.BigDecimal rating, int count) {
        Cook c = cooks.findById(cookId).orElseThrow(() -> ApiException.notFound("Cook"));
        c.setRating(rating);
        c.setReviewCount(count);
    }
}
