# Node2：业务 Worker

在 **2 核 4 G** 其中一台 ECS（业务 Worker）上执行。需先完成 Node1 的 K3s server 安装并拿到 token。

## 1. 设置变量

将下面两个变量改为你的实际值（在 Node2 上执行前先写好）：

- **NODE1_IP**：Node1 的**内网 IP**（如 `172.x.x.x`），Node2 必须能访问该 IP 的 6443 端口。
- **K3S_TOKEN**：在 Node1 上执行 `sudo cat /var/lib/rancher/k3s/server/node-token` 得到的字符串。

## 2. 安装 K3s Agent（加入集群）

```bash
export K3S_URL="https://<NODE1_IP>:6443"
export K3S_TOKEN="<从上一步复制的 token>"
curl -sfL https://get.k3s.io | sh -
```

或使用当前目录脚本（先编辑脚本里的 NODE1_IP 和 K3S_TOKEN，再执行）：

```bash
chmod +x install-agent.sh
# 编辑 install-agent.sh，填入 NODE1_IP 和 K3S_TOKEN
./install-agent.sh
```

## 3. 确认

在 Node1 或已配置 kubectl 的操作机上执行 `kubectl get nodes`，应看到 Node2 为 Ready。随后由操作机为该节点打标签：`kubectl label nodes <Node2主机名> node=node2 --overwrite`。
