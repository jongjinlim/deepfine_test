# 재고 관리 시스템 (Inventory Management MVP)

상품의 현재 재고 수량을 확인하고, 입고/출고 시 동시 요청에도 데이터 정합성을 보장하는 재고 관리 API입니다.

## 기술 스택

- Java 21, Spring Boot 4.1
- Spring Data JPA (Hibernate), PostgreSQL 16
- Redisson (분산락), Redis 7
- Spring Event (`ApplicationEventPublisher` + `@TransactionalEventListener`) 기반 이력 적재
- JUnit5 + AssertJ (통합 테스트, 실제 Redis/Postgres 연동)

## 프로젝트 구조

패키지 구조

1. `config`
    - Bean 등록이 필요한 설정 클래스를 모아둔 패키지입니다.
        - `redis`: Redisson 설정(`RedissonConfig`)과 분산락 처리를 위한 AOP(`DistributedLock`, `DistributedLockAop`, `AopForTransaction`, `CustomSpringELParser`)를 담고 있습니다.

2. `controller`
    - 컨트롤러 계층을 담당하며, 외부 클라이언트(웹, 모바일 등)와의 인터페이스 역할을 합니다.
    - 요청(request)과 응답(response)을 명확하게 분리하여, 컨트롤러가 도메인 객체와 직접적으로 엮이지 않도록 설계되었습니다.
        - `request`: API 요청을 받을 때 사용하는 DTO(`InventoryIncreaseRequest`, `InventoryDecreaseRequest`, `ProductRegisterRequest`)를 관리하는 패키지입니다.
        - `response`: API 응답을 반환할 때 사용하는 DTO(`InventoryResponse`)를 관리하는 패키지입니다.

3. `entity`
    - 여러 테이블 엔티티가 공통으로 상속하는 `BaseEntity`를 담은 패키지입니다.
    - 생성/수정 시각(`createdAt`/`updatedAt`)을 JPA Auditing(`@CreatedDate`/`@LastModifiedDate`)으로 자동 채워주기 위한 목적으로, 각 Entity가 시각 관리 로직을 중복해서 갖지 않도록 했습니다.

4. `enums`
    - 도메인 전역에서 쓰는 enum(`InventoryChangeType`)을 관리하는 패키지입니다.

5. `error`
    - 예외와 오류를 전역으로 관리하는 패키지입니다(`ErrorCode`, `GlobalException`, `ApiControllerAdvice`).
        - `response`: API 응답값(성공/에러)을 일관된 형식(`GlobalResponse`, `GlobalErrorResponse`)으로 설정해 응답을 반환할 때 사용하는 클래스 패키지입니다.

6. `service`
    - 비즈니스 로직을 담당하는 패키지입니다.
    - domain, service 계층, repository를 포함하고 있는 핵심 패키지입니다.
        - 테이블(도메인)별로 패키지 구조를 나눴습니다 (`inventory`, `inventoryHistory`).
            - `domain`
                - service 계층에서 사용할 순수 도메인 객체(`Inventory`, `InventoryChangedEvent`)를 담고 있는 패키지입니다.
                - Entity 클래스와 분리해서, 재고 증감 검증 같은 비즈니스 로직이 JPA 매핑 세부사항에 엮이지 않도록 설계했습니다.
                - Setter 없이 `@Builder`와 의미 있는 이름의 메서드(`increase`, `decrease`)로만 값을 바꿀 수 있게 했습니다. 무분별한 상태 변경 경로를 없애고, 수량이 음수가 되면 안 된다는 규칙을 도메인 메서드 안에서만 검증하도록 강제해 예측 가능한 코드를 유지하려 했습니다.
            - `service`
                - 서비스 로직을 담당하는 service 클래스와 repository interface를 담고 있는 패키지입니다.
                - service에서는 직접적으로 Entity를 호출하지 않고 domain 객체만을 이용해 repository에 값을 넘겨주도록 처리했습니다.
                - 분산락이 걸리는 로직(`register`/`increase`/`decrease`)은 `InventoryLockedOperations`로 따로 분리해뒀습니다. Spring AOP는 같은 빈 내부에서의 self-invocation(자기 자신 호출)을 가로채지 못해서, `InventoryService`가 항상 별도 빈을 거쳐 호출하게 만들어야 락이 확실히 걸립니다.
            - `repository`
                - 데이터베이스와 직접적으로 연관된 클래스(Entity, `JpaRepository`, 구현체)를 담고 있는 패키지입니다.
                - service 패키지의 `Repository` 인터페이스를 `RepositoryImpl`이 구현하고, 내부적으로 Spring Data JPA의 `JpaRepository`를 사용해 Entity ↔ Domain 객체 변환을 담당합니다. service 계층이 JPA 구현 세부사항에 의존하지 않도록 인터페이스로 경계를 분리해, 나중에 구현체를 교체하거나 테스트에서 대체하기 쉽도록 했습니다.

