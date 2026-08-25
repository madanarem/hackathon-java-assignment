# Testing Strategy

This document describes how this codebase is tested, how to run the tests and the
coverage report, and a real tooling limitation encountered (and worked around) while
wiring up JaCoCo for this specific Quarkus version.

## Layers

Tests are organized in three layers, mirroring the hexagonal architecture:

1. **Pure unit tests** (`*Test.java` for domain-only classes, `*UnitTest.java` where a
   `@QuarkusTest` class of the same base name already exists) - plain JUnit 5, no Quarkus
   context. Ports (`WarehouseStore`, `LocationResolver`, `LegacyStoreManagerGateway`, ...)
   are mocked with Mockito. These cover the domain use cases
   (`CreateWarehouseUseCase`, `ArchiveWarehouseUseCase`, `ReplaceWarehouseUseCase`,
   `SearchWarehousesUseCase`), the REST-layer mapping/error-translation logic in
   `WarehouseResourceImpl`, `StoreEventObserver`, `LegacyStoreManagerGateway`, and simple
   entity/mapping classes (`DbWarehouse#toWarehouse()`, `Product`, `Store`).
   Run: `./mvnw test -Dtest=*UnitTest,CreateWarehouseUseCaseTest,DbWarehouseTest,...`

2. **`@QuarkusTest` integration tests** (`*Test.java`, `*IT.java` for concurrency/DB
   specifics) - exercise the same business rules end-to-end against a real (H2, in
   `%test` profile) database, plus REST endpoints via RestAssured, CDI eventing, and
   transaction boundaries. These are the tests that actually prove the wiring (Panache
   queries, `@Transactional` boundaries, CDI event observers, optimistic locking) works,
   which the pure unit tests above cannot.

3. **Explicit-run integration tests** (`WarehouseConcurrencyIT`, `WarehouseTestcontainersIT`)
   - not included in the default `./mvnw test` run (see `CODE_ASSIGNMENT.md`); run them
   with `./mvnw test -Dtest=WarehouseConcurrencyIT,WarehouseTestcontainersIT`.

Each use case and REST endpoint is tested for:
- **Positive** cases (valid input, expected side effects, 2xx responses).
- **Negative** cases (business-rule violations: duplicate codes, invalid locations,
  capacity/stock limits, archived-warehouse mutation, missing/invalid required fields
  -> `IllegalArgumentException` mapped to `400`; not-found -> `404`; malformed request
  bodies -> `422`).
- **Error/edge** cases (concurrent modification -> lost-update prevention or
  `OptimisticLockException`; transaction rollback -> legacy system never notified; null
  required fields).

## Running

```bash
./mvnw clean test                 # full unit + default integration suite (113+ tests)
./mvnw test -Dtest=WarehouseConcurrencyIT,WarehouseTestcontainersIT
./mvnw clean verify               # also runs the JaCoCo coverage check (see below)
```

As of this pass: **115 tests, 0 failures, 0 errors** across 23 test classes (full suite
including the two explicitly-run IT classes).

## Coverage (JaCoCo)

`pom.xml` wires `jacoco-maven-plugin`: `prepare-agent` (instrumentation), `report` (bound
to the `test` phase, HTML/CSV/XML under `target/site/jacoco/`), and `check` (bound to
`verify`, enforcing **80% line coverage** as a build-breaking gate). Current result on
the scoped bundle (see caveat below): **100% line coverage (233/233 lines), `mvn verify`
passes the gate.**

```bash
./mvnw clean verify
open target/site/jacoco/index.html   # (or just browse the folder on Windows)
```

### Coverage measurement caveat (Quarkus 3.13.x + JaCoCo)

While wiring this up, four classes came back as a **flat, suspicious 0% covered** despite
being exercised by many passing `@QuarkusTest`s: `WarehouseRepository`, `ProductRepository`,
`ProductResource`, `StoreResource`.

Investigation: `@QuarkusTest` runs the app through Quarkus's own `QuarkusClassLoader`,
and normal-scoped CDI beans (`@ApplicationScoped`, `@RequestScoped` - which is exactly
what these four classes are) are resolved through an Arc-generated client proxy. When a
test calls a method through that proxy, JaCoCo's coverage agent cannot correlate the
executed bytecode back to the plain `.class` file it analyzed for the report - so the hit
is silently dropped, regardless of how thoroughly the class is actually tested. This was
confirmed empirically: the exact same use-case classes read as **0% covered when only
exercised via `@QuarkusTest`**, and jumped to **100% covered** the moment a plain
(non-Quarkus) Mockito-based unit test called them directly with `new` - proving the gap is
in the *measurement*, not the tests.

The proper long-term fix is `io.quarkus:quarkus-jacoco`, a dedicated extension built to
solve exactly this - but it's only published starting at Quarkus **3.15.2**, and this
project is pinned to **3.13.3** (per `CODE_ASSIGNMENT.md`/`README.md`). Bumping the
platform version to pull it in was deliberately not done here: it would also require
re-validating the separately-versioned `quarkus-openapi-generator-server` extension
against the new platform version, which is a real compatibility risk not worth taking on
just to fix a coverage-reporting artifact in an otherwise fully green build.

Given that, `pom.xml`'s JaCoCo `check` rule **excludes** exactly those four classes from
the enforced 80% ratio - not because they're untested, but because this toolchain cannot
measure them. Each is still covered behaviorally:

| Class | Behaviorally covered by |
|---|---|
| `WarehouseRepository` | `ArchiveWarehouseUseCaseTest`, `ReplaceWarehouseUseCaseTest`, `WarehouseValidationTest`, `WarehouseSearchResourceTest`, `WarehouseConcurrencyIT`, `WarehouseTestcontainersIT` |
| `ProductRepository` | `ProductEndpointTest` |
| `ProductResource` | `ProductEndpointTest` (CRUD, 404, 422 cases) |
| `StoreResource` | `StoreResourceTest` (CRUD, 404, 422 cases), `StoreTransactionIntegrationTest` |

If/when the platform is upgraded to Quarkus >= 3.15, adding `io.quarkus:quarkus-jacoco`
(test scope) and removing these four exclusions is the correct follow-up - see the
extension's own guide for the `jacoco-maven-plugin` vs. `quarkus-jacoco` interplay (they
instrument the same classes differently and need either exec-file merging or picking one).
