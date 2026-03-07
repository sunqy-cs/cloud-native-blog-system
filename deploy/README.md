# 四节点 K3s 部署配置

按 **node1（控制）→ node2/node3/node4（Worker）** 顺序执行。部署前请先完成 `docs/部署前准备与步骤.md` 中的网络、系统准备。

## 目录说明

| 目录 | 角色 | 内容 |
|------|------|------|
| **node1/** | K3s 控制节点（**2 核 2 G**，配置最差那台） | 安装 K3s server、取 token、打标签/污点 |
| **node2/** | 业务 Worker（2 核 4 G） | 执行 join 加入集群 |
| **node3/** | 业务 Worker（2 核 4 G） | 同上 |
| **node4/** | 数据节点（**4 核 8 G**，配置最好那台） | 同上 |

## 执行顺序

1. **在 Node1（4 核 8 G）**：执行 `node1/install-server.sh`，记下输出的 token；再执行 `node1/label-taint.sh`。
2. **在 Node2、Node3、Node4**：分别执行 `node2/install-agent.sh`、`node3/install-agent.sh`、`node4/install-agent.sh`（先填好各目录 README 中的 `NODE1_IP` 和 `K3S_TOKEN`）。
3. **在操作机**：从 Node1 拷贝 kubeconfig（见 `node1/README.md`），执行 `kubectl get nodes` 确认 4 台 Ready；执行 `node1/label-taint.sh` 为 Node1 打标签与污点；编辑并执行 **`label-nodes.sh`**（填 Node2/Node3/Node4 主机名）为三台 Worker 打 `node=node2`、`node=node3`、`node=node4`。

## 变量说明

- **NODE1_IP**：Node1 的**内网 IP**（如 172.x.x.x），Node2/3/4 需能访问该 IP 的 6443 端口。
- **K3S_TOKEN**：在 Node1 安装完成后，从 `/var/lib/rancher/k3s/server/node-token` 读取。
