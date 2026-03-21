# 敏感信息与 GitHub 推送

## 为何 push 被拒绝

GitHub **密钥扫描**会拒绝包含云厂商 AccessKey、密码等明文的提交。`user-service` 的 `application.yml` 中曾写入阿里云密钥时，会导致 `remote rejected`。

弹窗若同时出现 `Connection was reset`，多为网络/代理问题，可与密钥问题**同时存在**；先按本文处理密钥与历史，再检查网络或使用代理/VPN 访问 GitHub。

## 立刻要做的事（密钥已可能泄露）

1. 登录 [阿里云 RAM 控制台](https://ram.console.aliyun.com/manage/ak)，**禁用并删除**曾被提交的旧 AccessKey，**新建**一对密钥。
2. 新密钥只放在本机：环境变量或 `application-local.yml`（已 `.gitignore`），**不要**再写进会提交的 `application.yml`。

## 当前仓库约定

- **提交中的** `services/user-service/src/main/resources/application.yml` 仅使用占位符与环境变量：
  - `ALIYUN_DYPNS_ENABLED`（默认 `false`）
  - `ALIYUN_DYPNS_ACCESS_KEY_ID`
  - `ALIYUN_DYPNS_ACCESS_KEY_SECRET`
- 本地调试：复制 `application-local.yml.example` → `application-local.yml`，并设置 `spring.profiles.active=local`。

## 若 GitHub 仍不允许推送（历史记录里仍有密钥）

仅修改当前文件不够，需从 **Git 历史**中去掉密钥或换新仓库。

### 方案 A：密钥只出现在最近几条未推送/可丢弃的提交

若坏提交尚未被他人基于开发，可交互式变基或软重置后重新提交（请自行确认没有需要保留的提交）：

```bash
# 示例：回到远程 main 一致，保留工作区
git fetch origin
git reset --soft origin/main
git add -A
git commit -m "chore: 移除明文密钥，改用环境变量与 local 配置"
git push origin main
```

### 方案 B：历史较深，使用 git-filter-repo（推荐）

1. 安装：[git-filter-repo](https://github.com/newren/git-filter-repo)（需单独安装）。
2. 用**替换表**把历史里的旧密钥字符串整块替换成 `REDACTED`（替换为你真实泄露过的字符串）：

创建文本文件 `replacements.txt`：

```text
原AccessKeyId字符串==>REDACTED
原AccessKeySecret字符串==>REDACTED
```

执行：

```bash
git filter-repo --replace-text replacements.txt --force
git push origin --force --all
```

**注意**：`--force` 会改写远程历史，协作者需重新克隆或变基。

### 方案 C：新建空仓库迁移

将当前代码（无密钥）作为初始提交推到新仓库，旧仓库归档；最简单但丢失星标/Issue 等。

## 网络 `Recv failure: Connection was reset`

- 检查本机 VPN/系统代理是否与 Git 一致（`git config --global http.proxy`）。
- 尝试：`git config --global http.version HTTP/1.1`
- 或改用 SSH 远程：`git remote set-url origin git@github.com:OWNER/REPO.git`

---

完成密钥轮换 + 历史清理 + 推送成功后，再在本地配置新 AccessKey 进行短信功能测试。
