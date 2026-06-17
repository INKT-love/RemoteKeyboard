#!/usr/bin/env python3
"""
Remote Keyboard Server
跨平台 WebSocket 服务端，接收手机键盘指令并模拟键鼠操作
支持 Windows / Linux / macOS
"""

import asyncio
import json
import logging
import platform
import socket
import sys
import threading
from typing import Optional

import websockets
from pynput.keyboard import Controller as KeyboardController, Key, KeyCode
from pynput.mouse import Controller as MouseController, Button

# 配置
DEFAULT_HOST = "0.0.0.0"
DEFAULT_PORT = 8765
DISCOVERY_PORT = 8767
DISCOVERY_RESPONSE_PORT = 8768
DISCOVERY_MAGIC = "REMOTE_KEYBOARD_DISCOVER"

# 日志
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S"
)
logger = logging.getLogger("RemoteKeyboard")

# 键鼠控制器
keyboard = KeyboardController()
mouse = MouseController()

# 当前平台
PLATFORM = platform.system().lower()

# 按键映射
KEY_MAP = {
    # 特殊键
    "enter": Key.enter,
    "return": Key.enter,
    "tab": Key.tab,
    "space": Key.space,
    "backspace": Key.backspace,
    "delete": Key.delete,
    "escape": Key.esc,
    "esc": Key.esc,
    "caps_lock": Key.caps_lock,
    "capslock": Key.caps_lock,

    # 修饰键
    "shift": Key.shift,
    "shift_l": Key.shift,
    "shift_r": Key.shift_r,
    "ctrl": Key.ctrl,
    "ctrl_l": Key.ctrl,
    "ctrl_r": Key.ctrl_r,
    "alt": Key.alt,
    "alt_l": Key.alt,
    "alt_r": Key.alt_r,
    "super": Key.cmd,
    "win": Key.cmd,
    "cmd": Key.cmd,
    "meta": Key.cmd,

    # 方向键
    "up": Key.up,
    "down": Key.down,
    "left": Key.left,
    "right": Key.right,

    # 导航键
    "home": Key.home,
    "end": Key.end,
    "pageup": Key.page_up,
    "page_up": Key.page_up,
    "pagedown": Key.page_down,
    "page_down": Key.page_down,
    "insert": Key.insert,

    # 功能键
    "f1": Key.f1, "f2": Key.f2, "f3": Key.f3, "f4": Key.f4,
    "f5": Key.f5, "f6": Key.f6, "f7": Key.f7, "f8": Key.f8,
    "f9": Key.f9, "f10": Key.f10, "f11": Key.f11, "f12": Key.f12,

    # 其他
    "printscreen": Key.print_screen,
    "scroll_lock": Key.scroll_lock,
    "pause": Key.pause,
    "menu": Key.menu,
}

# 鼠标按键映射
MOUSE_BUTTON_MAP = {
    "left": Button.left,
    "right": Button.right,
    "middle": Button.middle,
}

# 修饰键集合（用于持续按住）
MODIFIER_KEYS = {"shift", "ctrl", "alt", "super", "win", "cmd", "meta",
                 "shift_l", "shift_r", "ctrl_l", "ctrl_r", "alt_l", "alt_r"}


def get_local_ip() -> str:
    """获取本机局域网 IP"""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"


def resolve_key(key_name: str):
    """解析按键名称为 pynput Key 对象"""
    key_lower = key_name.lower()

    # 先查特殊键映射
    if key_lower in KEY_MAP:
        return KEY_MAP[key_lower]

    # 单个字符直接用
    if len(key_name) == 1:
        return KeyCode.from_char(key_name)

    # 尝试作为 KeyCode
    try:
        return KeyCode.from_char(key_lower)
    except Exception:
        return None


async def handle_key_event(data: dict):
    """处理按键事件"""
    action = data.get("action", "tap")
    key_name = data.get("key", "")
    modifiers = data.get("modifiers", [])
    text = data.get("text", "")

    if action == "type" and text:
        # 直接输入文本
        keyboard.type(text)
        return

    key = resolve_key(key_name)
    if key is None:
        logger.warning(f"Unknown key: {key_name}")
        return

    # 按下修饰键
    mod_keys = []
    for mod in modifiers:
        mod_key = resolve_key(mod)
        if mod_key and mod.lower() in MODIFIER_KEYS:
            keyboard.press(mod_key)
            mod_keys.append(mod_key)

    try:
        if action == "press":
            keyboard.press(key)
        elif action == "release":
            keyboard.release(key)
        elif action == "tap":
            keyboard.press(key)
            keyboard.release(key)
    finally:
        # 释放修饰键
        for mod_key in mod_keys:
            keyboard.release(mod_key)


