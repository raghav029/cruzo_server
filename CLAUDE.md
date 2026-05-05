# Car Booking Server — Project Context

## What This Is
White-label corporate car booking SaaS. Fleet managers buy this software to manage their fleet, drivers, and corporate clients. Built as a Spring Boot monolith, sold to multiple operators (multi-tenant).

Full PRD: `/Users/raghavjha/Desktop/PROJECT/CAR_BOOKING/docs/PRD.md`
API Spec: `/Users/raghavjha/Desktop/PROJECT/CAR_BOOKING/docs/API_SPEC.md`
DB Schema: `/Users/raghavjha/Desktop/PROJECT/CAR_BOOKING/docs/schema.sql`

---

## Tech Stack
- **Java 17** + **Spring Boot 3.3.4**
- **PostgreSQL** — single database, all tenants share it (isolated by `tenant_id`)
- **Flyway** — all schema changes go in `src/main/resources/db/migration/V{n}__description.sql`
- **Spring Security + JWT** (jjwt 0.12.5) — stateless, no sessions
- **Lombok** — use `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` on entities/DTOs
- **MapStruct** — entity ↔ DTO mapping (add mappers in `com.carbooking.mapper`)
- **Springdoc** — Swagger UI at `/swagger-ui.html`

---

## Package Structure
```
com.carbooking/
├── CarBookingApplication.java
├── config/          # Spring beans: SecurityConfig, AuditConfig
├── security/        # JWT: JwtTokenProvider, JwtAuthenticationFilter, UserPrincipal
├── common/
│   ├── enums/       # All enums: Role, BookingStatus, VehicleType, etc.
│   ├── exception/   # Custom exceptions + GlobalExceptionHandler
│   ├── response/    # ApiResponse<T>, PagedResponse<T>
│   └── util/        # AuditableEntity (base class), SecurityUtils
├── entity/          # JPA entities — one per DB table, no business logic
├── repository/      # Spring Data JPA interfaces — queries only
├── dto/
│   ├── request/     # One class per API write operation
│   └── response/    # One class per resource returned
├── mapper/          # MapStruct interfaces
├── service/         # All business logic, @Transactional here
└── controller/
    ├── admin/       # /api/admin/** — SUPER_ADMIN only
    └── tenant/      # /api/** — all tenant-scoped endpoints
```

---

## Actors & Roles
| Role | Description |
|---|---|
| `SUPER_ADMIN` | Platform owner (us). `tenant_id = null` in JWT. Manages all tenants. |
| `FLEET_MANAGER` | Buys the software. Manages fleet, drivers, clients. |
| `CORPORATE_ADMIN` | Company admin. Approves bookings, views invoices. |
| `EMPLOYEE` | Books rides. |
| `DRIVER` | Executes trips. Updates booking status. |

**One user = one role. No multi-role accounts.**

---

## Multi-Tenancy Rules
- Every tenant-scoped table has `tenant_id UUID NOT NULL`
- `tenantId` is **always extracted from the JWT** — never from request body or URL
- Every service method that touches tenant data must verify: `entity.tenantId == JWT.tenantId`
- Throw `UnauthorizedException` (→ 403) on mismatch
- `SUPER_ADMIN` has `tenantId = null` in JWT and bypasses tenant checks

```java
// Standard tenant isolation guard (copy this pattern in every service):
UUID tenantId = SecurityUtils.getCurrentTenantId();
Entity entity = repository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("X not found"));
if (!entity.getTenant().getId().equals(tenantId)) {
    throw new UnauthorizedException("Access denied");
}
```

---

## API Response Pattern
All endpoints return `ApiResponse<T>` or `ApiResponse<PagedResponse<T>>`.

```java
// Success with data
return ResponseEntity.ok(ApiResponse.ok(data));

// Success with message
return ResponseEntity.ok(ApiResponse.ok("Driver assigned successfully", data));

// Created
return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(data));

// No content (DELETE)
return ResponseEntity.noContent().build();
```

Errors are handled by `GlobalExceptionHandler` — never return raw error strings from controllers.

---

## Booking State Machine
```
PENDING_APPROVAL
  ├─[CORPORATE_ADMIN approve] ──► APPROVED
  │                                 └─[FLEET_MANAGER assign] ──► DRIVER_ASSIGNED
  │                                                                   └─[DRIVER] ──► DRIVER_EN_ROUTE
  │                                                                                      └─► ARRIVED
  │                                                                                            └─► IN_PROGRESS
  │                                                                                                  └─► COMPLETED
  └─[CORPORATE_ADMIN reject] ──► REJECTED

Any state except IN_PROGRESS / COMPLETED → CANCELLED_BY_EMPLOYEE / CANCELLED_BY_ADMIN /
                                           CANCELLED_BY_FLEET_MANAGER / CANCELLED_BY_DRIVER
```

