#!/usr/bin/env bash
# 在已配置 kubectl 的操作机上执行，为 Node2/Node3/Node4 打标签
# 执行前请先：kubectl get nodes 查看节点名，并修改下面 NODE2_NAME、NODE3_NAME、NODE4_NAME

NODE2_NAME=""   # 改为 Node2 的主机名（kubectl get nodes 第一列）
NODE3_NAME=""
NODE4_NAME=""

if [ -z "$NODE2_NAME" ] || [ -z "$NODE3_NAME" ] || [ -z "$NODE4_NAME" ]; then
  echo "当前节点列表："
  kubectl get nodes
  echo "请编辑本脚本，填写 NODE2_NAME、NODE3_NAME、NODE4_NAME（与上表 NAME 列一致）"
  exit 1
fi

kubectl label nodes "$NODE2_NAME" node=node2 --overwrite
kubectl label nodes "$NODE3_NAME" node=node3 --overwrite
kubectl label nodes "$NODE4_NAME" node=node4 --overwrite
echo "已为 Node2/Node3/Node4 设置 node 标签。"
