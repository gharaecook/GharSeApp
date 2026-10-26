# API

Base path `/api/v1`. JSON in and out. Interactive docs: **`/swagger-ui.html`** (OpenAPI at `/v3/api-docs`), enabled in
the `local` and `test` profiles only.

Authenticate with `Authorization: Bearer <accessToken>`. Roles: `CUSTOMER`, `COOK`, `ADMIN`.

## Errors

```json
{ "timestamp": "2026-10-05T14:00:00Z", "status": 400, "code": "VALIDATION_ERROR", "message": "Invalid request",
  "path": "/api/v1/auth/register", "errors": [ { "field": "email", "message": "must be a well-formed email address" } ] }
```

Common codes: `VALIDATION_ERROR` 400, `UNAUTHORIZED` 401, `FORBIDDEN` 403, `NOT_FOUND` 404, `RATE_LIMITED` 429,
`INTERNAL_ERROR` 500. Business codes: `EMAIL_TAKEN`, `COOK_NOT_APPROVED`, `CART_OTHER_COOK`, `CART_EMPTY`,
`BATCH_SOLD_OUT`, `BATCH_CAPACITY_EXCEEDED`, `ORDERING_CLOSED`, `DISH_INACTIVE`, `DELIVERY_ADDRESS_REQUIRED`,
`INVALID_ORDER_TRANSITION`, `ORDER_NOT_CANCELLABLE`, `ORDER_NOT_COMPLETED`, `REVIEW_ALREADY_EXISTS`, `FSSAI_REQUIRED`,
`BATCH_*` validation codes.

## Auth (public)
`POST /auth/register` `{name, phone, email, password(8+), role: CUSTOMER|COOK, locality?}` → `{accessToken, refreshToken, user}`
`POST /auth/login` `{email, password}` · `POST /auth/refresh` `{refreshToken}` · `GET|PUT /users/me`
- `POST /auth/forgot-password` `{email}` → generic confirmation and (when enabled for local/manual delivery) `resetToken`.
- `POST /auth/reset-password` `{token, newPassword(8+)}` → `204`; tokens expire after 30 minutes and work once.
  The database stores only a token hash. The raw token is returned only in local and test profiles; production hides it.
  Email delivery is not implemented, so production needs a delivery provider before users can complete this flow.
  Resetting a password invalidates refresh tokens; already-issued access tokens expire normally.

## Discovery (public)
- `GET /cooks?q&cuisine&category&vegetarian&minRating&availableToday&locality&city&lat&lng&sort=rating|newest|popular&page&size`
- `GET /cooks/{id}` (adds `favorite` when logged in) · `GET /cooks/{id}/dishes` · `GET /cooks/{id}/batches` · `GET /cooks/{id}/reviews`
- `GET /dishes/{id}` · `GET /cuisines` · `GET /batches?today&limited&limit`

## Customer
- Cart: `GET /cart`, `POST /cart/items {dishId,batchId,quantity}`, `PUT /cart/items/{id} {quantity}`, `DELETE /cart/items/{id}`, `DELETE /cart`
- Orders: `POST /orders {orderType: PICKUP|COOK_DELIVERY|THIRD_PARTY_DELIVERY, deliveryAddress?}`, `GET /orders`, `GET /orders/{id}`, `POST /orders/{id}/cancel`
- With `PAYMENT_PROVIDER=razorpay`, order creation creates a pending Razorpay order and returns `paymentCheckout` (`keyId`, `orderId`, `amount` in paise, `currency`). Complete Razorpay Checkout, then post `{razorpayOrderId, razorpayPaymentId, razorpaySignature}` to `POST /orders/{id}/payment/verify`. The server verifies the signature before marking the order paid. Configure `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` on the backend; never expose the secret to the frontend. Production defaults to Razorpay and requires both credentials; local development and tests default to the mock provider.
- Review: `POST /orders/{id}/review {rating, tasteRating?, hygieneRating?, packagingRating?, reliabilityRating?, comment?}`
- Favorites: `POST|DELETE /cooks/{id}/favorite`, `GET /favorites`
- `GET /notifications`, `POST /notifications/{id}/read` (any logged-in user)
- `POST /files?folder=profiles|dishes|kitchens|documents` multipart `file` (JPEG/PNG/WebP/PDF, ≤ 5 MB) → `{url}`

## Cook
- `GET|PUT /cook/profile`, `POST /cook/documents`, `GET /cook/dashboard`, `GET /cook/earnings`
- `GET|POST /cook/dishes`, `PUT|DELETE /cook/dishes/{id}` (DELETE deactivates)
- `GET|POST /cook/batches`, `PUT /cook/batches/{id}`
- `GET /cook/orders?status=A,B`, `GET /cook/orders/{id}`
- `POST /cook/orders/{id}/confirm | prepare | ready | dispatch | handover | complete`

## Admin
`GET /admin/dashboard`, `GET /admin/cooks?status`, `GET /admin/cooks/pending`, `GET /admin/cooks/{id}`,
`POST /admin/cooks/{id}/approve`, `POST /admin/cooks/{id}/reject {reason}`, `GET /admin/orders?status`, `GET /admin/users?role`
- Delivery areas: `GET|POST /admin/delivery-areas`, `PUT /admin/delivery-areas/{id}/status {status: DELIVERING|NOT_STARTED}`,
  `DELETE /admin/delivery-areas/{id}`. A new place includes its Google `placeId`, name, formatted address, locality, city,
  coordinates, and status.

Lists return `{content, page, size, totalElements, totalPages}`.
