# 业务服务部署（Node2/Node3）

Gateway、7 个微服务、前端，2 副本 + 反亲和，调度在 Node2/Node3；镜像来自阿里云 ACR。

## 1. 构建并推送镜像到 ACR

在**本机**（或 CI）执行：

```bash
# 1. 登录阿里云 ACR（在 ACR 控制台可查看登录命令）
docker login --username=你的账号 registry.cn-beijing.aliyuncs.com

# 2. 设置命名空间并构建推送（在项目根目录执行）
export ACR_REGISTRY=registry.cn-beijing.aliyuncs.com
export ACR_NAMESPACE=你的ACR命名空间
bash deploy/scripts/build-push-acr.sh
```

或修改 `deploy/scripts/build-push-acr.sh` 里的 `ACR_NAMESPACE` 后直接运行。

## 2. 修改镜像地址

将本目录下所有 `*.yaml` 中的 **`your-namespace`** 替换为你的 ACR 命名空间（与上面一致），例如：

```bash
# Linux/Mac
sed -i 's/your-namespace/你的ACR命名空间/g' deploy/business/*.yaml
```

或手动把 `registry.cn-beijing.aliyuncs.com/your-namespace/` 改成 `registry.cn-beijing.aliyuncs.com/你的命名空间/`。

若 ACR 地域不是北京，把 `registry.cn-beijing.aliyuncs.com` 一并改为对应地域地址。

## 3. 配置拉取密钥（ACR 私有仓库时）

若命名空间为私有，需在集群创建拉取密钥，供 blog-app 使用：

```bash
kubectl create secret docker-registry acr-secret \
  --docker-server=registry.cn-beijing.aliyuncs.com \
  --docker-username=你的账号 \
  --docker-password=你的ACR密码 \
  -n blog-app
```

并在每个 Deployment 的 `spec.template.spec` 下增加：

```yaml
imagePullSecrets:
  - name: acr-secret
```

（若为公开仓库可省略。）

## 4. 部署顺序

在**已配置 kubeconfig 的操作机**上，在项目根目录执行：

```bash
# 命名空间与公共配置
kubectl apply -f deploy/business/namespace.yaml
kubectl apply -f deploy/business/app-config.yaml

# 业务服务（先 Gateway，再其余；或一次性）
kubectl apply -f deploy/business/gateway-service.yaml
kubectl apply -f deploy/business/user-service.yaml
kubectl apply -f deploy/business/file-service.yaml
kubectl apply -f deploy/business/content-service.yaml
kubectl apply -f deploy/business/interaction-service.yaml
kubectl apply -f deploy/business/ai-service.yaml
kubectl apply -f deploy/business/search-service.yaml
kubectl apply -f deploy/business/frontend.yaml
```

## 5. 访问

- **Gateway**：NodePort 30080。任选 Node2 或 Node3 的**节点 IP**，浏览器访问 `http://<节点IP>:30080`。
- **前端**：NodePort 30081，`http://<节点IP>:30081`。
- 生产建议用云厂商负载均衡或 Ingress 把 80/443 指到 Node2、Node3 的 30080/30081，实现入口高可用。

## 6. AI 服务密钥（可选）

ai-service 如需 DeepSeek/DashScope，将 API Key 写入 Secret 并在 `ai-service.yaml` 的 env 中引用（例如 `DEEPSEEK_API_KEY`、`DASHSCOPE_API_KEY`），勿写死在 YAML 里。
