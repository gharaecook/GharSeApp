package com.gharse.cook;

import com.gharse.auth.AuthUser;
import com.gharse.common.PageResponse;
import com.gharse.cook.CookDtos.CookDetail;
import com.gharse.cook.CookDtos.CookSummary;
import com.gharse.menu.DishCategory;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public cook discovery plus customer favorites. */
@RestController
@RequestMapping("/api/v1")
public class CookController {

    private final CookService service;
    private final CookRepository cooks;
    private final FavoriteRepository favorites;
    private final Clock clock;

    public CookController(CookService service, CookRepository cooks, FavoriteRepository favorites, Clock clock) {
        this.service = service;
        this.cooks = cooks;
        this.favorites = favorites;
        this.clock = clock;
    }

    /**
     * Search and filter approved cooks. {@code q} matches cook name, locality, speciality, cuisine and dish names.
     * Pass {@code lat}/{@code lng} to get distances and nearest-first ordering. {@code sort}: rating | newest | popular.
     */
    @GetMapping("/cooks")
    public PageResponse<CookSummary> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) DishCategory category,
            @RequestParam(required = false) Boolean vegetarian,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) Boolean availableToday,
            @RequestParam(required = false) String locality,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "rating") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        return service.search(new CookSearch(q, cuisine, category, vegetarian, minRating, availableToday, locality, city),
                sort, lat, lng, Math.max(page, 0), safeSize);
    }

    @GetMapping("/cooks/top")
    public List<CookSummary> topCooks() {
        return service.topCooks();
    }

    @GetMapping("/cooks/{id}")
    public CookDetail detail(@PathVariable UUID id) {
        return service.detail(id, AuthUser.currentOrNull());
    }

    @PostMapping("/cooks/{id}/favorite")
    @Transactional
    public ResponseEntity<Void> favorite(@PathVariable UUID id) {
        AuthUser me = AuthUser.current();
        service.getApproved(id);
        var key = new FavoriteCook.Key(me.id(), id);
        if (!favorites.existsById(key)) favorites.save(new FavoriteCook(me.id(), id, clock.instant()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/cooks/{id}/favorite")
    @Transactional
    public ResponseEntity<Void> unfavorite(@PathVariable UUID id) {
        AuthUser me = AuthUser.current();
        var key = new FavoriteCook.Key(me.id(), id);
        if (favorites.existsById(key)) favorites.deleteById(key);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/favorites")
    @Transactional(readOnly = true)
    public List<CookSummary> myFavorites() {
        List<UUID> ids = favorites.findCookIdsByCustomer(AuthUser.current().id());
        List<Cook> found = cooks.findAllById(ids).stream().filter(Cook::isApproved).toList();
        var byId = service.summariesOf(found).stream().collect(java.util.stream.Collectors.toMap(CookSummary::id, s -> s));
        return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
    }
}
