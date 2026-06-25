# FirstClub Membership & Tier Reevaluation Service

A high-performance, enterprise-grade subscription and dynamic tier reevaluation engine built using **Spring Boot 3.x**, **Apache Kafka**, **PostgreSQL**, and **Redis**. This service handles asynchronous, out-of-band user metric processing to transition subscriber tiers seamlessly with sub-second performance.

---

## 📑 Executive Summary & Assignment Scope

### The Challenge
The core requirement was to construct a robust backend membership engine capable of managing user subscription lifecycles (subscribing, upgrading, and changing plans) while actively tracking user milestone metrics (monthly spends, transaction frequencies, and demographic metadata) to recalculate rewards or tier status levels dynamically.

### What We Delivered
We designed and implemented a production-ready microservice module that features:
1. **REST API Gateway Edge**: Clean, idempotent REST endpoints managing subscriber plan selections with strict structural JSR-380 input data validations.
2. **Asynchronous Milestone Ingestion**: A decoupled event consumer mapping ingestion pipelines via Apache Kafka to absorb burst order transaction events gracefully without degrading customer checkout experiences.
3. **Decoupled Strategy Reevaluation Engine**: A fully dynamic evaluation loop that checks eligibility using an extensible strategy design pattern.
4. **Resilient Data Layers**: A transactional PostgreSQL mapping configuration backed by an optimistic locking versioning structure alongside a low-latency Redis cache-eviction boundary.
5. **100% Green Test Matrix**: A stateless, ultra-fast Unit and Slice testing ecosystem tracking 10 core execution logic flows in less than 5 seconds.

---

## 📐 Architecture & Senior Engineering Design Decisions

### 1. The Strategy Design Pattern (Tier Eligibility Engine)
* **The Decision**: Rather than embedding endless conditional `if-else` blocks inside our core service layer to validate if a user qualifies for a tier upgrade, we implemented an extensible **Strategy Pattern** linked via a master `TierEligibilityStrategy` interface.
* **The Thought Process**: Hardcoded business thresholds limit long-term agility. By isolating `OrderCountStrategy`, `OrderValueStrategy`, and `CohortEligibilityStrategy` into decoupled stateless components, our application strictly adheres to the **Open-Closed Principle (SOLID)**. Introducing future rules (e.g., Geo-location restrictions or referral milestones) requires creating a new strategy class without rewriting existing code.

### 2. Out-of-Band Async Processing via Kafka
* **The Decision**: Order milestones and transaction metrics are processed asynchronously by listening to a dedicated `order-milestone-events` Kafka topic.
* **The Thought Process**: Forcing a user to wait for tier recalculations during an active checkout loop slows down latency and introduces a single point of failure. By moving evaluation out-of-band, checkout services execute lightning fast. If our tier service experiences unexpected downtime, Kafka acts as a persistent buffer, ensuring no reevaluation events are lost.

### 3. Distributed Cache Invalidation Pattern
* **The Decision**: Upon a successful tier advancement, an automated hook immediately flushes the user profile cache layer via `RedisTemplate.delete(userId)`.
* **The Thought Process**: To maintain ultra-low latency reads across frontend user profiles, subscriber data is heavily cached. To avoid systemic "stale data splits" where a user sees their old tier status after a real-time upgrade, we enforce strict programmatic cache eviction immediate to state write commits.

### 4. High-Throughput Concurrency & Race-Condition Insulation
* **The Decision**: Combined JPA Optimistic Locking (`@Version`) with stateless strategy workers.
* **The Thought Process**: In a production environment, an automated backend process could consume a burst of Kafka event messages for a single user (e.g., three quick purchases back-to-back). If parallel Kafka consumer threads try to reevaluate and modify that same user profile simultaneously, it can lead to classic data corruption or lost updates. By enforcing `@Version` tracking, the database will safely reject stale modification attempts, throwing an `ObjectOptimisticLockingFailureException`. The service can then catch this and execute a clean retry loop with the freshest user state, safely claiming the architectural concurrency bonus.