## 실행 방법

### 1. 인프라 기동 (PostgreSQL, Redis)

```bash
docker compose up -d
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

기본적으로 `local` 프로필(`application-local.yml`)로 동작하며, `localhost:5432` PostgreSQL과 `localhost:6379` Redis에 연결합니다.
`spring.jpa.hibernate.ddl-auto=update`로 기동 시 필요한 테이블/제약조건이 자동 생성됩니다. (스키마를 명시적으로 보고 싶다면 `ddl.sql` 참고)

### 3. 테스트 실행

```bash
./gradlew test
```

테스트는 `@SpringBootTest`로 실제 Redis/PostgreSQL에 붙어 동작하므로, 실행 전 `docker compose up -d`가 되어 있어야 합니다.

### 4. API 문서 (Swagger UI)

애플리케이션 기동 후 `http://localhost:8080/swagger-ui.html` 에서 API를 직접 확인/호출할 수 있습니다.

## API 명세

모든 응답은 `{ "data": ..., "error": null }` 또는 `{ "data": null, "error": { "code": ..., "message": ... } }` 형태의 공통 포맷(`GlobalResponse`)을 따릅니다.

| Method | URL                                | 설명                         | Request Body                     |
|--------|------------------------------------|------------------------------|-----------------------------------|
| POST   | `/api/products`                    | 신규 상품 등록 (수량 0)      | `{ "name": "상품A" }`            |
| GET    | `/api/inventories/{id}`             | 재고 단건 조회               | -                                  |
| POST   | `/api/inventories/increase`         | 입고 (미등록 상품이면 자동 등록 후 입고) | `{ "name": "상품A", "quantity": 10 }` |
| POST   | `/api/inventories/{id}/decrease`    | 출고 (수량 부족 시 실패)     | `{ "quantity": 5 }`               |

### 에러 코드

| code                     | HTTP Status | 설명                                   |
|--------------------------|-------------|----------------------------------------|
| `BAD_REQUEST_BODY`       | 400         | 요청 형식/값이 잘못됨 (검증 실패, 경로변수 타입 불일치, JSON 파싱 실패 등) |
| `PRODUCT_NOT_FOUND`      | 404         | 존재하지 않는 상품 id 조회/출고 시도    |
| `INSUFFICIENT_STOCK`     | 409         | 출고 요청 수량이 현재 재고보다 많음     |
| `INVALID_QUANTITY`       | 400         | 입출고 수량이 0 이하                    |
| `DUPLICATE_PRODUCT_NAME` | 409         | 이미 등록된 상품명으로 등록 시도         |
| `LOCK_ACQUISITION_FAILED`| 503         | 분산락 획득 실패(동시 요청 과다)         |
| `INTERNAL_SERVER_ERROR`  | 500         | 예상하지 못한 서버 오류                 |

## DB DDL

전체 스크립트는 [`ddl.sql`](./ddl.sql)에도 있습니다. 애플리케이션은 `ddl-auto=update`로 기동 시 아래 스키마를 자동 생성하므로 별도 실행은 필수가 아니며, 실제 반영되는 스키마를 명시적으로 문서화하기 위한 용도입니다.

```sql
-- 상품(재고) 테이블
CREATE TABLE IF NOT EXISTS inventory (
    id         BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name       VARCHAR(255)             NOT NULL,
    quantity   INTEGER                  NOT NULL,
    created_at TIMESTAMP(6)             NOT NULL,
    updated_at TIMESTAMP(6)             NOT NULL,
    CONSTRAINT uk_inventory_name UNIQUE (name)
);

COMMENT ON TABLE inventory IS '상품';
COMMENT ON COLUMN inventory.id IS '재고 아이디';
COMMENT ON COLUMN inventory.name IS '상품명';
COMMENT ON COLUMN inventory.quantity IS '현재 재고 수량';
COMMENT ON COLUMN inventory.created_at IS '생성 시각';
COMMENT ON COLUMN inventory.updated_at IS '수정 시각';

-- 입출고 이력 테이블
CREATE TABLE IF NOT EXISTS inventory_history (
    id               BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    inventory_id     BIGINT                   NOT NULL,
    type             VARCHAR(255)             NOT NULL CHECK (type IN ('INCREASE', 'DECREASE')),
    change_quantity  INTEGER                  NOT NULL,
    result_quantity  INTEGER                  NOT NULL,
    created_at       TIMESTAMP(6)             NOT NULL,
    updated_at       TIMESTAMP(6)             NOT NULL
);

COMMENT ON TABLE inventory_history IS '입출고 이벤트 이력';
COMMENT ON COLUMN inventory_history.id IS '이력 아이디';
COMMENT ON COLUMN inventory_history.inventory_id IS '대상 재고 아이디';
COMMENT ON COLUMN inventory_history.type IS '입고/출고 구분';
COMMENT ON COLUMN inventory_history.change_quantity IS '변경된 수량';
COMMENT ON COLUMN inventory_history.result_quantity IS '변경 후 남은 재고 수량';
COMMENT ON COLUMN inventory_history.created_at IS '생성 시각';
COMMENT ON COLUMN inventory_history.updated_at IS '수정 시각';

CREATE INDEX IF NOT EXISTS idx_inventory_history_inventory_id ON inventory_history (inventory_id);
```