async def handle_mouse_event(data: dict):
    """处理鼠标事件"""
    action = data.get("action", "move")

    if action == "move":
        x = data.get("x", 0)
        y = data.get("y", 0)
        mouse.move(x, y)

    elif action in ("click", "tap"):
        button_name = data.get("button", "left")
        button = MOUSE_BUTTON_MAP.get(button_name, Button.left)
        mouse.click(button)

    elif action == "press":
        button_name = data.get("button", "left")
        button = MOUSE_BUTTON_MAP.get(button_name, Button.left)
        mouse.press(button)

    elif action == "release":
        button_name = data.get("button", "left")
        button = MOUSE_BUTTON_MAP.get(button_name, Button.left)
        mouse.release(button)

    elif action == "scroll":
        delta = data.get("delta", 0)
        mouse.scroll(0, delta)


async def handle_client(websocket):
    """处理单个客户端连接"""
    client_addr = websocket.remote_address
    logger.info(f"客户端连接: {client_addr}")

    # 发送状态
    status = json.dumps({
        "type": "status",
        "connected": True,
        "platform": PLATFORM
    })
    await websocket.send(status)

    try:
        async for message in websocket:
            try:
                data = json.loads(message)
                msg_type = data.get("type", "")

                if msg_type == "key":
                    await handle_key_event(data)
                elif msg_type == "mouse":
                    await handle_mouse_event(data)
                elif msg_type == "ping":
                    await websocket.send(json.dumps({"type": "pong"}))
                else:
                    logger.debug(f"Unknown message type: {msg_type}")

            except json.JSONDecodeError:
                logger.warning(f"Invalid JSON: {message[:100]}")
            except Exception as e:
                logger.error(f"Error handling message: {e}")

    except websockets.exceptions.ConnectionClosed:
        pass
    finally:
        logger.info(f"客户端断开: {client_addr}")


async def discovery_responder():
    """UDP 广播发现响应"""
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.setblocking(False)

    try:
        sock.bind(("", DISCOVERY_PORT))
    except OSError:
        logger.warning(f"无法绑定发现端口 {DISCOVERY_PORT}，设备发现功能不可用")
        return

    hostname = socket.gethostname()
    loop = asyncio.get_event_loop()

    logger.info(f"设备发现服务启动，端口 {DISCOVERY_PORT}")

    while True:
        try:
            data, addr = await loop.run_in_executor(None, lambda: sock.recvfrom(1024))
            message = data.decode("utf-8", errors="ignore").strip()

            if message == DISCOVERY_MAGIC:
                # 回复到发送者的地址和端口
                response = f"REMOTE_KEYBOARD_SERVER:{hostname}:{hostname}:{DEFAULT_PORT}:{PLATFORM}"
                sock.sendto(response.encode(), (addr[0], addr[1]))
                logger.info(f"发现请求来自 {addr}，已回复")

        except BlockingIOError:
            await asyncio.sleep(0.1)
        except Exception as e:
            logger.debug(f"发现服务错误: {e}")
            await asyncio.sleep(0.1)


async def main(host: str = DEFAULT_HOST, port: int = DEFAULT_PORT):
    """启动服务"""
    local_ip = get_local_ip()

    logger.info("=" * 50)
    logger.info("  Remote Keyboard Server")
    logger.info("=" * 50)
    logger.info(f"  平台: {PLATFORM}")
    logger.info(f"  监听: {host}:{port}")
    logger.info(f"  局域网 IP: {local_ip}")
    logger.info(f"  设备发现端口: {DISCOVERY_PORT}")
    logger.info("=" * 50)
    logger.info("")
    logger.info("  在手机上添加设备:")
    logger.info(f"    IP: {local_ip}")
    logger.info(f"    端口: {port}")
    logger.info("")
    logger.info("  按 Ctrl+C 停止服务")
    logger.info("")

    # 启动 WebSocket 服务
    async with websockets.serve(handle_client, host, port):
        logger.info(f"WebSocket 服务已启动 ws://{host}:{port}")
        # 同时运行发现服务
        await discovery_responder()


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Remote Keyboard Server")
    parser.add_argument("--host", default=DEFAULT_HOST, help=f"监听地址 (默认: {DEFAULT_HOST})")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT, help=f"监听端口 (默认: {DEFAULT_PORT})")
    args = parser.parse_args()

    try:
        asyncio.run(main(args.host, args.port))
    except KeyboardInterrupt:
        logger.info("服务已停止")