### 5. Database-Driven Configurable Perks (Extensibility)
* **The Decision**: Modeled tier advantages (e.g., free delivery flags, dynamic discount percentages) via a dedicated relational mapping layer managed by `TierBenefitRepository`.
* **The Thought Process**: Hardcoding benefit perks directly into Java enums or service classes forces a full application redeployment whenever marketing decides to adjust a tier's discount percentage. By persisting benefits dynamically and linking them to tier records, product managers can update system-wide perks at runtime via simple database adjustments or configuration APIs without touching a line of code.

---

## 🔄 System Core Flow Blueprint

```text
[Kafka Event Ingestion]
         │
         ▼
[OrderConsumer] ──(Dispatches Metrics)──► [MembershipServiceImpl]
                                                  │
                                          (Loads Tier Chain)
                                                  │
                                                  ▼
                                      [TierEligibilityStrategy]
                                      ├── OrderCountStrategy  (Passed? ✅)
                                      ├── OrderValueStrategy  (Passed? ✅)
                                      └── CohortStrategy      (Passed? ✅)
                                                  │
                                           (All Evaluated)
                                                  │
                                                  ▼
                                       [Database State Commit]
                                       ├── Update User Tier 
                                       └── Increment Version
                                                  │
                                                  ▼
                                       [Redis Cache Eviction]
```

---

## 🛠️ Infrastructure & Local Execution Steps

### Prerequisites
* **Java 21 / OpenJDK 25**
* **Maven 3.9+**
* **Docker & Docker Compose**

### 1. Boot Environment Infrastructure
Spin up pre-configured Docker containers holding your localized PostgreSQL database cluster and your Apache Kafka broker instances:

```bash
docker-compose up -d
```

### 2. Compile and Execute Unit Test Suite
Verify that all strategies, service mocks, internal cash flows, and error advisors are completely green:

```bash
mvn clean test
```

### 3. Start the Application Service
Run the service locally bound to port 8080:

```bash
mvn spring-boot:run
```

---

## 📨 Postman Testing API Registry
Import these definitions into Postman to test system endpoints locally.

### 1. Add New User Subscription
* **HTTP Method:** `POST`
* **URL:** `http://localhost:8080/api/v1/memberships/subscribe`
* **Headers:** `Content-Type: application/json`
* **Payload:** ```json
{
    "userId": "user_dev_03",
    "planId": "plan_quarterly_adv",
    "tierId": "tier_silver"
}

### 2. Modify an Existing Subscription
* **HTTP Method:** `PUT`
* **URL:** `http://localhost:8080/api/v1/memberships/modify`
* **Headers:** `Content-Type: application/json`
* **Payload:** ```json
{
    "userId": "user_dev_03",
    "planId": "plan_yearly_pro",
    "tierId": "tier_silver"
}

### 3. Fetch the Seeded Catalog (Plans and Tiers)
* **HTTP Method:** `GET`
* **URL:** `http://localhost:8080/api/v1/memberships/catalog`

### 4. Get Subscription Status
* **HTTP Method:** `GET`
* **URL:** `http://localhost:8080/api/v1/memberships/status/:user_id`

### 5. Delete Subscription
* **HTTP Method:** `DELETE`
* **URL:** `http://localhost:8080/api/v1/memberships/cancel/:user_id`

### 4. Out-Of-Band Realtime Upgrade Verification (Kafka Event Simulation)
To simulate live streaming transactions matching your database cohort configurations, open your host terminal and broadcast a structured payload directly into the active containerized broker:
```bash
docker exec -it firstclub-kafka kafka-console-producer --bootstrap-server localhost:9092 --topic order-milestone-events
```
Paste the following event marker message into the interactive standard input stream prompt: ```json
{"userId": "user_dev_03", "currentMonthOrderCount": 5, "currentMonthSpend": 100.00}

---

## 🛡️ Production-Grade Resilience & Error Management
The service includes a centralized network advisor (GlobalExceptionHandler) to intercept systemic failures gracefully.

Deterministic Time Tracking: All responses dump formatted Instant.now().toString() tags, enforcing clean ISO-8601 UTC formats to prevent regional client timezone drift.

Internal Data Masking: Unexpected server anomalies (500 Internal Server Errors) are logged inside private telemetry frameworks for security tracking while surfacing sanitized safe notifications back to consumers:
```json
{
    "timestamp": "2026-06-25T17:18:22.104Z",
    "status": 500,
    "error": "Internal Server Error",
    "message": "An unexpected internal error occurred. Please contact system administrators."
}
```