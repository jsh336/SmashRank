# SmashRank Frontend — Angular 17 & Tailwind CSS

Este es el frontend del proyecto SmashRank, construido con **Angular 17** usando componentes independientes (**standalone**) y estilizado con **Tailwind CSS**.

## Estructura de Directorios

El proyecto sigue una estructura personalizada y limpia:
- `/public` - Recursos públicos
- `/src/environments` - Configuración de URLs de API para desarrollo/producción
- `/src/app/core` - Servicios principales (ej. `RankingService`) e interceptores
- `/src/app/models` - Modelos de datos TypeScript (ej. `PlayerRanking`)
- `/src/app/shared` - Elementos compartidos
- Nomenclatura del componente principal: `app.ts`, `app.html` y `app.scss`.

## Requisitos Previos

Necesitas tener instalado **Node.js** (versión 18 o superior recomendada) en tu sistema.

## Ejecución en Local (Desarrollo)

1. Entra en este directorio desde la terminal:
   ```bash
   cd frontend
   ```
2. Instala las dependencias necesarias:
   ```bash
   npm install
   ```
3. Inicia el servidor de desarrollo de Angular:
   ```bash
   npm start
   ```
4. Abre [http://localhost:4200](http://localhost:4200) en tu navegador.

*Nota: La aplicación cuenta con un fallback automático a datos de demo realista en caso de que el backend de Spring Boot no esté levantado en `http://localhost:8080`.*

## Dockerización

Puedes construir y ejecutar la imagen Docker del frontend con los siguientes comandos:

```bash
# Construir la imagen
docker build -t smashrank-frontend .

# Ejecutar el contenedor en el puerto 80
docker run -d -p 80:80 smashrank-frontend
```
