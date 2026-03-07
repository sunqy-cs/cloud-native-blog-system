#!/usr/bin/env bash
# Node1：安装 K3s Server（控制面）
# 在 2 核 2 G（配置最差）那台 ECS 上执行

set -e
curl -sfL https://get.k3s.io | sh -
echo "---"
echo "安装完成后请执行: sudo cat /var/lib/rancher/k3s/server/node-token"
echo "将 token 保存，供 Node2/Node3/Node4 执行 install-agent.sh 时使用。"
