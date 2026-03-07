#!/usr/bin/env bash
# 在已配置 kubectl 且能连上集群的机器上执行（通常是操作机或 Node1）
# 为 Node1 打 label 和 taint，避免业务 Pod 调度到控制节点

set -e
# 控制面节点带有 node-role.kubernetes.io/control-plane（K3s 默认），用 -l 选择更可靠
NODE1_NAME=$(kubectl get nodes -l node-role.kubernetes.io/control-plane= -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true)
if [ -z "$NODE1_NAME" ]; then
  echo "未找到 control-plane 节点，请手动指定： export NODE1_NAME=控制节点名 && $0"
  exit 1
fi
echo "Node1 节点名: $NODE1_NAME"
kubectl label nodes "$NODE1_NAME" node=node1 --overwrite
kubectl taint nodes "$NODE1_NAME" node-role.kubernetes.io/control-plane:NoSchedule --overwrite 2>/dev/null || true
echo "已为 Node1 设置 node=node1 并添加 NoSchedule 污点。"
