# GitHub Workflow (CI/CD Pipeline)

This document describes the CI/CD pipeline recommended for this project. It's provided
as reference documentation rather than a live `.github/workflows/*.yml` file, so it can
be reviewed and adopted independently of repository/token permissions.

To activate it, create `.github/workflows/ci.yml` with the contents below.

## What it does

On every push and pull request targeting `main`, the pipeline:

1. Checks out the repository.
2. Sets up JDK 17 (Temurin) with Maven dependency caching.
3. Runs the full unit test suite plus the JaCoCo coverage gate (`./mvnw clean verify`,
   which fails the build if line coverage on the scoped bundle drops below 80% — see
   [TESTING.md](TESTING.md) for what's scoped in/out and why).
4. Runs the integration tests that aren't included by default
   (`WarehouseConcurrencyIT`, `WarehouseTestcontainersIT`).
5. Packages the application (`./mvnw package -DskipTests`) to confirm it builds
   end-to-end.
6. Uploads the Surefire and JaCoCo reports as build artifacts, even if a previous step
   failed, so failures/coverage drops are easy to inspect from the Actions run.

## Workflow file

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    name: Build, unit tests and integration tests
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
          cache: maven

      - name: Run unit test suite and JaCoCo coverage gate
        run: ./mvnw -B clean verify

      - name: Run concurrency & Testcontainers integration tests
        run: ./mvnw -B test -Dtest=WarehouseConcurrencyIT,WarehouseTestcontainersIT

      - name: Package application
        run: ./mvnw -B package -DskipTests

      - name: Upload test and coverage reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-and-coverage-reports
          path: |
            target/surefire-reports/
            target/site/jacoco/
          if-no-files-found: ignore
```

## Health checks

The application exposes SmallRye/Quarkus health endpoints (see `pom.xml`'s
`quarkus-smallrye-health` dependency and `HealthCheckTest`), which a deployment
pipeline's rollout/readiness gate would poll:

| Endpoint             | Purpose                                                   |
|-----------------------|------------------------------------------------------------|
| `GET /q/health`       | Overall status (liveness + readiness combined)             |
| `GET /q/health/live`  | Liveness probe — is the process alive                      |
| `GET /q/health/ready` | Readiness probe — includes datasource connectivity check   |

In a container/Kubernetes deployment, `/q/health/live` and `/q/health/ready` map
directly to `livenessProbe` and `readinessProbe`.

## Possible follow-ups (not implemented)

- A separate `cd.yml` that builds and pushes a container image on a tag/release,
  then deploys using the health endpoints above as the rollout gate.
- Branch protection on `main` requiring the CI job to pass before merge.
- A native-image build job (the `native` Maven profile already exists in `pom.xml`)
  for smaller/faster-starting container images.
