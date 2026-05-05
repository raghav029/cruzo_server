# Car Booking Server — Architecture Guide

> **This is the single source of truth for architecture decisions.**
> When in doubt, follow this document. If something in the code contradicts this guide, the guide wins — fix the code.

---

## What Kind of App Is This

A white-label multi-tenant SaaS. Every resource belongs to a tenant. The app is a **modular monolith** — all modules live in one deployable JAR but are organized as if they were separate services. Do not move to microservices until module boundaries are stable.

---

## The Golden Rules (Never Break These)

1. Controllers only do: receive request → validate → call one service method → return response. No `if`, no `repository`, no business logic.
2. All business logic lives in `application/` services. One class per use-case.
3. Files stay under ~200 lines. If a service grows past that, split it into more use-case classes.
4. Never expose JPA entities through the API. Controllers speak DTOs only.
5. `tenantId` always comes from the JWT, never from the request body or URL.
6. Every write that changes a booking status must append a `BookingStatusHistory` row in the same transaction.
7. Delete the old file when you move a class. Never keep both the old and new versions alive.
8. `common/`, `config/`, `security/` are global shared infrastructure — never move them into a module.

---

## Package Layout

```
com.carbooking/
│
├── CarBookingApplication.java
│
├── config/          ← Spring beans (SecurityConfig, AuditConfig, OpenApiConfig)
├── security/        ← JWT only (JwtTokenProvider, JwtAuthenticationFilter, UserPrincipal)
├── common/
│   ├── enums/       ← All enums shared across modules
│   ├── exception/   ← Custom exceptions + GlobalExceptionHandler
│   ├── response/    ← ApiResponse<T>, PagedResponse<T>
│   └── util/        ← AuditableEntity, SecurityUtils, TenantSupport (see below)
│
└── modules/
    ├── booking/
    ├── fleet/       ← vehicles + drivers
    ├── client/      ← corporate clients + employees
    ├── dailytrip/
    ├── invoice/
    ├── reporting/
    ├── tenant/
    ├── auth/
    └── notification/
```

**Shared `entity/` and `repository/` at root are the interim state.** As each module is migrated, its JPA entity moves to `modules/<feature>/infrastructure/persistence/` and its Spring Data repo moves alongside it. Until then, modules may reference the root entity/repository packages — that is expected and fine.

---

## Module Shape

Every module follows this exact shape. No exceptions.

```
modules/<feature>/
├── controller/
│   └── <Feature>Controller.java
│
├── application/
│   ├── <Feature>Service.java          ← thin facade: delegates to use-case classes
│   ├── <Feature>CreationService.java  ← one class per use-case
│   ├── <Feature>QueryService.java
│   └── ...
│
├── domain/
│   ├── model/        ← pure Java objects, zero Spring/JPA annotations
│   ├── repository/   ← Java interfaces (ports), no Spring Data here
│   └── strategy/     ← pluggable algorithms (e.g. fare calculation)
│
├── infrastructure/
│   ├── persistence/  ← JPA entities + Spring Data repos for this module
│   └── external/     ← third-party API clients (maps, SMS, etc.)
│
└── dto/
    ├── request/
    └── response/
```

**The facade service** (`<Feature>Service.java`) is optional but recommended for complex modules like booking. It keeps the controller simple: the controller talks to one class, and that class delegates to the right use-case service. For simple modules (e.g. PricingConfig), skip the facade and have the controller call the use-case service directly.

**No interface + impl pairs.** In a monolith with a single implementation, `BookingService` + `BookingServiceImpl` is noise. Use a concrete class. The `domain/repository/` port interfaces are the only exception — those are interfaces by design (to invert the dependency from application → infrastructure).

---

## Naming Conventions

| Type | Example |
|---|---|
| Controller | `BookingController` |
| Facade service | `BookingService` |
| Use-case service | `BookingCreationService`, `BookingAssignmentService` |
| JPA Entity | `BookingEntity` (or `Booking` in interim flat packages) |
| Domain model | `BookingRecord` (pure Java, introduced during domain extraction) |
| Request DTO | `CreateBookingRequest` |
| Response DTO | `BookingResponse` |
| Mapper | `BookingMapper` |
| Repository (port) | `BookingRepository` (interface in `domain/repository/`) |
| Repository (impl) | `JpaBookingRepository` (Spring Data, in `infrastructure/persistence/`) |

---

## Helper / Wrapper Classes (Use These — Don't Reinvent)

These classes exist in `common/util/` to remove boilerplate that every module repeats.

### `TenantSupport.java`
Extend this in any service that touches tenant-scoped data. Provides the tenant guard in one line.