Every status transition must:
1. Update `booking.status`
2. Insert a `BookingStatusHistory` row (append-only audit trail)
3. Notify relevant actors via `NotificationService`
4. All wrapped in `@Transactional`

---

## Controller Pattern
Controllers are thin — validate input, call service, return response.

```java
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> create(
            @Valid @RequestBody CreateVehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(vehicleService.create(request)));
    }
}
```

---

## Service Pattern
All business logic lives in services. Always `@Transactional` on write methods.

```java
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final TenantRepository tenantRepository;

    @Transactional
    public VehicleResponse create(CreateVehicleRequest request) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        if (vehicleRepository.existsByTenantAndPlateNumber(tenant, request.getPlateNumber())) {
            throw new DuplicateResourceException("Vehicle with plate " + request.getPlateNumber() + " already exists");
        }

        Vehicle vehicle = Vehicle.builder()
                .tenant(tenant)
                .plateNumber(request.getPlateNumber())
                // ... map fields
                .build();

        return VehicleMapper.INSTANCE.toResponse(vehicleRepository.save(vehicle));
    }
}
```

---

## Entity Pattern
Entities extend `AuditableEntity` (provides `id`, `createdAt`, `updatedAt`). No business logic in entities.

```java
@Entity
@Table(name = "table_name")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MyEntity extends AuditableEntity {
    // fields only
}
```

---

## DTO Pattern
Use separate request/response DTOs. Validation annotations go on request DTOs.

```java
// Request DTO
@Getter @Setter
public class CreateVehicleRequest {
    @NotBlank private String plateNumber;
    @NotNull private VehicleType vehicleType;
    // ...
}

// Response DTO
@Getter @Builder
public class VehicleResponse {
    private UUID id;
    private String plateNumber;
    // ...
}
```

---

## Exception Handling
Use specific exceptions — `GlobalExceptionHandler` maps them to HTTP status:

| Exception | HTTP |
|---|---|
| `ResourceNotFoundException` | 404 |
| `BusinessRuleException` | 422 |
| `DuplicateResourceException` | 409 |
| `UnauthorizedException` | 403 |
| `BadCredentialsException` | 401 |

---

## Database / Flyway Rules
- Never use `ddl-auto: create` or `update` — Flyway owns DDL
- New table or column = new migration file: `V2__add_something.sql`, `V3__...`
- Never edit an existing migration file once it's been run
- All tables have UUID PK, `created_at`, `updated_at`
- All tenant-scoped tables have `tenant_id` FK

---

## Build Order (what's done vs todo)
### Done
- [x] Project structure + pom.xml
- [x] application.yml
- [x] All enums
- [x] AuditableEntity, ApiResponse, PagedResponse
- [x] Custom exceptions + GlobalExceptionHandler
- [x] SecurityUtils
- [x] JWT security layer (JwtTokenProvider, JwtAuthenticationFilter, UserPrincipal)
- [x] SecurityConfig
- [x] All 13 JPA entities
- [x] All 13 repositories
- [x] V1 Flyway migration (full schema)

### Next — build in this order
- [x] Auth module: `AuthService` + `AuthController` → `POST /api/auth/login`
- [x] Tenant module: `TenantService` + `TenantController` → `/api/admin/tenants` CRUD
- [x] Vehicle module
- [x] Driver module
- [x] CorporateClient module
- [x] CorporateEmployee module
- [x] PricingConfig module
- [x] CancellationConfig module
- [x] Booking module (most complex — state machine)
- [x] Invoice module
- [x] Reports module
- [x] NotificationService (MSG91 SMS)
- [x] Final fare calculation (time-based, on COMPLETED transition)
- [x] CORPORATE_ADMIN creation endpoint (POST /api/corporate-clients/{id}/admins)
- [x] Forgot password / change password (POST /api/auth/forgot-password, /api/auth/change-password)
- [x] Swagger/OpenAPI (springdoc, JWT bearer, @Tag on all controllers)

---

## Key Business Rules (enforced in service layer)
- Booking `scheduledAt` must be ≥ `now + 2 hours`
- Cancellation window is configurable per tenant (`CancellationConfig.cancellationWindowHours`)
- After cancellation window: only FLEET_MANAGER can cancel (force cancel)
- Driver assignment: both manual (fleet manager picks) and auto (most-rested available driver)
- Auto-assign picks by: `availability = AVAILABLE` AND matching `vehicleType`, ordered by `updatedAt ASC`
- Invoice line items snapshot fare values at generation time — never recalculate from current config
- SUPER_ADMIN has `tenantId = null` — bypass all tenant isolation checks for SUPER_ADMIN role

---

## Environment Variables Needed to Run
```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=carbooking
DB_USERNAME=postgres
DB_PASSWORD=yourpassword
JWT_SECRET=your-32-plus-character-secret-key-here
```
Optional (for full features):
```
MSG91_AUTH_KEY=...
GOOGLE_MAPS_API_KEY=...
AWS_S3_BUCKET=...
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
```
