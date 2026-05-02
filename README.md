# NovoBanco — Microservicio de Cuentas y Transacciones

Microservicio REST para la gestión de clientes, cuentas bancarias y transacciones financieras (depósitos, retiros y transferencias).

---

## Descripción del problema

El sistema debe permitir:

- Registrar clientes con identificación única (cédula, RUC o pasaporte).
- Abrir cuentas de ahorro (SAVINGS) o corriente (CHECKING) asociadas a un cliente.
- Realizar depósitos, retiros y transferencias entre cuentas con garantías de consistencia.
- Consultar el historial de transacciones y detectar duplicados por referencia única.
- Listar las transferencias salientes de un cliente en un rango de fechas.

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.0.6 / Spring Framework 7 |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | PostgreSQL 16 |
| Contenedores | Docker + Docker Compose |
| Documentación API | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven |

---

## Modelo de datos

### Motor seleccionado: PostgreSQL 16

**Justificación técnica:**

1. **Soporte nativo de transacciones ACID con aislamiento configurable.** Las operaciones financieras requieren que un débito y su crédito correspondiente sean atómicos e indivisibles. PostgreSQL garantiza esto con `SERIALIZABLE` o `READ COMMITTED` + bloqueos explícitos (`SELECT FOR UPDATE`), sin riesgo de lecturas sucias ni escrituras fantasma.

2. **Tipos de dato adecuados para finanzas.** `NUMERIC(19,4)` almacena valores monetarios con precisión exacta, sin los errores de redondeo de `FLOAT` o `DOUBLE`. PostgreSQL también tiene `UUID` nativo, `TIMESTAMPTZ` con zona horaria y `CHECK` constraints en columna, todo necesario para este dominio.

3. **Índices parciales y compuestos.** PostgreSQL permite crear índices sobre subconjuntos de filas (ej: `WHERE type = 'TRANSFER_DEBIT'`) reduciendo el tamaño del índice y acelerando consultas específicas como el historial paginado o las transferencias salientes por rango de fechas, sin indexar filas irrelevantes.

4. **Bloqueo pesimista a nivel de fila (`FOR UPDATE`).** El control de concurrencia para retiros y transferencias simultáneas requiere bloquear exactamente la fila de la cuenta afectada, no la tabla completa. PostgreSQL implementa esto eficientemente con MVCC, permitiendo que lecturas concurrentes no bloqueen escrituras en otras filas.

5. **Madurez y ecosistema.** PostgreSQL 16 es el estándar de facto en sistemas financieros de producción. Su comportamiento bajo alta carga, sus herramientas de monitoreo (`pg_stat_activity`, `EXPLAIN ANALYZE`) y su compatibilidad con Spring Data JPA + Hibernate lo hacen la opción más predecible y documentada para este tipo de dominio.

---

### Esquema DDL

El script completo está en [`schema.sql`](schema.sql) en la raíz del repositorio. Incluye tipos de dato, constraints, checks y todos los índices.

---

### Decisiones de diseño del modelo

**Normalización**

El modelo está en **3FN (Tercera Forma Normal)**. Cada tabla representa una entidad del dominio sin atributos derivados:

- `clients` almacena solo datos del cliente.
- `accounts` referencia al cliente por `client_id` (FK) — el nombre del cliente no se repite en la tabla de cuentas.
- `transactions` referencia a la cuenta por `account_id` — el saldo no se almacena en transacciones, se mantiene en `accounts`.

No se desnormalizó ningún campo porque el volumen y los patrones de acceso no lo justifican: las consultas son por cuenta individual (`account_id = ?`), no agregaciones masivas entre tablas.

**Estructura de índices**

