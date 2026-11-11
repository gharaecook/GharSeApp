package com.gharse.admin;

import com.gharse.admin.AdminDtos.AdminCookDetail;
import com.gharse.admin.AdminDtos.AdminCookRow;
import com.gharse.admin.AdminDtos.DashboardResponse;
import com.gharse.admin.AdminDtos.CreateDeliveryAreaRequest;
import com.gharse.admin.AdminDtos.DeliveryAreaResponse;
import com.gharse.admin.AdminDtos.DeliveryAreaStatusRequest;
import com.gharse.auth.AuthUser;
import com.gharse.common.PageResponse;
import com.gharse.cook.ApprovalStatus;
import com.gharse.cook.CookDtos.RejectRequest;
import com.gharse.order.OrderDtos.OrderResponse;
import com.gharse.order.OrderService;
import com.gharse.order.OrderStatus;
import com.gharse.user.UserResponse;
import com.gharse.user.UserRole;
import com.gharse.user.UserStatus;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService service;
    private final OrderService orderService;

    public AdminController(AdminService service, OrderService orderService) {
        this.service = service;
        this.orderService = orderService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return service.dashboard();
    }

    @GetMapping("/delivery-areas")
    public java.util.List<DeliveryAreaResponse> deliveryAreas() {
        return service.deliveryAreas();
    }

    @PostMapping("/delivery-areas")
    public DeliveryAreaResponse addDeliveryArea(@Valid @RequestBody CreateDeliveryAreaRequest request) {
        return service.addDeliveryArea(request);
    }

    @PutMapping("/delivery-areas/{id}/status")
    public DeliveryAreaResponse setDeliveryAreaStatus(@PathVariable UUID id,
                                                       @Valid @RequestBody DeliveryAreaStatusRequest request) {
        return service.setDeliveryAreaStatus(id, request);
    }

    @DeleteMapping("/delivery-areas/{id}")
    public void deleteDeliveryArea(@PathVariable UUID id) {
        service.deleteDeliveryArea(id);
    }

    @GetMapping("/cooks")
    public PageResponse<AdminCookRow> cooks(@RequestParam(required = false) ApprovalStatus status,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return service.cooks(status, Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    }

    @GetMapping("/cooks/pending")
    public PageResponse<AdminCookRow> pending(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return service.cooks(ApprovalStatus.PENDING, Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    }

    @GetMapping("/cooks/{id}")
    public AdminCookDetail cook(@PathVariable UUID id) {
        return service.cookDetail(id);
    }

    @GetMapping("/cooks/{cookId}/documents/{documentId}/url")
    public Map<String, String> cookDocumentUrl(@PathVariable UUID cookId, @PathVariable UUID documentId) {
        return Map.of("url", service.documentUrl(cookId, documentId));
    }

    @PostMapping("/cooks/{id}/approve")
    public AdminCookRow approve(@PathVariable UUID id) {
        return service.approve(AuthUser.current().id(), id);
    }

    @PostMapping("/cooks/{id}/reject")
    public AdminCookRow reject(@PathVariable UUID id, @Valid @RequestBody RejectRequest r) {
        return service.reject(AuthUser.current().id(), id, r.reason());
    }

    @GetMapping("/orders")
    public PageResponse<OrderResponse> orders(@RequestParam(required = false) OrderStatus status,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return orderService.all(status, Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    }

    @GetMapping("/users")
    public PageResponse<UserResponse> users(@RequestParam(required = false) UserRole role,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return service.users(role, Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    }

    @PostMapping("/users/{id}/suspend")
    public UserResponse suspendUser(@PathVariable UUID id) {
        return service.setUserStatus(AuthUser.current().id(), id, UserStatus.SUSPENDED);
    }

    @PostMapping("/users/{id}/reactivate")
    public UserResponse reactivateUser(@PathVariable UUID id) {
        return service.setUserStatus(AuthUser.current().id(), id, UserStatus.ACTIVE);
    }
}
