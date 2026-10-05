package com.gharse.order;

import com.gharse.auth.AuthUser;
import com.gharse.order.OrderDtos.AddCartItemRequest;
import com.gharse.order.OrderDtos.CartResponse;
import com.gharse.order.OrderDtos.UpdateCartItemRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @GetMapping
    public CartResponse get() {
        return service.get(AuthUser.current().id());
    }

    /** 409 CART_OTHER_COOK when the cart already holds food from a different cook. */
    @PostMapping("/items")
    public CartResponse add(@Valid @RequestBody AddCartItemRequest r) {
        return service.addItem(AuthUser.current().id(), r);
    }

    @PutMapping("/items/{id}")
    public CartResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCartItemRequest r) {
        return service.updateItem(AuthUser.current().id(), id, r.quantity());
    }

    @DeleteMapping("/items/{id}")
    public CartResponse remove(@PathVariable UUID id) {
        return service.removeItem(AuthUser.current().id(), id);
    }

    /** Empties the cart (used by the "Clear Cart" choice when switching cooks). */
    @DeleteMapping
    public ResponseEntity<Void> clear() {
        service.clear(AuthUser.current().id());
        return ResponseEntity.noContent().build();
    }
}
