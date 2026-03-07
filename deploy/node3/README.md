# Node3：业务 Worker

在 **2 核 4 G** 另一台 ECS（业务 Worker）上执行。需先完成 Node1 的 K3s server 安装并拿到 token。

## 1. 设置变量

- **NODE1_IP**：Node1 的**内网 IP**（如 `172.x.x.x`）。
- **K3S_TOKEN**：Node1 上 `sudo cat /var/lib/rancher/k3s/server/node-token` 的输出。

## 2. 安装 K3s Agent

```bash
export K3S_URL="https://<NODE1_IP>:6443"
export K3S_TOKEN="<token>"
curl -sfL https://get.k3s.io | sh -
```

或编辑并执行当前目录的 `install-agent.sh`（与 node2 用法相同，仅需填同一份 NODE1_IP 和 K3S_TOKEN）。

## 3. 打标签

在操作机：`kubectl label nodes <Node3主机名> node=node3 --overwrite`
