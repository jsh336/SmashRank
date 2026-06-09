# SmashRank 🎮

SmashRank es una plataforma web diseñada para gestionar perfiles de usuarios y calcular el ranking competitivo de jugadores de **Super Smash Bros. Ultimate**, utilizando un sistema de puntuación dinámico para clasificar a los mejores luchadores de la comunidad a partir de los datos de torneos reales.

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
