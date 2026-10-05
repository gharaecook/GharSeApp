package com.gharse.order;

import com.gharse.availability.Batch;
import com.gharse.availability.BatchDishItem;
import com.gharse.availability.BatchRepository;
import com.gharse.common.ApiException;
import com.gharse.cook.Cook;
import com.gharse.menu.Dish;
import com.gharse.menu.DishRepository;
import com.gharse.order.OrderDtos.AddCartItemRequest;
import com.gharse.order.OrderDtos.CartItemResponse;
import com.gharse.order.OrderDtos.CartResponse;
import com.gharse.user.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A cart holds items from exactly one cook; adding from another cook is refused until the cart is cleared. */
@Service
@Transactional
public class CartService {

    private final CartRepository carts;
    private final DishRepository dishes;
    private final BatchRepository batches;
    private final UserRepository users;
    private final Clock clock;

    public CartService(CartRepository carts, DishRepository dishes, BatchRepository batches, UserRepository users, Clock clock) {
        this.carts = carts;
        this.dishes = dishes;
        this.batches = batches;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CartResponse get(UUID customerId) {
        return carts.findByCustomerId(customerId).map(this::toResponse)
                .orElseGet(() -> new CartResponse(null, null, null, java.util.List.of(), BigDecimal.ZERO, 0));
    }

    public CartResponse addItem(UUID customerId, AddCartItemRequest r) {
        Dish dish = dishes.findById(r.dishId()).orElseThrow(() -> ApiException.notFound("Dish"));
        Batch batch = batches.findById(r.batchId()).orElseThrow(() -> ApiException.notFound("Batch"));
        Cook cook = dish.getCook();
        if (!dish.isActive()) throw ApiException.badRequest("DISH_INACTIVE", "This dish is not available");
        if (!cook.isApproved()) throw ApiException.notFound("Dish");
        if (!batch.getCook().getId().equals(cook.getId())) throw ApiException.badRequest("BATCH_MISMATCH", "Dish is not part of this batch");
        BatchDishItem offered = batch.itemFor(dish.getId());
        if (offered == null) throw ApiException.badRequest("BATCH_MISMATCH", "Dish is not part of this batch");
        Instant now = clock.instant();
        if (batch.isSoldOut()) throw ApiException.conflict("BATCH_SOLD_OUT", "Sorry, this batch is sold out");
        if (!batch.isOrderingOpen(now)) throw ApiException.conflict("ORDERING_CLOSED", "Ordering for this batch has closed");

        Cart cart = carts.findByCustomerId(customerId).orElseGet(() -> {
            Cart c = new Cart();
            c.setCustomer(users.getReferenceById(customerId));
            return c;
        });
        if (!cart.getItems().isEmpty() && cart.getCook() != null && !cart.getCook().getId().equals(cook.getId())) {
            throw ApiException.conflict("CART_OTHER_COOK", "Your cart contains items from another Home Cook.");
        }
        cart.setCook(cook);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getDish().getId().equals(dish.getId()) && i.getBatch().getId().equals(batch.getId()))
                .findFirst().orElse(null);
        int newQty = (item == null ? 0 : item.getQuantity()) + r.quantity();
        ensureWithinRemaining(batch, newQty, cart, item);
        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setDish(dish);
            item.setBatch(batch);
            cart.getItems().add(item);
        }
        item.setQuantity(newQty);
        item.setUnitPrice(offered.getPrice());
        return toResponse(carts.save(cart));
    }

    public CartResponse updateItem(UUID customerId, UUID itemId, int quantity) {
        Cart cart = carts.findByCustomerId(customerId).orElseThrow(() -> ApiException.notFound("Cart item"));
        CartItem item = cart.getItems().stream().filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("Cart item"));
        ensureWithinRemaining(item.getBatch(), quantity, cart, item);
        item.setQuantity(quantity);
        return toResponse(carts.save(cart));
    }

    public CartResponse removeItem(UUID customerId, UUID itemId) {
        Cart cart = carts.findByCustomerId(customerId).orElseThrow(() -> ApiException.notFound("Cart item"));
        boolean removed = cart.getItems().removeIf(i -> i.getId().equals(itemId));
        if (!removed) throw ApiException.notFound("Cart item");
        if (cart.getItems().isEmpty()) cart.setCook(null);
        return toResponse(carts.save(cart));
    }

    public void clear(UUID customerId) {
        carts.findByCustomerId(customerId).ifPresent(c -> {
            c.getItems().clear();
            c.setCook(null);
        });
    }

    /** Soft check for a good error message early; the hard guarantee is the atomic reservation at checkout. */
    private void ensureWithinRemaining(Batch batch, int quantityOfThisLine, Cart cart, CartItem line) {
        int others = cart.getItems().stream()
                .filter(i -> i != line && i.getBatch().getId().equals(batch.getId()))
                .mapToInt(CartItem::getQuantity).sum();
        if (others + quantityOfThisLine > batch.remaining()) {
            throw ApiException.conflict("BATCH_CAPACITY_EXCEEDED",
                    "Only " + batch.remaining() + " portion(s) left in " + batch.getName());
        }
    }

    private CartResponse toResponse(Cart cart) {
        Instant now = clock.instant();
        var items = cart.getItems().stream().map(i -> {
            BatchDishItem offered = i.getBatch().itemFor(i.getDish().getId());
            BigDecimal price = offered != null ? offered.getPrice() : i.getUnitPrice();
            return new CartItemResponse(i.getId(), i.getDish().getId(), i.getDish().getName(), i.getDish().getImageUrl(),
                    i.getBatch().getId(), i.getBatch().getName(), i.getBatch().getAvailableDate(), price, i.getQuantity(),
                    price.multiply(BigDecimal.valueOf(i.getQuantity())), i.getBatch().remaining(),
                    i.getBatch().isOrderingOpen(now) && i.getDish().isActive());
        }).toList();
        BigDecimal subtotal = items.stream().map(CartItemResponse::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        Cook cook = cart.getCook();
        return new CartResponse(cart.getId(), cook == null ? null : cook.getId(),
                cook == null ? null : cook.getUser().getName(), items, subtotal,
                items.stream().mapToInt(CartItemResponse::quantity).sum());
    }
}