| Índice | Columnas | Motivo |
|---|---|---|
| `idx_transactions_account_id_created_at` | `(account_id, created_at DESC)` | Soporta el historial paginado ordenado por fecha sin full scan |
| `idx_transactions_reference` | `(reference)` | Búsqueda de transacción por referencia (detección de duplicados, Q4) |
| `idx_transactions_transfer_debit_created_at` | `(type, created_at)` WHERE `type='TRANSFER_DEBIT'` | Índice parcial para transferencias salientes (Q3) — ignora depósitos y retiros |
| `idx_transactions_transfer_reference` | `(transfer_reference)` WHERE NOT NULL | Relacionar las dos mitades de una transferencia sin escanear toda la tabla |
| `idx_accounts_client_id` | `(client_id)` | FK lookup: cuentas de un cliente |
| `idx_accounts_uuid`, `idx_clients_uuid`, `idx_transactions_uuid` | `(uuid)` | Búsquedas por identificador público de la API |

Los constraints `UNIQUE` en `reference`, `account_number`, `email` e `identification` crean automáticamente índices únicos en PostgreSQL — no se duplican.

**Soporte para historial paginado**

La consulta de historial (`GET /accounts/{accountNumber}/transactions?page=0&size=20`) se traduce a:

```sql
SELECT * FROM transactions
WHERE account_id = ?
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;
```

El índice compuesto `(account_id, created_at DESC)` hace que PostgreSQL resuelva esta consulta con un **Index Scan** directo, sin ordenar en memoria ni leer filas de otras cuentas. La paginación con `LIMIT/OFFSET` es eficiente en las primeras páginas, que son las más consultadas (historial reciente).

---

## Arquitectura

El proyecto sigue **Arquitectura Hexagonal (Ports & Adapters)**. El dominio no tiene dependencias hacia infraestructura; la comunicación entre capas ocurre exclusivamente a través de interfaces (puertos).

```
┌──────────────────────────────────────────────────────┐
│                   INFRASTRUCTURE                     │
│  ┌─────────────────┐        ┌──────────────────────┐ │
│  │  REST Controllers│        │  JPA Adapters        │ │
│  │  (Adapters IN)  │        │  (Adapters OUT)      │ │
│  └────────┬────────┘        └──────────┬───────────┘ │
│           │ uses ports IN              │ implements   │
│  ┌────────▼────────────────────────────▼───────────┐ │
│  │                 APPLICATION                     │ │
│  │   AccountPort  ClientPort  TransactionPort      │ │
│  │   AccountRepositoryPort  ClientRepositoryPort   │ │
│  │   TransactionRepositoryPort                     │ │
│  │                                                 │ │
│  │   AccountService  ClientService                 │ │
│  │   TransactionService                            │ │
│  └─────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────┐ │
│  │                   DOMAIN                        │ │
│  │   Client  Account  Transaction                  │ │
│  │   AccountDomainService  Exceptions  Enums       │ │
│  └─────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────┘
```

---

## Diagrama ER

```
┌─────────────────────────────────┐
│            clients              │
├─────────────────────────────────┤
│ id              BIGSERIAL  PK   │
│ uuid            UUID       UQ   │
│ full_name       VARCHAR         │
│ email           VARCHAR    UQ   │
│ identification  VARCHAR    UQ   │
│ type_identification  ENUM       │
│ phone           VARCHAR         │
│ address         VARCHAR         │
│ gender          VARCHAR         │
│ created_at      TIMESTAMP       │
└────────────────┬────────────────┘
                 │ 1
                 │
                 │ N
┌────────────────▼────────────────┐
│            accounts             │
├─────────────────────────────────┤
│ id             BIGSERIAL   PK   │
│ uuid           UUID        UQ   │
│ account_number VARCHAR     UQ   │
│ client_id      BIGINT      FK   │
│ type           ENUM             │  SAVINGS | CHECKING
│ currency       VARCHAR          │
│ balance        DECIMAL          │
│ status         ENUM             │  ACTIVE | BLOCKED | CLOSED
│ created_at     TIMESTAMP        │
│ updated_at     TIMESTAMP        │
└────────────────┬────────────────┘
                 │ 1
                 │
                 │ N
┌────────────────▼────────────────┐
│           transactions          │
├─────────────────────────────────┤
│ id               BIGSERIAL  PK  │
│ uuid             UUID       UQ  │
│ account_id       BIGINT     FK  │
│ type             ENUM            │  DEPOSIT | WITHDRAWAL
│                                  │  TRANSFER_DEBIT | TRANSFER_CREDIT
│ amount           DECIMAL         │
│ reference        UUID       UQ  │  idempotencia
│ status           ENUM            │  SUCCESS | FAILED
│ description      VARCHAR         │
│ transfer_reference  UUID   NULL  │  vincula débito ↔ crédito
│ created_at       TIMESTAMP       │
└─────────────────────────────────┘
```

