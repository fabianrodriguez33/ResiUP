# Arquitectura de ResiUP y ResiAPI

Los diagramas están en Mermaid (se ven en GitHub). Siguen los niveles 1 a 3 del modelo C4.

## Nivel 1 · Contexto

```mermaid
flowchart LR
    R[Residente]:::p --> APP
    P[Pastor / Monitor]:::p --> APP
    G[Garita]:::p --> APP
    A[Apoderado]:::p --> APP
    AD[Administrador]:::p --> APP
    APP[ResiUP<br/>aplicación Android e iOS]:::s -->|HTTPS + JWT| API[ResiAPI<br/>servicio REST]:::s
    API --> DB[(PostgreSQL)]:::d
    API -. correo / push opcionales .-> EXT[SMTP · FCM]:::e
    classDef p fill:#dbeafe,stroke:#1d4ed8
    classDef s fill:#dcfce7,stroke:#15803d
    classDef d fill:#fef3c7,stroke:#b45309
    classDef e fill:#f3f4f6,stroke:#6b7280
```

Cinco roles usan la misma aplicación; el rol viaja firmado en el token y decide las pantallas y las rutas permitidas.

## Nivel 2 · Contenedores

```mermaid
flowchart TB
    subgraph Movil[Teléfono]
        UI[shared · Compose Multiplatform<br/>presentación, dominio y datos]
        AND[androidApp<br/>punto de entrada, cámara, almacén seguro]
        IOS[iosApp<br/>proyecto Xcode]
        LOC[(SQLDelight<br/>copia local)]
        UI --- LOC
        AND --> UI
        IOS --> UI
    end
    subgraph Servidor[Docker Compose]
        API[ResiAPI · Spring Boot 3 / Java 21]
        DB[(PostgreSQL 16<br/>migraciones Flyway V1–V21)]
        API --> DB
    end
    UI -->|Ktor · HTTPS · JWT| API
```

| Contenedor | Tecnología | Responsabilidad |
|---|---|---|
| `shared` | Kotlin Multiplatform, Compose, Koin, Ktor, SQLDelight | Toda la lógica y las pantallas |
| `androidApp` / `iosApp` | Android (AAB/APK firmado) / Xcode | Arranque y capacidades nativas por plataforma |
| ResiAPI | Spring Boot, Spring Security (JWT), JPA | Reglas de negocio y roles |
| PostgreSQL | 16 | Datos; el esquema nace solo de Flyway |

## Nivel 3 · Componentes de la aplicación (`shared`)

```mermaid
flowchart LR
    subgraph presentation
        S[Pantallas Compose] --> VM[ViewModel<br/>StateFlow + estado de UI]
    end
    subgraph domain
        UC[Casos de uso]
        RI[[Interfaces de repositorio]]
        M[Modelos]
    end
    subgraph data
        RP[Repositorios] --> API[Apis Ktor]
        RP --> DBL[(SQLDelight)]
        SEG[Almacén seguro del token]
    end
    VM --> UC --> RI
    VM --> RI
    RP -. implementa .-> RI
```

Reglas de dependencia: `presentation → domain ← data`. El dominio no conoce Ktor, SQLDelight ni Android. Las pantallas no llaman al cliente HTTP: piden datos a un ViewModel o a una interfaz de repositorio del dominio (`AdministracionRepository`, `AccionesPastorRepository`, `ReservasRepository`, `ResidenteRepository`, etc.).

## Nivel 3 · Componentes del servicio (ResiAPI)

```mermaid
flowchart LR
    C[controller<br/>rutas + @PreAuthorize] --> S[service<br/>reglas de negocio]
    S --> R[repository<br/>Spring Data JPA] --> E[(entity / PostgreSQL)]
    S --> X[Enviadores<br/>correo · push · SMS]
    SEC[security<br/>JWT + roles] --> C
    EX[exception<br/>respuestas 400/401/403/404/409] --- C
```

## Decisiones

- **Un solo código para Android e iOS** (Kotlin Multiplatform); lo específico (cámara, almacén seguro) entra por interfaces del dominio con implementación por plataforma.
- **Reglas en el servidor**: fechas, horarios, cierres y roles se validan en ResiAPI aunque la interfaz también los muestre.
- **Offline-first**: la interfaz lee de la base local y las operaciones pendientes se suben antes de bajar datos (ver `README.md`). Las acciones de administración y corrección del pastor exigen conexión porque el servidor es la única fuente de verdad.
- **Secretos fuera del repositorio**: clave de firma del JWT y credenciales por variables de entorno; el almacén de claves de Android no se versiona.
