# Architecture

## Shape

A **modular monolith**: one deployable Spring Boot application, one PostgreSQL database, one React SPA.

```
Browser (React SPA, Vercel)
   │  HTTPS, JSON, Bearer JWT
   ▼
Spring Boot (Render)  ──►  PostgreSQL (Supabase)
   │
   └─► StorageService (local disk now, S3-compatible later)
```

## Backend modules (`com.gharse.*`)

| Package | Responsibility |
|---------|----------------|
| `common` | config properties, base entities, error model + global handler, request logging, rate limiter, file upload + storage |
| `auth` | register/login/refresh, JWT, Spring Security config, CORS |
| `user` | `User` entity, `/users/me` |
| `cook` | cook profile, discovery search (JPA Specifications), documents, favorites, cook dashboard/earnings |
| `kitchen` | kitchen entity (address, coordinates, photos, verification) |
| `cuisine` | cuisine catalogue |
| `menu` | dishes |
| `availability` | batches (capacity, cut-off, pickup/delivery windows) and the atomic reservation queries |
| `order` | cart, orders, status state machine, cook order actions |
| `payment` | `PaymentProvider` abstraction + `MockPaymentProvider`, payment records |
| `delivery` | `DeliveryProvider` abstraction + `MockDeliveryProvider`, delivery records |
| `review` | reviews and cook rating aggregation |
| `notification` | `NotificationService` abstraction, database implementation |
| `admin` | dashboard, cook approval, admin lists, bootstrap admin, demo seeder |

Modules talk through services and repositories in the same process. Package dependencies are not enforced yet (no
ArchUnit); keep controllers thin and put rules in services.

## Key design decisions

1. **Batch capacity is atomic.** `BatchRepository.reserve` is a single `UPDATE … SET reserved = reserved + :q WHERE
   reserved + :q <= capacity AND status = 'OPEN' AND cutoff > now`. Zero rows updated means the order is refused. A DB
   `CHECK (reserved_quantity <= capacity)` is the last line of defence, and `Batch` uses `@DynamicUpdate` so editing a
   batch can never overwrite the reserved count with a stale value. A concurrency test covers it.
2. **Capacity is per batch**, counted in portions across all dishes of the batch.
3. **Dishes are ordered through batches.** A cart item always has a dish *and* a batch; the batch carries date, cut-off,
   fulfilment windows and the batch-specific price.
4. **Prices are snapshotted** into `order_item` (name and unit price) at order time.
5. **Provider abstractions**: `PaymentProvider`, `DeliveryProvider`, `StorageService`, `NotificationService`,
   `RateLimiter`. OrderService depends on services, never on a gateway.
6. **Enums everywhere for status**, stored as strings with DB `CHECK` constraints.
7. **Search stays in PostgreSQL**: one `Specification` per filter (cook name, dish, cuisine, locality, category,
   vegetarian, rating, available today). Distance sorting uses haversine in memory when `lat/lng` are supplied.
8. **Stateless auth**: short-lived access JWT + refresh JWT (typed claims, so an access token cannot refresh).
9. **Errors**: one `ApiError` body, no stack traces. Cross-owner access answers `404` so ids cannot be probed.
10. **Observability**: one structured log line per request (request id, user id, method, path, status, duration).
    The `prod` profile switches console logs to ECS JSON.

## Extension points (not implemented)

- `S3StorageService` (env vars `STORAGE_*` reserved), real `PaymentProvider` (Razorpay), real `DeliveryProvider`
  (Porter/Borzo/Shiprocket), WhatsApp/SMS `NotificationService`, Redis-backed `RateLimiter`.
- Multiple roles per user: replace `users.role` with a join table.
- Refresh-token revocation: store token ids in a table and check on refresh.