- `inventory.name`에 `UNIQUE` 제약을 걸어, 분산락과 별개로 DB 레벨에서도 상품명 중복을 막습니다.
- `inventory_history.inventory_id`에 인덱스를 걸어, 특정 상품의 이력 조회(`findByInventoryId`)가 풀 스캔이 되지 않도록 했습니다.
- `inventory_history.type`은 `CHECK` 제약으로 `INCREASE`/`DECREASE` 외의 값이 들어가지 않도록 했습니다.

## 동시성 & 데이터 정합성 설계

### 분산락 (Redisson)

입고/출고/등록은 전부 **상품명(name)** 을 기준으로 하는 분산락(`@DistributedLock`, AOP 기반)으로 보호됩니다.

- 입고(`increase`)는 상품이 없을 수도 있어 애초에 `id`가 없는 경우가 존재하므로, 유일하게 항상 알 수 있는 식별자인 `name`을 락 키로 사용합니다.
- 출고(`decrease`)는 `id`로 요청이 들어오지만, 같은 상품에 대한 입고/출고/등록이 서로 다른 락 네임스페이스를 쓰면 동시에 들어왔을 때 서로를 막지 못하는 문제가 있어(같은 행을 서로 다른 키로 잠그는 상황), 내부적으로 `id → name`을 조회한 뒤 동일하게 `name` 기준으로 락을 겁니다.
- 락이 걸리는 실제 로직은 `InventoryLockedOperations`에 모여 있고, `InventoryService`는 이를 호출하는 얇은 파사드입니다. (같은 클래스 내부에서 락이 걸린 메서드를 직접 호출하면 Spring AOP 프록시를 우회해 락이 걸리지 않는 self-invocation 문제가 있어, 락 로직을 별도 빈으로 분리했습니다.)
- 상품명에는 DB `UNIQUE` 제약도 함께 걸어 2차 방어선을 두었습니다.
- 락 획득 대기(`waitTime`)는 5초, 락 임대 시간(`leaseTime`)은 3초입니다(`@DistributedLock` 기본값).
  - 이 락이 보호하는 구간은 DB 조회 1번 + 검증 + 저장 1번, 이벤트 발행이 전부라 실제 실행 시간은 수십 ms 수준입니다. 3초는 여기에 수십~수백 배의 여유를 둔 값으로, 일시적인 DB 지연은 흡수하면서도 락을 쥔 프로세스가 죽는 최악의 경우에도 다른 요청이 오래 막히지 않고 3초 안에 자동 해제되도록 한 것입니다.
  - leaseTime을 명시하면 Redisson의 자동 연장(watchdog)은 동작하지 않고 고정 시간 후 그대로 풀립니다. 지금처럼 작업 시간이 짧고 예측 가능한 경우엔 문제가 없지만, 트랜잭션이 더 무거워지거나 시간이 가변적인 로직이라면 leaseTime을 고정하는 대신 watchdog(자동 연장)을 쓰는 쪽이 더 안전한 선택입니다.

### 이력 관리 (Event 기반)

재고가 변경되면 `InventoryChangedEvent`를 발행하고, `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`로 동작하는 `InventoryHistoryEventListener`가 별도 트랜잭션(`REQUIRES_NEW`)으로 `inventory_history`에 이력을 적재합니다. 재고 변경 트랜잭션이 커밋된 뒤에만 이력이 기록되므로, 롤백된 요청에 대한 이력이 남지 않습니다.

`inventory`/`inventory_history` 모두 `created_at`/`updated_at`은 공통 `BaseEntity`(JPA Auditing, `@CreatedDate`/`@LastModifiedDate`)로 관리됩니다.

## 테스트 구성

- `InventoryServiceQueryTest` — 재고 조회 기능 테스트 (단일 스레드)
- `InventoryServiceConcurrencyTest` — 분산락이 실제 동시 요청 상황에서 정합성을 지키는지 검증하는 통합 테스트 (50~100+ 스레드로 동시 요청)
  - 동시 출고 시 수량이 정확히 차감되는지
  - 동시 신규 입고 시 행이 1개만 생성되고 수량이 정확히 합산되는지
  - 동시 등록 시 행이 1개만 생성되는지
  - **같은 상품에 대해 입고와 출고를 동시에 섞어 호출해도** 정합성이 유지되는지
- `InventoryHistoryEventListenerTest` — 입고/출고 후 이력이 정확히 기록되는지 검증
