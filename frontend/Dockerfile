# 第一阶段：构建 Vue 静态资源
FROM node:22-alpine AS build
WORKDIR /app

# 构建时注入前端 API 前缀，生产环境由 Nginx 代理到后端
ARG VITE_API_BASE_PATH=/api
ENV VITE_API_BASE_PATH=$VITE_API_BASE_PATH

# 先安装依赖，再复制源码，提升 Docker 构建缓存命中率
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# 第二阶段：使用 Nginx 托管前端 dist
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
