# qd-137 热气球飞行基地地面固定支架飞行航线风力区间匹配系统

## 项目简介

热气球飞行基地地面固定支架、飞行航线与风力区间匹配系统。项目包含 Vue/Vite 前端、Spring Boot 后端、MySQL 与 Redis，已按依赖层缓存和固定端口交付链路规范整理。

## 航线支架绑定：身份核对与角色权限

「航线支架绑定」页已按角色收窄，先做身份核对再操作：

- **调度（DISPATCHER）**：可改挂任意航线的支架（绑定 / 解绑 / 试配）。
- **放飞员（LAUNCH_OPERATOR）**：只读，仅可查看自己当班航线已经挂上的支架，不能改挂，也不能访问其他航线。

后端 `/api/bindings/**` 全部接口逐请求核对身份（请求头 `X-Operator-Code` / `X-Operator-Password`），未核对返回 401，越权返回 403。身份核对接口：`POST /api/auth/verify`。

初始账号（口令经 SHA-256 存储）：

| 工号 | 口令 | 角色 | 当班航线 |
| --- | --- | --- | --- |
| DISP-001 | dispatch123 | 调度 | 全部航线 |
| OP-001 | pilot123 | 放飞员 | RTE-001、RTE-002 |
| OP-002 | pilot123 | 放飞员 | RTE-003 |

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
