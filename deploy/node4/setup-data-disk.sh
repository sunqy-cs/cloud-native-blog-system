#!/usr/bin/env bash
# 在 Node4（数据节点）上执行：挂载数据盘并让 K3s 使用该盘存储 PVC
# 使用前请确认数据盘设备名（如 /dev/vdb），必要时修改下面 DATA_DISK

set -e
DATA_DISK="${DATA_DISK:-/dev/vdb}"   # 阿里云 ECS 常见为 /dev/vdb，可改为 vdc 等
MOUNT_DATA="/mnt/data"               # 数据盘挂载点
K3S_STORAGE="/var/lib/rancher/k3s/storage"  # K3s local-path 在节点上的根目录

echo "数据盘设备: $DATA_DISK"
echo "挂载点: $MOUNT_DATA"

if [ ! -b "$DATA_DISK" ]; then
  echo "错误: 未找到块设备 $DATA_DISK，请先在控制台挂载云盘并修改本脚本 DATA_DISK"
  echo "当前块设备: $(lsblk -d -o NAME,SIZE,MODEL 2>/dev/null || true)"
  exit 1
fi

# 若整盘使用（无分区），可直接格式化为 ext4；若有分区则用 DATA_PART（如 /dev/vdb1）
DATA_PART="${DATA_PART:-}"
if [ -z "$DATA_PART" ]; then
  if [ -e "${DATA_DISK}1" ]; then
    DATA_PART="${DATA_DISK}1"
    echo "使用已有分区: $DATA_PART"
  else
    DATA_PART="$DATA_DISK"
    echo "将使用整盘 $DATA_DISK（无分区）"
  fi
fi

# 1. 若未格式化则格式化（仅整盘或未格式化的分区）
if ! sudo blkid -o value -s TYPE "$DATA_PART" 2>/dev/null | grep -q ext4; then
  echo "正在格式化 $DATA_PART 为 ext4..."
  sudo mkfs -t ext4 -F "$DATA_PART"
fi

# 2. 挂载到 /mnt/data
sudo mkdir -p "$MOUNT_DATA"
if ! mountpoint -q "$MOUNT_DATA"; then
  sudo mount "$DATA_PART" "$MOUNT_DATA"
  echo "已挂载 $DATA_PART 到 $MOUNT_DATA"
else
  echo "$MOUNT_DATA 已挂载，跳过"
fi

# 3. 写入 /etc/fstab（用 UUID，避免盘符变化）
UUID=$(sudo blkid -o value -s UUID "$DATA_PART" 2>/dev/null || true)
if [ -n "$UUID" ]; then
  if ! grep -q "UUID=$UUID" /etc/fstab 2>/dev/null; then
    echo "UUID=$UUID $MOUNT_DATA ext4 defaults,nofail 0 2" | sudo tee -a /etc/fstab
    echo "已写入 /etc/fstab"
  fi
else
  echo "警告: 未获取到 UUID，请手动将 $DATA_PART $MOUNT_DATA ext4 defaults,nofail 0 2 加入 /etc/fstab"
fi

# 4. 让 K3s 在 Node4 上使用该盘：把 local-path 的存储目录指向数据盘
#    做法：在 Node4 上把数据盘挂到 K3s 的 storage 目录（需先停 k3s-agent）
if [ -d "$K3S_STORAGE" ]; then
  if mountpoint -q "$K3S_STORAGE"; then
    echo "K3s 存储目录 $K3S_STORAGE 已是挂载点，无需再绑。"
  else
    echo "---"
    echo "可选：让 K3s 的 PVC 存储落在数据盘上（仅 Node4 需做一次）："
    echo "  1. 停止 K3s agent: sudo systemctl stop k3s-agent"
    echo "  2. 备份并清空原目录: sudo mv $K3S_STORAGE ${K3S_STORAGE}.bak 2>/dev/null; sudo mkdir -p $K3S_STORAGE"
    echo "  3. 绑定挂载: sudo mount --bind $MOUNT_DATA $K3S_STORAGE"
    echo "  4. 开机挂载: echo \"$MOUNT_DATA $K3S_STORAGE none bind 0 0\" | sudo tee -a /etc/fstab"
    echo "  5. 启动 K3s: sudo systemctl start k3s-agent"
    echo "若已确认无需绑定，可忽略上述步骤。"
  fi
else
  echo "未检测到 $K3S_STORAGE，请确保本机为 K3s 节点。绑定步骤见上方「可选」说明。"
fi

echo "---"
echo "完成。验证: df -h $MOUNT_DATA && ls $MOUNT_DATA"
