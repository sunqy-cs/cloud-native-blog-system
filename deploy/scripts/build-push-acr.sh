#!/usr/bin/env bash
# 构建并推送所有业务镜像到阿里云 ACR


set -e
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# 改为你的 ACR 地址（如 registry.cn-beijing.aliyuncs.com）和命名空间
ACR_REGISTRY="${ACR_REGISTRY:-registry.cn-beijing.aliyuncs.com}"
ACR_NAMESPACE="${ACR_NAMESPACE:-your-namespace}"
TAG="${TAG:-latest}"

if [ "$ACR_NAMESPACE" = "your-namespace" ]; then
  echo "请设置 ACR_NAMESPACE 或修改脚本中的 ACR_NAMESPACE（阿里云 ACR 命名空间）"
  exit 1
fi

echo "ACR: $ACR_REGISTRY/$ACR_NAMESPACE, TAG: $TAG"
cd "$REPO_ROOT"

services=(gateway-service user-service file-service content-service interaction-service ai-service search-service)
for svc in "${services[@]}"; do
  echo "--- Build $svc ---"
  docker build -t "$ACR_REGISTRY/$ACR_NAMESPACE/$svc:$TAG" -f "services/$svc/Dockerfile" "services/$svc"
  echo "--- Push $svc ---"
  docker push "$ACR_REGISTRY/$ACR_NAMESPACE/$svc:$TAG"
done

echo "--- Build frontend ---"
docker build -t "$ACR_REGISTRY/$ACR_NAMESPACE/frontend:$TAG" -f frontend/Dockerfile frontend
echo "--- Push frontend ---"
docker push "$ACR_REGISTRY/$ACR_NAMESPACE/frontend:$TAG"

echo "Done. 镜像列表："
for svc in "${services[@]}"; do echo "  $ACR_REGISTRY/$ACR_NAMESPACE/$svc:$TAG"; done
echo "  $ACR_REGISTRY/$ACR_NAMESPACE/frontend:$TAG"