```java
// Usage inside a service that extends TenantSupport:
Tenant tenant = requireTenant();                    // resolves + null-checks in one call
assertSameTenant(entity.getTenant().getId());       // throws UnauthorizedException on mismatch
UUID tenantId = currentTenantId();                  // raw UUID when you just need the id
```

```java
// common/util/TenantSupport.java
@RequiredArgsConstructor
public abstract class TenantSupport {

    protected final TenantRepository tenantRepository;

    protected Tenant requireTenant() {
        UUID id = SecurityUtils.getCurrentTenantId();
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    }

    protected void assertSameTenant(UUID entityTenantId) {
        if (!entityTenantId.equals(SecurityUtils.getCurrentTenantId())) {
            throw new UnauthorizedException("Access denied");
        }
    }

    protected UUID currentTenantId() {
        return SecurityUtils.getCurrentTenantId();
    }
}
```

### `BookingHistoryHelper.java`
Every booking status transition must append a history row. Call this helper instead of copy-pasting the builder.

```java
// Usage:
historyHelper.append(booking, fromStatus, toStatus, actorUser, reason);
historyHelper.append(booking, fromStatus, toStatus, actorUser, null);   // no reason
```

```java
// modules/booking/application/BookingHistoryHelper.java
@Component
@RequiredArgsConstructor
public class BookingHistoryHelper {

    private final BookingStatusHistoryRepository historyRepository;

    public void append(Booking booking, BookingStatus from, BookingStatus to, User actor, String reason) {
        historyRepository.save(BookingStatusHistory.builder()
                .tenant(booking.getTenant())
                .booking(booking)
                .fromStatus(from)
                .toStatus(to)
                .actor(actor)
                .reason(reason)
                .transitionedAt(Instant.now())
                .build());
    }
}
```

### `ApiResponse<T>` — already exists in `common/response/`
Always use the static factory methods. Never construct it directly.

```java
ApiResponse.ok(data)              // 200 with body
ApiResponse.ok("message", data)   // 200 with message + body
ApiResponse.error("message")      // used by GlobalExceptionHandler only
```

### `PagedResponse<T>` — already exists in `common/response/`
Wrap any Spring Data `Page<T>` before returning it.

```java
Page<BookingResponse> page = bookingQueryService.list(status, pageable);
return ResponseEntity.ok(ApiResponse.ok(new PagedResponse<>(page)));
```

### `ResponseHelper.java` (controller layer convenience)
For controllers, one helper eliminates the `ResponseEntity.status(...).body(ApiResponse.ok(...))` boilerplate.

```java
// Usage in controller:
return ResponseHelper.ok(service.get(id));
return ResponseHelper.created(service.create(request));
return ResponseHelper.noContent();
```

```java
// common/util/ResponseHelper.java
public final class ResponseHelper {

    private ResponseHelper() {}

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(ApiResponse.ok(message, data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(data));
    }

    public static ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }
}
```

---

## Tenant Isolation Pattern

Every service method that reads or writes tenant-scoped data must follow this pattern exactly.

```java
// 1. Resolve the tenant (for writes that create new records)
Tenant tenant = requireTenant();

// 2. Load + guard (for reads/updates on existing records)
Booking booking = bookingRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
assertSameTenant(booking.getTenant().getId());

// 3. SUPER_ADMIN bypass — SecurityUtils.getCurrentRole() returns "SUPER_ADMIN"
//    TenantSupport.assertSameTenant() checks getCurrentTenantId() which is null for SA
//    → SA bypasses all checks automatically, no extra code needed
```

---

## Controller Pattern

```java
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> create(
            @Valid @RequestBody CreateBookingRequest request) {
        return ResponseHelper.created(bookingService.create(request));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> list(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseHelper.ok(new PagedResponse<>(bookingService.list(status, PageRequest.of(page, size))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> get(@PathVariable UUID id) {
        return ResponseHelper.ok(bookingService.get(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        bookingService.delete(id);
        return ResponseHelper.noContent();
    }
}
```

---

## Service Pattern

```java
@Service
@RequiredArgsConstructor
public class BookingCreationService extends TenantSupport {

    private final BookingRepository bookingRepository;
    private final BookingHistoryHelper historyHelper;
    private final NotificationService notificationService;
    // TenantRepository injected via TenantSupport constructor

    @Transactional
    public BookingResponse create(CreateBookingRequest request) {
        Tenant tenant = requireTenant();

        // business rule
        if (request.getScheduledAt().isBefore(Instant.now().plus(2, ChronoUnit.HOURS))) {
            throw new BusinessRuleException("Booking must be scheduled at least 2 hours in advance");
        }

        Booking booking = Booking.builder()
                .tenant(tenant)
                // ... map fields
                .status(BookingStatus.PENDING_APPROVAL)
                .build();

        booking = bookingRepository.save(booking);
        historyHelper.append(booking, null, BookingStatus.PENDING_APPROVAL, currentUser(), null);
        notificationService.notifyBookingCreated(booking);

        return BookingMapper.INSTANCE.toResponse(booking);
    }
}
```

