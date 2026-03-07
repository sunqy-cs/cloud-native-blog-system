# Node4：数据节点

在 **4 核 8 G**（配置最好）那台 ECS 上执行。数据节点跑 MySQL、Nacos、ES、MinIO、PG、可观测等，用配置最好的机器保证稳定。

## 1. 设置变量

- **NODE1_IP**：Node1 的**内网 IP**（如 `172.x.x.x`）。
- **K3S_TOKEN**：Node1 上 `sudo cat /var/lib/rancher/k3s/server/node-token` 的输出。

## 2. 安装 K3s Agent

```bash
export K3S_URL="https://<NODE1_IP>:6443"
export K3S_TOKEN="<token>"
curl -sfL https://get.k3s.io | sh -
```

或编辑并执行当前目录的 `install-agent.sh`（填同一份 NODE1_IP 和 K3S_TOKEN）。

## 3. 打标签

在操作机：`kubectl label nodes <Node4主机名> node=node4 --overwrite`

## 4. 数据盘与 StorageClass（第一步：存储准备）

Node4 将跑 MySQL、Nacos、ES、MinIO、PG、Loki 等，需先准备数据盘并让 K3s 使用，避免占满系统盘。

### 4.1 在 Node4 上挂载数据盘

1. **在云控制台**为 Node4 实例挂载一块云盘（如 100GiB），并**连接/挂载到实例**（阿里云需在控制台点「挂载」并选择实例）。
2. **SSH 到 Node4**，确认块设备（常见为 `/dev/vdb`）：
   ```bash
   lsblk
   ```
3. 执行本目录下的脚本（默认使用 `/dev/vdb`，若不是请先设置变量）：
   ```bash
   # 若数据盘为 /dev/vdb
   chmod +x /root/workspace/node4/setup-data-disk.sh
   /root/workspace/node4/setup-data-disk.sh

   # 若为其他设备，例如 /dev/vdc
   DATA_DISK=/dev/vdc /root/workspace/node4/setup-data-disk.sh
   ```
4. 若希望 **K3s 的 PVC 都落在数据盘**（推荐），按脚本末尾提示执行「可选」步骤：停 k3s-agent → 把数据盘绑定到 `/var/lib/rancher/k3s/storage` → 写 fstab → 启动 k3s-agent。

### 4.2 确认 StorageClass

K3s 默认自带 **local-path** StorageClass，无需再建。只要后续有状态 workload 都加 **nodeSelector: node=node4**，PVC 会调度到 Node4 并在其存储目录创建。

在**操作机或 Node1** 上验证：

```bash
kubectl get storageclass
kubectl get sc local-path -o yaml   # 确认存在且通常为 default
```

若需测试 PVC 是否落在 Node4 的数据盘，可创建一条测试 PVC 和 Pod（nodeSelector=node4），在 Node4 上查看 `/var/lib/rancher/k3s/storage`（或你绑定后的数据盘）是否出现对应目录。

---

## 5. 部署 Node4 中间件（K8s 清单）

本目录下所有 YAML 均带 `nodeSelector: node=node4`、有状态组件使用 `storageClassName: local-path`，在**已配置 kubeconfig 的操作机或 Node1** 上按顺序 apply 即可。

### 5.1 一次性准备（可选）

- **Node4 上 Elasticsearch**：若 ES Pod 启动失败（vm.map_count），在 Node4 执行：
  ```bash
  sudo sysctl -w vm.max_map_count=262144
  echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf
  ```
- **MySQL 完整表结构**：当前 `mysql-configmap.yaml` 只含建库；若需整库表结构，在仓库根目录执行后再 apply mysql：
  ```bash
  kubectl create configmap mysql-init-sql --from-file=infrastructure/mysql/ -n blog-infra --dry-run=client -o yaml | kubectl apply -f -
  ```

### 5.2 推荐 apply 顺序

```bash
# 1. 命名空间
kubectl apply -f namespace.yaml

# 2. 配置与密钥（ConfigMap/Secret 已在各组件 YAML 内，若单独创建过 mysql-init-sql 见上）
kubectl apply -f mysql-configmap.yaml
kubectl apply -f postgres-configmap.yaml
kubectl apply -f prometheus-configmap.yaml
kubectl apply -f loki-configmap.yaml
kubectl apply -f promtail-configmap.yaml

# 3. 中间件（先数据库与 Nacos，再其余）
kubectl apply -f mysql.yaml
kubectl apply -f nacos.yaml
kubectl apply -f minio.yaml
kubectl apply -f elasticsearch.yaml
kubectl apply -f postgres.yaml

# 4. 可观测
kubectl apply -f prometheus-deploy.yaml
kubectl apply -f grafana.yaml
kubectl apply -f jaeger.yaml
kubectl apply -f loki.yaml
kubectl apply -f promtail.yaml
```

或一次性（保证上面顺序即可）：

```bash
kubectl apply -f namespace.yaml
kubectl apply -f mysql-configmap.yaml -f postgres-configmap.yaml -f prometheus-configmap.yaml -f loki-configmap.yaml -f promtail-configmap.yaml
kubectl apply -f mysql.yaml -f nacos.yaml -f minio.yaml -f elasticsearch.yaml -f postgres.yaml
kubectl apply -f prometheus-deploy.yaml -f grafana.yaml -f jaeger.yaml -f loki.yaml -f promtail.yaml
```

### 5.3 清单与访问方式

| 文件 | 组件 | 集群内访问（同 namespace） |
|------|------|----------------------------|
| mysql.yaml + mysql-configmap.yaml | MySQL | `mysql.blog-infra.svc.cluster.local:3306`，库 `blog`，root 见 Secret `mysql-secret` |
| nacos.yaml | Nacos | `nacos.blog-infra.svc.cluster.local:8848` |
| minio.yaml | MinIO | API `minio.blog-infra.svc.cluster.local:9000`，控制台 9001 |
| elasticsearch.yaml | Elasticsearch | `elasticsearch.blog-infra.svc.cluster.local:9200` |
| postgres.yaml + postgres-configmap.yaml | PostgreSQL(pgvector) | `postgres.blog-infra.svc.cluster.local:5432`，库 `blog_vector`，用户 blog |
| prometheus-deploy.yaml | Prometheus | `prometheus.blog-infra.svc.cluster.local:9090` |
| grafana.yaml | Grafana | `grafana.blog-infra.svc.cluster.local:3000`，admin/admin |
| jaeger.yaml | Jaeger | `jaeger.blog-infra.svc.cluster.local:16686`（UI） |
| loki.yaml | Loki | `loki.blog-infra.svc.cluster.local:3100` |
| promtail.yaml | Promtail | DaemonSet，Node2/Node3/Node4 各一 Pod，推日志到 Loki |

对外访问需通过 NodePort 或 Ingress 暴露对应 Service，业务微服务在 K8s 内用上述集群内地址连接即可。
