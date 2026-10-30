package com.gharse.admin;

import com.gharse.admin.AdminDtos.AdminCookDetail;
import com.gharse.admin.AdminDtos.AdminCookRow;
import com.gharse.admin.AdminDtos.DashboardResponse;
import com.gharse.admin.AdminDtos.CreateDeliveryAreaRequest;
import com.gharse.admin.AdminDtos.DeliveryAreaResponse;
import com.gharse.admin.AdminDtos.DeliveryAreaStatusRequest;
import com.gharse.common.ApiException;
import com.gharse.common.AppProperties;
import com.gharse.common.PageResponse;
import com.gharse.common.storage.StorageService;
import com.gharse.cook.ApprovalStatus;
import com.gharse.cook.Cook;
import com.gharse.cook.CookDocumentRepository;
import com.gharse.cook.CookRepository;
import com.gharse.cook.CookService;
import com.gharse.cook.DocumentType;
import com.gharse.kitchen.KitchenRepository;
import com.gharse.menu.DishDtos.DishResponse;
import com.gharse.menu.DishRepository;
import com.gharse.notification.NotificationService;
import com.gharse.notification.NotificationType;
import com.gharse.order.OrderRepository;
import com.gharse.order.OrderStatus;
import com.gharse.user.User;
import com.gharse.user.UserRepository;
import com.gharse.user.UserResponse;
import com.gharse.user.UserRole;
import com.gharse.user.UserStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Platform administration. Only reachable through /api/v1/admin/** (ROLE_ADMIN). */
@Service
@Transactional
public class AdminService {

    private final UserRepository users;
    private final CookRepository cooks;
    private final KitchenRepository kitchens;
    private final CookDocumentRepository documents;
    private final DishRepository dishes;
    private final OrderRepository orders;
    private final CookService cookService;
    private final NotificationService notifications;
    private final AppProperties props;
    private final StorageService storage;
    private final DeliveryAreaRepository deliveryAreas;

