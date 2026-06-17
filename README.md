# Remote Keyboard

将手机变成远程键盘，通过局域网或公网控制 Windows/Linux 电脑。

## 功能特性

- 🎹 **全键盘布局** — QWERTY 标准布局，支持竖屏/横屏自适应
- 🖱️ **触控板模式** — 手机屏幕变鼠标触控板
- ⌨️ **快捷键面板** — 一键复制/粘贴/撤销/切换窗口
- 🎨 **莫奈取色** — 跟随系统壁纸动态取色 (Material You)
- 📱 **MiuiX 风格** — 小米 MIUI 设计语言，支持亮色/暗色切换
- 📡 **局域网直连** — 低延迟，无需外网
- 🌐 **公网中继** — 通过中继服务器穿透内网
- 🔍 **设备发现** — UDP 广播自动发现局域网设备

## 项目结构

```
RemoteKeyboard/
├── android/          # Android 客户端 (Kotlin + Jetpack Compose)
├── server/           # 电脑端服务 (Python)
├── relay/            # 中继服务器 (Python)
└── README.md
```

## 快速开始

### 1. 启动电脑端服务

**Windows:**
```bash
cd server
pip install -r requirements.txt
python keyboard_server.py
```

**Linux:**
```bash
cd server
pip3 install -r requirements.txt
python3 keyboard_server.py

# 或安装为系统服务
chmod +x install.sh
sudo ./install.sh
```

服务启动后会显示局域网 IP 和端口。

### 2. 手机端连接

1. 打开 Remote Keyboard APP
2. 点击 "扫描设备" 自动发现局域网设备，或手动输入 IP:端口
3. 点击 "连接"
4. 开始使用！

### 3. 公网中继（可选）

```bash
cd relay
pip install -r requirements.txt
python relay_server.py --port 8766

# 或使用 Docker
docker build -t remote-keyboard-relay .
docker run -p 8766:8766 remote-keyboard-relay
```

## 通信协议

WebSocket JSON 消息格式:

```json
// 按键事件
{"type": "key", "action": "tap", "key": "a", "modifiers": ["ctrl"]}

// 鼠标事件
{"type": "mouse", "action": "move", "x": 100, "y": 200}

// 心跳
{"type": "ping"} → {"type": "pong"}
```

## 技术栈

| 组件 | 技术 |
|------|------|
| Android 客户端 | Kotlin, Jetpack Compose, Material 3, OkHttp |
| 电脑服务端 | Python, pynput, websockets |
| 中继服务器 | Python, websockets |

## 开发

### Android 端

使用 Android Studio 打开 `android/` 目录，需要:
- Android Studio Hedgehog+
- JDK 17
- Android SDK 35

### 服务端

Python 3.8+ 即可运行。

## 许可证

MIT License
