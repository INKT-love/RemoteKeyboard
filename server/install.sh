#!/bin/bash
# Remote Keyboard Server - Linux 安装脚本
set -e

INSTALL_DIR="/opt/remote-keyboard"
SERVICE_NAME="remote-keyboard"

echo "========================================="
echo "  Remote Keyboard Server 安装"
echo "========================================="

# 检查 Python
if ! command -v python3 &> /dev/null; then
    echo "错误: 未找到 python3，请先安装 Python 3.8+"
    exit 1
fi

# 安装依赖
echo "安装 Python 依赖..."
pip3 install -r requirements.txt

# 创建安装目录
echo "创建安装目录..."
sudo mkdir -p "$INSTALL_DIR"
sudo cp keyboard_server.py "$INSTALL_DIR/"
sudo cp requirements.txt "$INSTALL_DIR/"

# 创建 systemd 服务
echo "创建 systemd 服务..."
sudo tee /etc/systemd/system/${SERVICE_NAME}.service > /dev/null << EOF
[Unit]
Description=Remote Keyboard Server
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=$INSTALL_DIR
ExecStart=$(which python3) $INSTALL_DIR/keyboard_server.py
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

# 启用并启动服务
sudo systemctl daemon-reload
sudo systemctl enable "$SERVICE_NAME"
sudo systemctl start "$SERVICE_NAME"

LOCAL_IP=$(hostname -I | awk '{print $1}')

echo ""
echo "========================================="
echo "  安装完成!"
echo "========================================="
echo ""
echo "  服务状态: sudo systemctl status $SERVICE_NAME"
echo "  查看日志: sudo journalctl -u $SERVICE_NAME -f"
echo ""
echo "  在手机上添加设备:"
echo "    IP: $LOCAL_IP"
echo "    端口: 8765"
echo ""
echo "  管理命令:"
echo "    启动: sudo systemctl start $SERVICE_NAME"
echo "    停止: sudo systemctl stop $SERVICE_NAME"
echo "    重启: sudo systemctl restart $SERVICE_NAME"
echo "========================================="
