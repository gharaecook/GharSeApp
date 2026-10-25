package com.gharse.user;

import com.gharse.auth.AuthUser;
import com.gharse.common.ApiException;
import com.gharse.cook.Cook;
import com.gharse.cook.CookRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
@Transactional
public class UserController {

    public record UpdateMeRequest(@Size(min = 1, max = 120) String name,
                                  @Pattern(regexp = "^[0-9+\\-\\s]{10,20}$", message = "must be a valid phone number") String phone,
                                  @Size(max = 500) String address,
                                  @Size(max = 120) String society,
                                  @Size(max = 120) String area,
                                  @Size(max = 200) String landmark,
                                  @DecimalMin("-90") @DecimalMax("90") Double deliveryLatitude,
                                  @DecimalMin("-180") @DecimalMax("180") Double deliveryLongitude) {}

    private final UserRepository users;
    private final CookRepository cooks;

    public UserController(UserRepository users, CookRepository cooks) {
        this.users = users;
        this.cooks = cooks;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public UserProfileResponse me() {
        return response(load());
    }

    @PutMapping
    public UserProfileResponse update(@Valid @RequestBody UpdateMeRequest r) {
        User u = load();
        if ((r.deliveryLatitude() == null) != (r.deliveryLongitude() == null)) {
            throw ApiException.badRequest("INVALID_DELIVERY_LOCATION", "Provide both delivery latitude and longitude");
        }
        String combinedAddress = java.util.stream.Stream.of(
                        r.address() == null ? u.getAddress() : r.address(),
                        r.society() == null ? u.getSociety() : r.society(),
                        r.area() == null ? u.getArea() : r.area(),
                        r.landmark() == null ? u.getLandmark() : r.landmark())
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.joining(", "));
        if (combinedAddress.length() > 500) {
            throw ApiException.badRequest("DELIVERY_ADDRESS_TOO_LONG", "Delivery address must be 500 characters or less");
        }
        if (r.name() != null && !r.name().isBlank()) u.setName(r.name().trim());
        if (r.phone() != null) u.setPhone(r.phone().trim());
        if (r.address() != null) u.setAddress(r.address().trim());
        if (r.society() != null) u.setSociety(r.society().trim());
        if (r.area() != null) u.setArea(r.area().trim());
        if (r.landmark() != null) u.setLandmark(r.landmark().trim());
        if (r.deliveryLatitude() != null) u.setDeliveryLatitude(r.deliveryLatitude());
        if (r.deliveryLongitude() != null) u.setDeliveryLongitude(r.deliveryLongitude());
        return response(u);
    }

    private User load() {
        return users.findById(AuthUser.current().id()).orElseThrow(() -> ApiException.unauthorized("Account not found"));
    }

    private UserProfileResponse response(User u) {
        UUID cookId = u.getRole() == UserRole.COOK ? cooks.findByUserId(u.getId()).map(Cook::getId).orElse(null) : null;
        return UserProfileResponse.of(u, cookId);
    }
}
