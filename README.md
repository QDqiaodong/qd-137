# qd-137 热气球飞行基地地面固定支架飞行航线风力区间匹配系统

## 项目简介

热气球飞行基地地面固定支架、飞行航线与风力区间匹配系统。项目包含 Vue/Vite 前端、Spring Boot 后端、MySQL 与 Redis，已按依赖层缓存和固定端口交付链路规范整理。

## 访问地址

- 前端地址: [http://localhost:8137](http://localhost:8137)
- 127.0.0.1 地址: [http://127.0.0.1:8137](http://127.0.0.1:8137)
- 后端 API: http://localhost:8147/api

## 端口

- 前端: 8137
- 后端: 8147
- MySQL: 3363
- Redis: 6436

## 编译与启动

```bash
cd backend
mvn compile -q

cd ../frontend
npm ci
npm run build

cd ..
docker compose up -d --build
```

Docker Compose 端口均绑定到 `127.0.0.1`，镜像基础地址通过 `.env` 中的 `DOCKER_REGISTRY` 统一控制。