> Una transferencia genera **dos filas** en `transactions`: una `TRANSFER_DEBIT` en la cuenta origen y una `TRANSFER_CREDIT` en la cuenta destino. Ambas comparten el mismo `transfer_reference` para poder relacionarlas.

---

## Restricciones y decisiones de negocio

### Saldo negativo

**Decisión:** la validación se hace en la **capa de dominio** (`AccountDomainService.debit`), no en la base de datos ni en el servicio de aplicación.

**Justificación:** el saldo negativo es una invariante de negocio, no una restricción técnica. Colocarla en el dominio garantiza que ningún camino de código (servicio, test, script) pueda violarla, independientemente del adaptador que llame.

---

### Cuenta inactiva

Las operaciones sobre cuentas `BLOCKED` o `CLOSED` lanzan `AccountNotOperableException`, que devuelve HTTP 422 con el mensaje:

```json
{
  "status": 422,
  "error": "La cuenta ACC-000001 no está activa (estado: BLOCKED). No se pueden realizar operaciones."
}
```

Este error es específico y distinto al error genérico 500, permitiendo al cliente diferenciar el rechazo por estado del rechazo por fondos insuficientes.

---

### Transferencia parcial

**Mecanismo:** `@Transactional` en `TransactionService.transfer`. Si el crédito falla después de aplicar el débito, Spring revierte toda la unidad de trabajo — el débito nunca se persiste.

**Garantía:** no existe un estado intermedio observable desde fuera de la transacción. La base de datos recibe o el par completo (débito + crédito) o ninguno.

---

### Concurrencia básica

**Mecanismo:** bloqueo pesimista (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) al leer las cuentas involucradas. Esto traduce a `SELECT ... FOR UPDATE` en PostgreSQL, que bloquea la fila hasta que la transacción termine.

**Orden de bloqueo:** para evitar deadlock entre dos transferencias inversas simultáneas (A→B y B→A), las cuentas se bloquean siempre en **orden lexicográfico** por número de cuenta. Ambas operaciones intentan bloquear la misma cuenta primero, por lo que una avanza y la otra espera.

```
Sin orden: T1 bloquea A, T2 bloquea B → deadlock
Con orden: T1 y T2 bloquean A primero → T1 avanza, T2 espera → sin deadlock
```

---

### Idempotencia

**Implementación:** cada transacción tiene un campo `reference` (UUID) con constraint `UNIQUE`. El cliente puede enviar una `reference` propia; si no lo hace, el sistema genera una.

Antes de ejecutar cualquier operación se consulta si esa `reference` ya existe:

```
segunda llamada con la misma reference → DuplicateTransactionException → HTTP 409
```

Esto protege contra reenvíos por error de red: el sistema rechaza el duplicado sin ejecutar el movimiento dos veces.

---

## Endpoints disponibles

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/clients` | Crear cliente |
| `GET` | `/api/v1/clients/{identification}` | Consultar cliente (+ cuentas opcional) |
| `GET` | `/api/v1/clients/{identification}/transfers/outgoing` | Transferencias salientes por rango de fechas |
| `POST` | `/api/v1/accounts` | Crear cuenta |
| `GET` | `/api/v1/accounts/{accountNumber}` | Consultar cuenta |
| `PATCH` | `/api/v1/accounts/{accountNumber}/status` | Cambiar estado de cuenta |
| `POST` | `/api/v1/accounts/{accountNumber}/deposit` | Depositar |
| `POST` | `/api/v1/accounts/{accountNumber}/withdraw` | Retirar |
| `POST` | `/api/v1/transfers` | Transferir entre cuentas |
| `GET` | `/api/v1/accounts/{accountNumber}/transactions` | Historial paginado |
| `GET` | `/api/v1/transactions/reference/{reference}` | Buscar transacción por referencia |

Documentación interactiva: `http://localhost:8080/swagger-ui.html`

