FROM node:20-alpine AS build
WORKDIR /build

COPY package*.json ./
RUN npm ci || npm install

COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /build/dist /usr/share/nginx/html
EXPOSE 80
