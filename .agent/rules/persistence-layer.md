---
trigger: always_on
glob: "**/sokybot-persistence/**/*"
description: Persistence layer guidelines (JPA/Hibernate)
---

# Persistence Layer Rules

## 1. Technology Stack
- **ORM**: Hibernate 5.6 (Embedded in bundle).
- **Database**: H2 (Embedded).
- **Pattern**: JPA 2.2 + Spring Data-style Repositories.

## 2. Module Structure (`sokybot-persistence`)
- `entities/`: JPA `@Entity` classes.
- `service/`: Repository interfaces (`Repository<T, ID>`) and Service contracts.
- `internal/`: Implementation details (hidden from OSGi exports).

## 3. Best Practices
1. **Separation of Concerns**: Define entities in `entities` package and repository interfaces in `service` package.
2. **OSGi Service**: The persistence layer acts as a service provider. Consumers should inject `IGamePersistenceFactory` or specific repository services via `@Reference`.
3. **No Direct SQL**: Use JPA Criteria API or JPQL within repository implementations. Avoid raw SQL to maintain database portability.
