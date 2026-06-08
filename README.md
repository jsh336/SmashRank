# SmashRank 🎮

SmashRank es una plataforma web diseñada para gestionar perfiles de usuarios y calcular el ranking competitivo de jugadores de **Super Smash Bros. Ultimate**, utilizando un sistema de puntuación dinámico para clasificar a los mejores luchadores de la comunidad a partir de los datos de torneos reales.

Este proyecto ha sido desarrollado como parte del Proyecto Individual de la Universidad, integrando tecnologías modernas de Frontend, Backend e Infraestructura.

## 🚀 Características Principales

* **Sistema de Ranking:** Cálculo automático de la clasificación de jugadores basado en su rendimiento y resultados obtenidos.
* **Integración con Start.gg:** Conexión en tiempo real con la API de Start.gg para importar automáticamente los resultados de los torneos y enfrentamientos.
* **Perfiles de Jugadores:** Interfaz Maestro-Detalle para visualizar las estadísticas de cada jugador, su posición en el ranking y su evolución competitiva.
* **Panel de Administración:** Herramientas exclusivas para que los administradores puedan gestionar el sistema, sincronizar nuevos torneos y supervisar las clasificaciones.
* **Autenticación Segura:** Acceso protegido y gestión de roles mediante el protocolo **OAuth2**.

## 🛠️ Stack Tecnológico

**Frontend**
* **Framework:** Angular (Última versión).
* **Diseño:** Arquitectura basada en componentes con patrón Maestro + Detalle.
* **Librerías:** RxJS para la gestión de estados reactivos y Angular Material para una interfaz limpia, moderna y responsiva.

**Backend**
* **Framework:** Spring Boot (Java).
* **Seguridad:** Implementación de **OAuth2** para el control de acceso y autenticación de usuarios y administradores.
* **API:** Arquitectura RESTful para la comunicación con el Frontend.

**Datos Externos y Consultas**
* **API Externa:** Integración con la API de **Start.gg** para la obtención de datos oficiales de torneos de Super Smash Bros. Ultimate.
* **Lenguaje de Consultas:** Uso de **GraphQL** para realizar peticiones eficientes, precisas y optimizadas a la API de Start.gg.

**Base de Datos**
* **Persistencia:** PostgreSQL. Base de datos relacional encargada de almacenar de forma segura los perfiles de los usuarios, los roles y los registros del ranking calculado.

**Infraestructura**
* **Docker:** Contenedores para soporte al desarrollo local y empaquetado final para despliegue unificado (Frontend, Backend y PostgresDB) mediante `docker-compose`.

## 📂 Estructura del Repositorio

* `/frontend`: Aplicación SPA en Angular.
* `/backend`: API REST en Spring Boot con la lógica de negocio, seguridad OAuth2 y consumo de GraphQL.
* `/docker`: Archivos de configuración de Docker y `docker-compose.yml` para levantar la base de datos y los servicios.

## 📋 Requisitos del Proyecto (Cumplimiento)

* **Gestión de Tareas:** Uso de GitHub Projects / Issues para la planificación y el seguimiento del desarrollo individual.
* **Control de Versiones:** Repositorio en GitHub con un historial de commits limpio y organizado que refleja la evolución del proyecto.
* **Persistencia:** Base de Datos relacional PostgreSQL integrada de forma nativa en el Backend.
* **Patrón Maestro-Detalle:** Implementado exhaustivamente en la visualización del perfil de los jugadores en Angular.
* **Dockerización:** Proyecto completamente empaquetado y listo para producción con Docker.

## 🤝 Colaboradores

* [@jsh336](https://github.com/jsh336)

## 📄 Licencia

Este proyecto es para fines educativos en el ámbito universitario.
