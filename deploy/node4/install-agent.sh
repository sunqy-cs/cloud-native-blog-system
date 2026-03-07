#!/usr/bin/env bash
# Node4：以 Agent 身份加入 K3s 集群（业务 Worker）
# 使用前请先修改下面两行：填 Node1 内网 IP 和 token

NODE1_IP="172.19.155.167"   # 改为 Node1 内网 IP
K3S_TOKEN="K10ff1afcc3a1fc1569b0bd4e2127e4275d04604715c894ff144b95878737bfd54e::server:a79d1db865df11d270a5ffbd7ccb1ad8"           # 改为 Node1 上 /var/lib/rancher/k3s/server/node-token 的内容

if [ -z "$K3S_TOKEN" ] || [ "$NODE1_IP" = "172.x.x.x" ]; then
  echo "请先编辑本脚本，填写 NODE1_IP 和 K3S_TOKEN"
  exit 1
fi

# 安装前配置国内镜像源，避免拉取 docker.io 超时（与 Node1 一致）
sudo mkdir -p /etc/rancher/k3s
sudo tee /etc/rancher/k3s/registries.yaml <<EOF
mirrors:
  docker.io:
    endpoint:
      - "https://docker.m.daocloud.io"
EOF

# 配置并启动 k3s agent（不下载，仅用本机已有 k3s）
K3S_BIN="${K3S_BIN:-/usr/local/bin/k3s}"

sudo tee /etc/systemd/system/k3s-agent.service <<EOF
[Unit]
Description=Lightweight Kubernetes - Agent
Documentation=https://k3s.io
After=network-online.target
Wants=network-online.target

[Service]
Type=notify
Environment="K3S_URL=https://${NODE1_IP}:6443"
Environment="K3S_TOKEN=${K3S_TOKEN}"
ExecStart=${K3S_BIN} agent
KillMode=process
Delegate=yes
Restart=always
RestartSec=2s

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable --now k3s-agent
echo "k3s-agent 已配置并启动，可用: sudo systemctl status k3s-agent"
