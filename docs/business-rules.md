# Business rules

Each rule below is enforced in the backend and covered by an automated test.

| # | Rule | Enforced in | Error code |
|---|------|-------------|-----------|
| 1 | Only approved cooks can publish food (dishes, batches) | `CookService.requireApprovedCook` | `COOK_NOT_APPROVED` (403) |
| 2 | A customer cannot order an inactive dish | `CartService`, `OrderService` | `DISH_INACTIVE` |
| 3 | A sold-out batch cannot be ordered | atomic reserve | `BATCH_SOLD_OUT` (409) |
| 4 | An order cannot exceed remaining capacity (capacity 10, reserved 8, order 3 → fails) | atomic reserve | `BATCH_CAPACITY_EXCEEDED` (409) |
| 5 | A cart/order belongs to exactly one cook | `CartService` | `CART_OTHER_COOK` (409) |
| 6 | Only COMPLETED orders can be reviewed | `ReviewService` | `ORDER_NOT_COMPLETED` |
| 7 | One review per order | unique `(order_id, customer_id)` + check | `REVIEW_ALREADY_EXISTS` |
| 8 | A cook cannot approve their own profile | role check (`ROLE_ADMIN`) + explicit guard | 403 |
| 9 | Only admins approve/reject cooks | `/api/v1/admin/**` | 403 |
| 10 | A cook modifies only their own dishes/batches | ownership check | 403 |
| 11 | A customer sees only their own orders | repository queries by owner | 404 |
| 12 | A cook sees only orders for their kitchen | repository queries by owner | 404 |
| 13 | Cancelled orders cannot move to PREPARING (or anywhere) | `OrderStatus` | `INVALID_ORDER_TRANSITION` |
| 14 | Completed orders cannot be cancelled; only CREATED can | `OrderStatus` | `ORDER_NOT_CANCELLABLE` |
| 15 | Prices are snapshotted into `order_item` | `OrderService.create` | – |
| 16 | Changing a dish price never changes historical orders | snapshot | – |

## More rules

- **Ordering window:** a batch accepts orders only while `status = OPEN`, before `order_cutoff`, and with capacity left.
  The cut-off must be on or before the batch day; capacity cannot be reduced below what is already reserved.
- **Fulfilment:** `PICKUP` needs a pickup window on the batch; delivery types need a delivery window and an address.
  Fees: pickup free, cook delivery `gharse.order.cook-delivery-fee` (₹30), courier = provider quote (mock ₹60).
- **Cancellation** releases the reserved portions and refunds the payment.
- **Cook approval:** new cooks are `PENDING`. A rejected cook who updates their profile goes back to `PENDING`.
  FSSAI is optional unless `gharse.cook.fssai-required=true`.
- **Visibility:** only `APPROVED` cooks, and only their *active* dishes, appear publicly. The kitchen address is never in
  public responses; pickup customers get it after the cook confirms.
- **Cart:** one per customer, single cook. The client offers "Clear Cart / Cancel" when it receives `CART_OTHER_COOK`.
- **Order lifecycle:**
  `CREATED → CONFIRMED → PREPARING → READY → (OUT_FOR_DELIVERY → DELIVERED | READY_FOR_PICKUP → PICKED_UP) → COMPLETED`;
  `CREATED → CANCELLED`. `POST /cook/orders/{id}/complete` accepts any status from READY onward and walks the remaining
  states so the lifecycle stays valid.
- **Ratings:** overall `rating` 1–5 is required, taste/hygiene/packaging/reliability are optional. A cook's rating is the
  average of overall ratings, rounded to two decimals.
- **Earnings:** completed orders; item subtotal, plus the delivery fee when the cook delivered it themselves. No
  platform commission in the MVP.