---

## Booking State Machine (Enforced in Services)

```
PENDING_APPROVAL
  ├─ [CORPORATE_ADMIN approve]  →  APPROVED
  │                                  └─ [FLEET_MANAGER assign / auto-assign]  →  DRIVER_ASSIGNED
  │                                                                                └─ [DRIVER]  →  DRIVER_EN_ROUTE
  │                                                                                               →  ARRIVED
  │                                                                                               →  IN_PROGRESS
  │                                                                                               →  COMPLETED
  └─ [CORPORATE_ADMIN reject]   →  REJECTED

Any state except IN_PROGRESS / COMPLETED:
  →  CANCELLED_BY_EMPLOYEE / CANCELLED_BY_ADMIN / CANCELLED_BY_FLEET_MANAGER / CANCELLED_BY_DRIVER
```

Every transition must:
1. `booking.setStatus(newStatus)`
2. `historyHelper.append(booking, oldStatus, newStatus, actor, reason)`
3. `notificationService.notify*(booking)`
4. Whole method is `@Transactional`

---

## Exception Reference

| Exception | HTTP | When to throw |
|---|---|---|
| `ResourceNotFoundException` | 404 | Entity not found by id |
| `BusinessRuleException` | 422 | Rule violated (2h window, cancellation window, etc.) |
| `DuplicateResourceException` | 409 | Unique constraint would be violated |
| `UnauthorizedException` | 403 | Wrong tenant, wrong role |
| `BadCredentialsException` | 401 | Bad login credentials |

Never throw raw `RuntimeException`. Never catch and swallow exceptions in services. Let `GlobalExceptionHandler` handle everything.

---

## Database Rules

- Flyway owns all DDL. `ddl-auto` is `validate` only.
- New column or table → new migration: `V2__description.sql`, `V3__...`
- Never edit a migration file that has already run.
- All tables: UUID primary key, `created_at`, `updated_at`, `tenant_id` (for tenant-scoped tables).
- Extend `AuditableEntity` for id + audit timestamps.

---

## What Stays Global (Never Moves Into a Module)

| Package | Reason |
|---|---|
| `common/` | Used by every module — exceptions, enums, response wrappers, utils |
| `config/` | Spring application-level configuration |
| `security/` | JWT and authentication filter wires into Spring Security globally |
| `entity/` | Interim — moves to `modules/<feature>/infrastructure/persistence/` during migration |
| `repository/` | Interim — moves alongside its entity |

---

## Migration Order & Status

Migrate one module at a time. When touching a module:
1. Move controller → `modules/<feature>/controller/`
2. Move service(s) → `modules/<feature>/application/`
3. Move DTOs → `modules/<feature>/dto/`
4. Move mapper → `modules/<feature>/infrastructure/persistence/`
5. Apply `TenantSupport`, `BookingHistoryHelper`, `ResponseHelper` to reduce boilerplate
6. **Delete the old files** — never keep both old and new
7. Run tests before committing

| Module | Status | Notes |
|---|---|---|
| `booking` | Partially migrated | `modules/booking/` exists but old `service/booking/` not yet deleted |
| `fleet` (vehicles + drivers) | Not started | In `service/VehicleService`, `service/DriverService` |
| `client` (corp clients + employees) | Not started | In `service/CorporateClientService` etc. |
| `dailytrip` | Not started | In `service/DailyTripService`, `service/DailyScheduleService` |
| `invoice` | Not started | In `service/InvoiceService` |
| `reporting` | Not started | In `service/ReportService` |
| `tenant` | Not started | In `service/TenantService` |
| `auth` | Not started | In `service/AuthService` |
| `notification` | Not started | In `service/NotificationService` |

**Phase 1 (now):** Complete booking migration — delete `service/booking/*`, move booking DTOs into module.
**Phase 2:** Fleet module — VehicleService + DriverService + their DTOs.
**Phase 3:** Client module — CorporateClientService + CorporateEmployeeService + their DTOs.
**Phase 4:** Remaining modules one by one.
**Phase 5 (last):** Domain extraction — introduce pure Java domain models in `domain/model/`, map from JPA entities in infrastructure. Only do this when module boundaries are stable.

---

## File Size Control

| Symptom | Fix |
|---|---|
| Service > 200 lines | Split into additional use-case classes (e.g. `BookingCancellationService`) |
| Controller > 100 lines | Split into multiple controllers by resource or action group |
| Entity > 150 lines | Extract value objects into separate `@Embeddable` classes |
| Mapper > 100 lines | Split by response type |
