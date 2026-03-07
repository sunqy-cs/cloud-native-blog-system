# 前端容器化：构建产物放进 Nginx，和微服务一样打成独立镜像
# 阶段一：用 Node 构建
FROM node:20-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# 阶段二：用 Nginx 提供静态文件（SPA 路由 fallback 到 index.html）
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
