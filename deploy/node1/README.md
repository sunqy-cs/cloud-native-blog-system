# Node1：K3s 控制节点

在 **2 核 2 G**（配置最差）那台 ECS 上执行。控制面只做调度、不跑业务与数据，资源需求低。建议使用 root 或具备 sudo 权限的用户。

## 1. 安装 K3s Server

```bash
# 若需指定外网/内网访问，可设 INSTALL_K3S_EXEC，例如：
# export INSTALL_K3S_EXEC="--tls-san <Node1公网IP>"   # 可选，从集群外 kubectl 时用
curl -sfL https://get.k3s.io | sh -
```

或直接执行当前目录下的脚本（与上面等价）：

```bash
chmod +x install-server.sh
./install-server.sh
```

## 2. 查看并保存 Token

Node2/Node3/Node4 加入集群需要用到 token：

```bash
sudo cat /var/lib/rancher/k3s/server/node-token
```

将输出复制保存，作为 **K3S_TOKEN**，在 node2/node3/node4 的 install-agent 中使用。

## 3. 本机 kubeconfig（在 Node1 上用 kubectl）

```bash
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $(id -u):$(id -g) ~/.kube/config
# 若从本机连，将 127.0.0.1 改为 Node1 内网 IP 或 127.0.0.1 均可
kubectl get nodes
```

## 4. 在操作机上使用 kubectl（从 Node1 拷出 kubeconfig）

在**你的电脑**或跳板机上：

```bash
# 在 Node1 上执行，把 kubeconfig 内容拷出来（或 scp）
scp root@<NODE1_IP>:/etc/rancher/k3s/k3s.yaml ~/.kube/config
# 编辑 ~/.kube/config，把 server 里的 127.0.0.1 改成 Node1 的公网 IP 或内网 IP（能访问到 6443 的地址）
kubectl get nodes
```

## 5. 给 Node1 打标签与污点（避免业务 Pod 调度到控制节点）

在**已配置好 kubectl 的操作机**上执行（先等 Node2/Node3/Node4 都 join 成功后）：

```bash
# 假设 Node1 的节点名为 node1 或类似，先用 kubectl get nodes 看名字
NODE1_NAME=$(kubectl get nodes -o jsonpath='{.items[?(@.metadata.labels.node-role\.kubernetes\.io/control-plane=="")].metadata.name}')
# 若上面为空，则直接指定节点名，例如：
# NODE1_NAME=node1

kubectl label nodes $NODE1_NAME node=node1 --overwrite
kubectl taint nodes $NODE1_NAME node-role.kubernetes.io/control-plane:NoSchedule --overwrite
```

或执行当前目录下的脚本（需在已配置 kubectl 且能连集群的机器上跑）：

```bash
./label-taint.sh
```

## 6. 为 Node2、Node3、Node4 打标签

在操作机上（根据实际节点名修改）：

```bash
kubectl label nodes <Node2主机名> node=node2 --overwrite
kubectl label nodes <Node3主机名> node=node3 --overwrite
kubectl label nodes <Node4主机名> node=node4 --overwrite
```

之后部署时用 `nodeSelector: node: node2` 等即可把 Pod 固定到对应节点。
