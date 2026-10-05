# Future roadmap

**Phase 1 — Basic marketplace (this repository).** Cook onboarding and approval, dishes, batches/pre-orders, cart,
orders, mock payment and delivery, reviews, favorites, admin panel.

**Phase 2 — Payments, delivery, notifications.** Razorpay `PaymentProvider` (webhooks, refunds), Porter/Borzo/Shiprocket
`DeliveryProvider` with tracking, S3-compatible `StorageService`, WhatsApp/SMS/email `NotificationService`, refresh-token
revocation, platform commission and cook payouts.

**Phase 3 — Recommendations and search.** Personalised "for you" ranking, PostgreSQL full-text/trigram search, saved
addresses and geocoding, cook availability calendar, repeat-order shortcuts.

**Phase 4 — AI.** AI Chef Assistant for cooks (descriptions, pricing hints), demand prediction for batch sizing,
natural-language search ("something light and Bengali for Sunday").

**Phase 5 — Multi-city expansion.** City as a first-class concept, per-city ops tooling and delivery partners, multiple
roles per user, localisation.

Explicitly out of scope until there is a real requirement: Kafka, Redis, Kubernetes, microservices, Elasticsearch,
own delivery fleet, subscriptions, loyalty, dynamic pricing, referrals, ads.
