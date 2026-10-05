package com.gharse.user;

import com.gharse.auth.AuthUser;
import com.gharse.common.ApiException;
import com.gharse.cook.Cook;
import com.gharse.cook.CookRepository;
import jakarta.validation.Valid;
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
                                  @Size(max = 500) String address) {}

    private final UserRepository users;
    private final CookRepository cooks;

    public UserController(UserRepository users, CookRepository cooks) {
        this.users = users;
        this.cooks = cooks;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public UserResponse me() {
        return response(load());
    }

    @PutMapping
    public UserResponse update(@Valid @RequestBody UpdateMeRequest r) {
        User u = load();
        if (r.name() != null && !r.name().isBlank()) u.setName(r.name().trim());
        if (r.phone() != null) u.setPhone(r.phone().trim());
        if (r.address() != null) u.setAddress(r.address());
        return response(u);
    }

    private User load() {
        return users.findById(AuthUser.current().id()).orElseThrow(() -> ApiException.unauthorized("Account not found"));
    }

    private UserResponse response(User u) {
        UUID cookId = u.getRole() == UserRole.COOK ? cooks.findByUserId(u.getId()).map(Cook::getId).orElse(null) : null;
        return UserResponse.of(u, cookId);
    }
}
