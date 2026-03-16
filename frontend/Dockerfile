# frontend/Dockerfile
FROM node:20-alpine AS build

WORKDIR /app

# Kopieer alleen de package.json en package-lock.json eerst (snellere builds)
COPY package*.json ./

# Installeer dependencies
RUN npm install

# Kopieer de rest van de app
COPY . .

# Build de Angular app
RUN npm run build --prod

# Serve production build
FROM nginx:alpine

# Remove default nginx configuration
RUN rm -f /etc/nginx/conf.d/default.conf

# Kopieer nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf

COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
