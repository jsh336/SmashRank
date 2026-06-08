# Stage 1: Construir la aplicación Angular
FROM node:20-alpine AS build
WORKDIR /app

# Copiar configuración de dependencias
COPY package*.json ./
RUN npm install

# Copiar el código fuente y compilar
COPY . .
RUN npm run build -- --configuration=production

# Stage 2: Servir la aplicación con Nginx
FROM nginx:1.25-alpine

# Copiar el build compilado al directorio web de Nginx
COPY --from=build /app/dist/smashrank-frontend /usr/share/nginx/html

# Copiar configuración de Nginx para soportar rutas de Angular
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