    public AdminService(UserRepository users, CookRepository cooks, KitchenRepository kitchens, CookDocumentRepository documents,
                        DishRepository dishes, OrderRepository orders, CookService cookService,
                        NotificationService notifications, AppProperties props, StorageService storage,
                        DeliveryAreaRepository deliveryAreas) {
        this.users = users;
        this.cooks = cooks;
        this.kitchens = kitchens;
        this.documents = documents;
        this.dishes = dishes;
        this.orders = orders;
        this.cookService = cookService;
        this.notifications = notifications;
        this.props = props;
        this.storage = storage;
        this.deliveryAreas = deliveryAreas;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        BigDecimal revenue = orders.totalRevenue();
        return new DashboardResponse(users.count(), users.countByRole(UserRole.CUSTOMER), cooks.count(),
                cooks.countByApprovalStatus(ApprovalStatus.PENDING), cooks.countByApprovalStatus(ApprovalStatus.APPROVED),
                cooks.countByApprovalStatus(ApprovalStatus.REJECTED), orders.count(),
                orders.countByOrderStatus(OrderStatus.COMPLETED), revenue == null ? BigDecimal.ZERO : revenue);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminCookRow> cooks(ApprovalStatus status, int page, int size) {
        var pr = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
        var result = status == null ? cooks.findAll(pr) : cooks.findByApprovalStatus(status, pr);
        return PageResponse.of(result, AdminCookRow::of);
    }

    @Transactional(readOnly = true)
    public AdminCookDetail cookDetail(UUID cookId) {
        Cook c = cooks.findById(cookId).orElseThrow(() -> ApiException.notFound("Cook"));
        return new AdminCookDetail(cookService.profileOf(c),
                dishes.findByCookIdOrderByNameAsc(cookId).stream().map(DishResponse::of).toList());
    }

    public AdminCookRow approve(UUID adminUserId, UUID cookId) {
        Cook c = cooks.findById(cookId).orElseThrow(() -> ApiException.notFound("Cook"));
        if (c.getUser().getId().equals(adminUserId)) throw ApiException.forbidden("You cannot approve your own profile");
        if (props.cook().fssaiRequired() && !documents.existsByCookIdAndDocumentType(cookId, DocumentType.FSSAI)) {
            throw ApiException.conflict("FSSAI_REQUIRED", "An FSSAI document is required before approval");
        }
        c.setApprovalStatus(ApprovalStatus.APPROVED);
        c.setRejectionReason(null);
        kitchens.findByCookId(cookId).ifPresent(k -> k.setVerificationStatus(ApprovalStatus.APPROVED));
        documents.findByCookIdOrderByCreatedAtDesc(cookId).stream()
                .filter(d -> d.getStatus() == ApprovalStatus.PENDING).forEach(d -> d.setStatus(ApprovalStatus.APPROVED));
        notifications.notify(c.getUser().getId(), NotificationType.COOK_APPROVED, "You're approved!",
                "Welcome to CUOQO. You can now add dishes and create batches.");
        return AdminCookRow.of(c);
    }

    public AdminCookRow reject(UUID adminUserId, UUID cookId, String reason) {
        Cook c = cooks.findById(cookId).orElseThrow(() -> ApiException.notFound("Cook"));
        if (c.getUser().getId().equals(adminUserId)) throw ApiException.forbidden("You cannot reject your own profile");
        c.setApprovalStatus(ApprovalStatus.REJECTED);
        c.setRejectionReason(reason);
        notifications.notify(c.getUser().getId(), NotificationType.COOK_REJECTED, "Profile needs changes", reason);
        return AdminCookRow.of(c);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> users(UserRole role, int page, int size) {
        var pr = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
        var result = role == null ? users.findAll(pr) : users.findByRole(role, pr);
        return PageResponse.of(result, (User u) -> UserResponse.of(u, null));
    }

    public UserResponse setUserStatus(UUID adminUserId, UUID userId, UserStatus status) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        if (user.getId().equals(adminUserId) && status == UserStatus.SUSPENDED) {
            throw ApiException.forbidden("You cannot suspend your own account");
        }
        if (user.getStatus() != status) {
            user.setStatus(status);
            user.setCredentialVersion(user.getCredentialVersion() + 1);
        }
        return UserResponse.of(user, null);
    }

    @Transactional(readOnly = true)
    public java.util.List<DeliveryAreaResponse> deliveryAreas() {
        return deliveryAreas.findAllByOrderByCityAscLocalityAsc().stream()
                .map(DeliveryAreaResponse::of).toList();
    }

    public DeliveryAreaResponse addDeliveryArea(CreateDeliveryAreaRequest request) {
        DeliveryAreaType type = request.areaType() == null ? DeliveryAreaType.PLACE : request.areaType();
        if (type == DeliveryAreaType.PLACE) {
            if (request.placeId() == null || request.placeId().isBlank() || request.formattedAddress() == null
                    || request.locality() == null || request.city() == null || request.latitude() == null
                    || request.longitude() == null) {
                throw ApiException.badRequest("INVALID_DELIVERY_AREA", "A place pin needs a place ID, address, locality, city and coordinates");
            }
            if (deliveryAreas.findByPlaceId(request.placeId()).isPresent()) {
                throw ApiException.conflict("DELIVERY_AREA_EXISTS", "This place is already on the delivery map");
            }
        } else if (request.boundary() == null || request.boundary().size() < 3) {
            throw ApiException.badRequest("INVALID_DELIVERY_AREA", "A delivery region must have at least three boundary points");
        }
        DeliveryArea area = new DeliveryArea();
        area.setAreaType(type);
        area.setPlaceId(request.placeId());
        area.setName(request.name());
        area.setFormattedAddress(request.formattedAddress());
        area.setLocality(request.locality());
        area.setCity(request.city());
        area.setLatitude(request.latitude());
        area.setLongitude(request.longitude());
        area.setBoundary(request.boundary());
        area.setStatus(request.status());
        return DeliveryAreaResponse.of(deliveryAreas.save(area));
    }

    public DeliveryAreaResponse setDeliveryAreaStatus(UUID id, DeliveryAreaStatusRequest request) {
        DeliveryArea area = deliveryAreas.findById(id).orElseThrow(() -> ApiException.notFound("Delivery area"));
        area.setStatus(request.status());
        return DeliveryAreaResponse.of(area);
    }

    public void deleteDeliveryArea(UUID id) {
        DeliveryArea area = deliveryAreas.findById(id).orElseThrow(() -> ApiException.notFound("Delivery area"));
        deliveryAreas.delete(area);
    }

    @Transactional(readOnly = true)
    public String documentUrl(UUID cookId, UUID documentId) {
        var document = documents.findById(documentId)
                .filter(d -> d.getCook().getId().equals(cookId))
                .orElseThrow(() -> ApiException.notFound("Cook document"));
        String key = document.getDocumentUrl();
        if (key == null || key.isBlank()) throw ApiException.notFound("Cook document file");
        return storage.getPrivateUrl(key, Duration.ofMinutes(5));
    }
}
