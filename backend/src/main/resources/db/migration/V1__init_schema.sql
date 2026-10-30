-- CUOQO base schema. All schema changes go through Flyway migrations.
-- Money is NUMERIC(10,2). Status columns are stored as strings mapped to Java enums and guarded by CHECK constraints.

CREATE TABLE users (
    id            UUID PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    phone         VARCHAR(20)  NOT NULL,
    email         VARCHAR(200) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('CUSTOMER', 'COOK', 'ADMIN')),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    address       VARCHAR(500),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX uq_users_email ON users (LOWER(email));

CREATE TABLE cook (
    id                 UUID PRIMARY KEY,
    user_id            UUID NOT NULL UNIQUE REFERENCES users (id),
    headline           VARCHAR(200),
    bio                TEXT,
    years_of_cooking   INT CHECK (years_of_cooking >= 0),
    locality           VARCHAR(120),
    city               VARCHAR(80) NOT NULL DEFAULT 'Pune',
    profile_photo_url  VARCHAR(500),
    signature_dishes   VARCHAR(500),
    approval_status    VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    rejection_reason   VARCHAR(500),
    rating             NUMERIC(3, 2) NOT NULL DEFAULT 0 CHECK (rating >= 0 AND rating <= 5),
    review_count       INT NOT NULL DEFAULT 0 CHECK (review_count >= 0),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_cook_locality ON cook (locality);
CREATE INDEX idx_cook_approval_status ON cook (approval_status);

CREATE TABLE kitchen (
    id                  UUID PRIMARY KEY,
    cook_id             UUID NOT NULL UNIQUE REFERENCES cook (id),
    address             VARCHAR(500),
    locality            VARCHAR(120),
    latitude            DOUBLE PRECISION CHECK (latitude BETWEEN -90 AND 90),
    longitude           DOUBLE PRECISION CHECK (longitude BETWEEN -180 AND 180),
    description         TEXT,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (verification_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE kitchen_photo (
    kitchen_id UUID NOT NULL REFERENCES kitchen (id) ON DELETE CASCADE,
    url        VARCHAR(500) NOT NULL
);
CREATE INDEX idx_kitchen_photo_kitchen ON kitchen_photo (kitchen_id);

CREATE TABLE cook_document (
    id              UUID PRIMARY KEY,
    cook_id         UUID NOT NULL REFERENCES cook (id),
    document_type   VARCHAR(30) NOT NULL CHECK (document_type IN ('FSSAI', 'ID_PROOF', 'OTHER')),
    document_number VARCHAR(100),
    document_url    VARCHAR(500),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    expiry_date     DATE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_cook_document_cook ON cook_document (cook_id);

CREATE TABLE cuisine (
    id   UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE cook_cuisine (
    cook_id    UUID NOT NULL REFERENCES cook (id) ON DELETE CASCADE,
    cuisine_id UUID NOT NULL REFERENCES cuisine (id),
    PRIMARY KEY (cook_id, cuisine_id)
);

CREATE TABLE dish (
    id                       UUID PRIMARY KEY,
    cook_id                  UUID NOT NULL REFERENCES cook (id),
    cuisine_id               UUID REFERENCES cuisine (id),
    name                     VARCHAR(150) NOT NULL,
    description              TEXT,
    category                 VARCHAR(20) NOT NULL CHECK (category IN ('MEAL', 'THALI', 'SNACK', 'SWEET', 'BAKERY', 'BEVERAGE', 'OTHER')),
    price                    NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    portion_description      VARCHAR(120),
    preparation_time_minutes INT CHECK (preparation_time_minutes >= 0),
    ingredients              TEXT,
    allergens                VARCHAR(300),
    image_url                VARCHAR(500),
    vegetarian               BOOLEAN NOT NULL DEFAULT TRUE,
    active                   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_dish_cook_id ON dish (cook_id);

CREATE TABLE batch (
    id                UUID PRIMARY KEY,
    cook_id           UUID NOT NULL REFERENCES cook (id),
    name              VARCHAR(150) NOT NULL,
    description       TEXT,
    available_date    DATE NOT NULL,
    order_cutoff      TIMESTAMP WITH TIME ZONE NOT NULL,
    capacity          INT NOT NULL CHECK (capacity > 0),
    reserved_quantity INT NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    pickup_start      TIME,
    pickup_end        TIME,
    delivery_start    TIME,
    delivery_end      TIME,
    status            VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED', 'CANCELLED')),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_batch_reserved_within_capacity CHECK (reserved_quantity <= capacity)
);
CREATE INDEX idx_batch_available_date ON batch (available_date);
CREATE INDEX idx_batch_status ON batch (status);
CREATE INDEX idx_batch_cook_id ON batch (cook_id);

CREATE TABLE batch_dish (
    batch_id UUID NOT NULL REFERENCES batch (id) ON DELETE CASCADE,
    dish_id  UUID NOT NULL REFERENCES dish (id),
    price    NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    PRIMARY KEY (batch_id, dish_id)
);

CREATE TABLE cart (
    id          UUID PRIMARY KEY,
    customer_id UUID NOT NULL UNIQUE REFERENCES users (id),
    cook_id     UUID REFERENCES cook (id),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE cart_item (
    id         UUID PRIMARY KEY,
    cart_id    UUID NOT NULL REFERENCES cart (id) ON DELETE CASCADE,
    dish_id    UUID NOT NULL REFERENCES dish (id),
    batch_id   UUID NOT NULL REFERENCES batch (id),
    quantity   INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(10, 2) NOT NULL CHECK (unit_price >= 0),
    CONSTRAINT uq_cart_item UNIQUE (cart_id, dish_id, batch_id)
);

CREATE SEQUENCE order_number_seq START WITH 10001 INCREMENT BY 1;

CREATE TABLE orders (
    id               UUID PRIMARY KEY,
    order_number     VARCHAR(20) NOT NULL UNIQUE,
    customer_id      UUID NOT NULL REFERENCES users (id),
    cook_id          UUID NOT NULL REFERENCES cook (id),
    subtotal         NUMERIC(10, 2) NOT NULL CHECK (subtotal >= 0),
    delivery_fee     NUMERIC(10, 2) NOT NULL DEFAULT 0 CHECK (delivery_fee >= 0),
    total_amount     NUMERIC(10, 2) NOT NULL CHECK (total_amount >= 0),
    order_type       VARCHAR(30) NOT NULL CHECK (order_type IN ('PICKUP', 'COOK_DELIVERY', 'THIRD_PARTY_DELIVERY')),
    payment_status   VARCHAR(30) NOT NULL CHECK (payment_status IN ('PENDING', 'AUTHORIZED', 'PAID', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED')),
    order_status     VARCHAR(30) NOT NULL CHECK (order_status IN ('CREATED', 'CONFIRMED', 'PREPARING', 'READY', 'OUT_FOR_DELIVERY', 'READY_FOR_PICKUP', 'DELIVERED', 'PICKED_UP', 'COMPLETED', 'CANCELLED')),
    delivery_address VARCHAR(500),
    delivery_provider VARCHAR(50),
    delivery_order_id VARCHAR(100),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_orders_customer_id ON orders (customer_id);
CREATE INDEX idx_orders_cook_id ON orders (cook_id);
CREATE INDEX idx_orders_status ON orders (order_status);

CREATE TABLE order_item (
    id                 UUID PRIMARY KEY,
    order_id           UUID NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    dish_id            UUID NOT NULL REFERENCES dish (id),
    batch_id           UUID NOT NULL REFERENCES batch (id),
    dish_name_snapshot VARCHAR(150) NOT NULL,
    unit_price         NUMERIC(10, 2) NOT NULL CHECK (unit_price >= 0),
    quantity           INT NOT NULL CHECK (quantity > 0),
    total_price        NUMERIC(10, 2) NOT NULL CHECK (total_price >= 0)
);
CREATE INDEX idx_order_item_order ON order_item (order_id);
CREATE INDEX idx_order_item_batch ON order_item (batch_id);

CREATE TABLE payment (
    id                  UUID PRIMARY KEY,
    order_id            UUID NOT NULL REFERENCES orders (id),
    provider            VARCHAR(50) NOT NULL,
    provider_payment_id VARCHAR(100),
    amount              NUMERIC(10, 2) NOT NULL CHECK (amount >= 0),
    status              VARCHAR(30) NOT NULL CHECK (status IN ('PENDING', 'AUTHORIZED', 'PAID', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED')),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_payment_order ON payment (order_id);

CREATE TABLE delivery_order (
    id                UUID PRIMARY KEY,
    order_id          UUID NOT NULL REFERENCES orders (id),
    provider          VARCHAR(50) NOT NULL,
    provider_order_id VARCHAR(100),
    pickup_address    VARCHAR(500),
    drop_address      VARCHAR(500),
    status            VARCHAR(30) NOT NULL CHECK (status IN ('REQUESTED', 'ASSIGNED', 'PICKED_UP', 'DELIVERED', 'CANCELLED', 'FAILED')),
    tracking_url      VARCHAR(500),
    quoted_cost       NUMERIC(10, 2) CHECK (quoted_cost >= 0),
    actual_cost       NUMERIC(10, 2) CHECK (actual_cost >= 0),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_delivery_order_order ON delivery_order (order_id);

CREATE TABLE review (
    id                 UUID PRIMARY KEY,
    order_id           UUID NOT NULL REFERENCES orders (id),
    customer_id        UUID NOT NULL REFERENCES users (id),
    cook_id            UUID NOT NULL REFERENCES cook (id),
    rating             INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    taste_rating       INT CHECK (taste_rating BETWEEN 1 AND 5),
    hygiene_rating     INT CHECK (hygiene_rating BETWEEN 1 AND 5),
    packaging_rating   INT CHECK (packaging_rating BETWEEN 1 AND 5),
    reliability_rating INT CHECK (reliability_rating BETWEEN 1 AND 5),
    comment            TEXT,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_review_order_customer UNIQUE (order_id, customer_id)
);
CREATE INDEX idx_review_cook_id ON review (cook_id);

CREATE TABLE favorite_cook (
    customer_id UUID NOT NULL REFERENCES users (id),
    cook_id     UUID NOT NULL REFERENCES cook (id),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (customer_id, cook_id)
);

CREATE TABLE notification (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users (id),
    type       VARCHAR(40) NOT NULL,
    title      VARCHAR(200) NOT NULL,
    message    VARCHAR(1000) NOT NULL,
    read       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_notification_user ON notification (user_id, created_at);
