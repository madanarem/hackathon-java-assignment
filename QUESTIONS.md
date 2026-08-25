# Questions

Here are 2 questions related to the codebase. There's no right or wrong answer - we want to understand your reasoning.

## Question 1: API Specification Approaches

When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded everything directly. 

What are your thoughts on the pros and cons of each approach? Which would you choose and why?

**Answer:**
```txt
Spec-first (Warehouse, generated from warehouse-openapi.yaml)

Pros:
- The YAML is a single source of truth that's guaranteed to match the wire contract -
  the generated interface won't compile if the implementation drifts from it, so the
  contract can't silently rot the way hand-written docs do.
- The spec is reviewable and shareable independently of the Java code - front-end teams,
  API consumers, and Swagger UI all consume the same artifact.
- It's a natural fit for teams doing contract-first design or consumer-driven contract
  testing, and for generating client SDKs in other languages later.
- It forces you to think about the shape of the API (types, required fields, response
  codes) before writing business logic.

Cons:
- Extra build-time step (code generation) and generated-sources wiring in the IDE - I hit
  this directly while adding the bonus /warehouse/search endpoint: I had to edit the YAML,
  regenerate, and only then could I implement the interface method, versus just adding a
  new method to a hand-coded resource.
- The generator's type mapping is coarse - `type: integer` query params came out as
  `BigInteger` rather than `Integer`, which just adds noisy conversion code in the adapter.
- Harder to do highly dynamic or non-RESTful things (e.g. ad-hoc query params, conditional
  fields) than in a hand-written resource, since you're constrained by what the generator
  supports.
- One more tool in the toolchain to understand, debug and version.

Hand-coded (Product, Store)

Pros:
- Fast to write and change - no regeneration step, no fighting the generator's type
  mapping, full control over validation, error handling and response shape.
- Easier to onboard to for anyone who just knows JAX-RS.

Cons:
- The "spec" is whatever Swagger annotations happen to be inferred from the code (or
  nothing at all) - it's documentation-by-accident rather than a contract, and it's easy
  for behavior to diverge from what's documented since nothing enforces it.
- No compile-time guarantee that the endpoint still matches what consumers expect after a
  refactor.

Which would I choose?

For an API with external or cross-team consumers, and especially one that's expected to
be long-lived and evolve carefully (which is exactly the kind of API a Warehouse
management surface tends to become), I'd default to spec-first/generated, because the
contract-enforcement benefit compounds over the life of the API and outweighs the
generation friction. For small, internal, CRUD-shaped resources like Product and Store
here, hand-coding is a reasonable pragmatic choice - the overhead of a formal spec buys
little when the resource is this simple and low-risk. In practice I'd want the whole
service on one approach rather than split, mainly for consistency: new engineers
shouldn't have to guess which pattern a given resource follows. If I had to pick one for
the whole codebase, I'd lean spec-first and accept the generation overhead, but I'd
watch for the type-mapping and flexibility pain points above and make sure the team has
tooling (fast regeneration, good error messages) to keep the friction low.
```

---

## Question 2: Testing Strategy

Given the need to balance thorough testing with time and resource constraints, how would you prioritize tests for this project? 

Which types of tests (unit, integration, parameterized, etc.) would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
Priority order, and why

1. Domain use-case unit tests (Archive/Replace/Create/Search) - highest priority. This
   is where the business rules live (uniqueness, capacity/stock limits, archive
   semantics), it's cheap to test (no DB required if you mock WarehouseStore/
   LocationResolver), and it's where regressions are most likely as rules change. These
   should run on every commit and be fast enough to run in a tight loop.

2. Parameterized tests for validation edge cases - a natural extension of (1).
   WarehouseValidationTest's @MethodSource pattern is a good model: enumerate boundary
   conditions (capacity == max vs capacity > max, stock == capacity vs stock > capacity,
   empty/unknown location) as data rather than one method per case. This is where most
   real bugs in this kind of system hide, and it scales far better than one-off tests as
   new rules get added.

3. Transaction-boundary / integration tests - next priority, and the highest-value tests
   in this specific codebase given the explicit business rule that the legacy system
   must never be notified of uncommitted data. StoreTransactionIntegrationTest is exactly
   the right shape (drive it through the real HTTP endpoint + real transaction, assert
   the side effect only happens on commit) - this is the kind of bug that a mocked unit
   test cannot catch, since it's about *when* something happens relative to a commit, not
   *whether* the logic is correct in isolation.

4. Concurrency tests - important but lower priority to write, and I'd deliberately keep
   the count small: they're expensive to write correctly (as this assignment
   demonstrated - the existing concurrency ITs were failing for infrastructure reasons,
   not business-logic reasons: no active transaction on raw executor threads, and a test
   asserting on data written in a still-open outer transaction) and slow/flaky by nature.
   I'd keep 2-3 tests that exercise the specific concurrent-write scenarios that matter
   (two updates racing on the same row) rather than a broad matrix, and treat them as a
   smoke test for "does optimistic locking still work", not as a substitute for reasoning
   about correctness statically.

5. Repository/adapter tests against a real database (WarehouseTestcontainersIT-style) -
   worth having a handful of, specifically for the things that only a real DB will show
   you: unique constraints, cascade/DDL truncation, JPQL query correctness (like the
   /search endpoint's filters here). I would not duplicate business-rule tests here since
   layer (1) already covers those more cheaply.

6. Full black-box endpoint tests (@QuarkusIntegrationTest against a packaged artifact,
   like WarehouseEndpointIT) - lowest priority for day-to-day development because they're
   slow and need real infrastructure (this one needs a live Postgres on a fixed port),
   but valuable as a small smoke suite in CI before a release/deploy, not on every commit.

How I'd keep coverage effective over time

- Treat "a bug fixed" as "a test added" - every root cause found (like the two behavioral
  bugs and the import.sql NPE fixed in this pass) gets a regression test in the layer
  closest to where the bug lived, so it can't silently come back.
- Keep the fast layers (1)-(2) large and the slow layers (4)-(6) deliberately small, and
  enforce that split in CI: fail-fast unit/parameterized tests on every push, integration
  tests (including the two explicitly-run IT classes) on every PR merge, and the
  Testcontainers/black-box suite on a nightly or pre-release pipeline so slow
  infrastructure-dependent tests don't block iteration speed.
- Use mutation testing or code coverage as a signal to find untested branches, not as a
  target to hit - a percentage number doesn't tell you whether the *business rules* are
  covered, only whether *lines* are.
- Review new use-case code with "what's the parameterized case list for this?" as a
  standing question, so validation logic keeps growing its data-driven test alongside it
  rather than accumulating one-off tests.
```