---

## Instrucciones de ejecución

### Requisitos

- Docker y Docker Compose instalados.
- Puerto `5432` (PostgreSQL) y `8080` (API) disponibles.

### Levantar el sistema

```bash
docker compose up --build
```

Este comando construye la imagen de la aplicación, levanta PostgreSQL, ejecuta el schema SQL inicial y arranca el servicio. La API queda disponible en `http://localhost:8080`.

### Detener y limpiar volúmenes

```bash
docker compose down -v
```

El flag `-v` elimina el volumen de datos de PostgreSQL. Útil para empezar desde cero con la base de datos limpia.

### Variables de entorno (opcionales)

Las variables tienen valores por defecto en `docker-compose.yml`:

| Variable | Defecto |
|---|---|
| `DB_NAME` | `novobanco_db` |
| `DB_USER` | `novobanco` |
| `DB_PASSWORD` | `novobanco_pass` |
| `DB_PORT` | `5432` |
| `SERVER_PORT` | `8080` |

---

## ADR — Decisiones de Arquitectura

### ADR-1: Arquitectura Hexagonal

**Contexto:** se necesita un diseño que permita cambiar la base de datos, el framework web o el mecanismo de persistencia sin tocar la lógica de negocio.

**Opciones consideradas:**
- Arquitectura en capas tradicional (Controller → Service → Repository).
- Arquitectura hexagonal (Ports & Adapters).

**Decisión:** arquitectura hexagonal. El dominio y la aplicación no importan nada de Spring ni JPA. Los adaptadores (REST, JPA) implementan interfaces definidas por la aplicación.

**Consecuencias:**
- (+) El dominio es testeable sin contexto Spring.
- (+) Cambiar PostgreSQL por otro motor solo requiere reemplazar el adaptador.
- (-) Más clases e interfaces para casos simples.
- (-) La curva de entrada es mayor para desarrolladores nuevos.

---

### ADR-2: Bloqueo pesimista para concurrencia

**Contexto:** dos retiros simultáneos sobre la misma cuenta no deben dejar saldo negativo. Se necesita un mecanismo de control de concurrencia.

**Opciones consideradas:**
- **Bloqueo optimista** (`@Version`): reintenta la operación si detecta conflicto.
- **Bloqueo pesimista** (`SELECT FOR UPDATE`): bloquea la fila antes de leer.
- **Sin bloqueo**: asumir que la validación de saldo es suficiente (incorrecto bajo concurrencia).

**Decisión:** bloqueo pesimista. Para operaciones financieras es preferible bloquear y esperar que reintentar, ya que un reintento puede fallar indefinidamente bajo alta contención y complica el manejo de errores en el cliente.

**Consecuencias:**
- (+) Garantía fuerte: imposible que dos transacciones simultáneas corrompan el saldo.
- (+) Sin lógica de reintento en el cliente.
- (-) Menor throughput bajo alta concurrencia sobre la misma cuenta.
- (-) Requiere orden de bloqueo explícito para evitar deadlocks en transferencias.

---

### ADR-3: Idempotencia por UUID de referencia

**Contexto:** los clientes pueden reenviar el mismo request por error de red. Sin protección, un depósito de $1000 podría ejecutarse dos veces.

**Opciones consideradas:**
- No implementar idempotencia (solo documentarla).
- Idempotencia por hash del body del request.
- Idempotencia por campo `reference` (UUID) en el body, con constraint `UNIQUE` en base de datos.

**Decisión:** campo `reference` opcional en el request. Si el cliente lo envía, el sistema lo usa; si no, genera uno internamente. La constraint `UNIQUE` en base de datos garantiza que ningún duplicado pase aunque dos hilos la verifiquen simultáneamente.

**Consecuencias:**
- (+) El cliente controla qué operaciones son idempotentes y cuáles no.
- (+) La base de datos es la última línea de defensa (no solo la validación en código).
- (+) Sirve como mecanismo de trazabilidad: el cliente puede rastrear cualquier transacción por su `reference`.
- (-) El cliente debe generar y conservar el UUID si quiere idempotencia garantizada.
