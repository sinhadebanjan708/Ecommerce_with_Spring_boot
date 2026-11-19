# Elvero Shop (Spring Boot)

Java 21 / Spring Boot 3.5 / Maven port of the Node.js ecommerce backend.

## Required environment variables
`JWT_SECRET`, `JWT_REFRESH_SECRET` (the same values the Node app uses, so existing tokens stay valid),
`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, and for OTP emails
`EMAIL_HOST`, `EMAIL_PORT`, `EMAIL_USER`, `EMAIL_PASS` (`EMAIL_SSL=true`, `EMAIL_STARTTLS=false` for port 465).

## Run
```
export DB_HOST=localhost DB_PORT=3306 DB_NAME=ecommerce_db DB_USER=root DB_PASSWORD=...
mvn spring-boot:run
```
Defaults: port 5000 (same as the Node app), uploads served from `./uploads` at `/uploads/**`.

## Status
- [x] Step 1: project foundation (pom, config, error handling)
- [x] Step 2: JPA entities + repositories for the 37 live tables (AI tables skipped)
- [x] Step 3: security, JWT, OTP auth (`/api/ecommerce/auth/**`)
- [ ] Later: catalog, cart/wishlist/checkout, payments/orders, inventory/coupons

## Notes
- Hibernate never touches the schema (`ddl-auto: none`); Flyway baselines the existing DB.
- Physical naming strategy is `Standard`: every column name is explicit in the entities.
- Audit columns (created_by, updated_by, assigned_by, ...) are plain `Long` fields, not associations.
- `orders.total_amount` and `products.base_price` are `String` because they are `varchar` in the live DB.
