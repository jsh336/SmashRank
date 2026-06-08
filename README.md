# SmashRank 🎮

Sistema de clasificación y ranking para competidores de **Super Smash Bros**, integrado con la API GraphQL de **[Start.gg](https://start.gg)**.

---

## 🛠️ Stack Tecnológico

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 (LTS) | Lenguaje principal |
| Spring Boot | 3.4.0 | Framework base |
| Spring Web | 3.4.0 | API REST |
| Spring Data JPA | 3.4.0 | Persistencia ORM |
| Spring WebFlux | 3.4.0 | WebClient reactivo → Start.gg |
| PostgreSQL | 16 | Base de datos |
| Lombok | latest | Reducción de boilerplate |
| Docker | - | Contenedores |

---

## 📁 Estructura del Proyecto

```
smashrank/
├── src/
│   ├── main/
│   │   ├── java/com/smashrank/
│   │   │   ├── SmashRankApplication.java        # Entry point
│   │   │   ├── config/
│   │   │   │   └── WebClientConfig.java          # WebClient para Start.gg
│   │   │   ├── controller/
│   │   │   │   ├── PlayerController.java         # REST /api/v1/players
│   │   │   │   └── StartGgController.java        # REST /api/v1/startgg
│   │   │   ├── service/
│   │   │   │   ├── PlayerService.java            # Lógica de negocio players
│   │   │   │   └── StartGgService.java           # Integración Start.gg GraphQL
│   │   │   ├── repository/
│   │   │   │   ├── PlayerRepository.java         # JPA Player
│   │   │   │   └── TournamentResultRepository.java
│   │   │   ├── entity/
│   │   │   │   ├── Player.java                   # Entidad jugador
│   │   │   │   └── TournamentResult.java         # Entidad resultado torneo
│   │   │   ├── dto/
│   │   │   │   ├── PlayerRequestDTO.java
│   │   │   │   ├── PlayerResponseDTO.java
│   │   │   │   ├── TournamentResultResponseDTO.java
│   │   │   │   └── GraphQLRequestDTO.java
│   │   │   └── exception/
│   │   │       ├── ResourceNotFoundException.java
│   │   │       ├── DuplicateResourceException.java
│   │   │       └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── java/com/smashrank/
│       │   └── SmashRankApplicationTests.java
│       └── resources/
│           └── application.properties            # H2 para tests
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── .gitignore
```

---

## 🚀 Inicio Rápido

### Prerrequisitos
- Java 21+
- Maven 3.9+
- Docker & Docker Compose

### 1. Clonar y configurar el token de Start.gg

Edita `src/main/resources/application.properties` y reemplaza:
```properties
startgg.api.token=YOUR_STARTGG_BEARER_TOKEN_HERE
```
> Obtén tu token en: https://developer.start.gg/docs/authentication

### 2. Levantar solo PostgreSQL con Docker

```bash
docker-compose up postgres-db -d
```

### 3. Ejecutar la aplicación localmente

```bash
mvn spring-boot:run
```

### 4. O levantar todo el stack con Docker Compose

```bash
docker-compose up --build
```

La aplicación estará disponible en: `http://localhost:8080`

---

## 📡 API REST — Endpoints

### Players (`/api/v1/players`)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/v1/players` | Crear jugador |
| `GET` | `/api/v1/players` | Listar todos (por ranking) |
| `GET` | `/api/v1/players?search=fox` | Buscar por gamertag |
| `GET` | `/api/v1/players/top?limit=10` | Top N del ranking |
| `GET` | `/api/v1/players/{id}` | Obtener por ID |
| `GET` | `/api/v1/players/gamertag/{tag}` | Obtener por gamertag |
| `PUT` | `/api/v1/players/{id}` | Actualizar jugador |
| `PATCH` | `/api/v1/players/{id}/rank-points?delta=50` | Sumar/restar puntos |
| `POST` | `/api/v1/players/recalculate-ranking` | Recalcular posiciones |
| `DELETE` | `/api/v1/players/{id}` | Eliminar jugador |

### Start.gg Integration (`/api/v1/startgg`)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/v1/startgg/players/{id}/sync` | Sincronizar jugador desde Start.gg |
| `POST` | `/api/v1/startgg/players/{id}/tournaments/sync` | Sincronizar torneos |
| `GET` | `/api/v1/startgg/players/{id}/tournaments` | Obtener torneos guardados |
| `POST` | `/api/v1/startgg/graphql` | Ejecutar query GraphQL custom |

---

## 🔧 Configuración (`application.properties`)

```properties
# PostgreSQL Docker
spring.datasource.url=jdbc:postgresql://postgres-db:5432/mi_base_datos
spring.datasource.username=mi_usuario
spring.datasource.password=mi_password

# Start.gg API
startgg.api.url=https://api.start.gg/gql/alpha
startgg.api.token=TU_TOKEN_AQUI
```

---

## 🔌 Uso del WebClient (Start.gg GraphQL)

```java
// Inyecta el servicio y ejecuta una query personalizada
Mono<Map> response = startGgService.executeGraphQLQuery("""
    query {
      tournament(slug: "tournament/mi-torneo") {
        id
        name
        numAttendees
      }
    }
""", null);
```

---

## 🐳 Variables de Entorno (Docker)

| Variable | Descripción | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | URL JDBC PostgreSQL | `jdbc:postgresql://postgres-db:5432/mi_base_datos` |
| `SPRING_DATASOURCE_USERNAME` | Usuario DB | `mi_usuario` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña DB | `mi_password` |
| `STARTGG_API_TOKEN` | Bearer token Start.gg | `YOUR_TOKEN_HERE` |
