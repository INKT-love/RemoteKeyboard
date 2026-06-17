#!/usr/bin/env python3
"""
Remote Keyboard Relay Server
WebSocket 中继服务器，用于公网穿透
支持多个客户端和多个服务端的配对
"""

import asyncio
import json
import logging
import time
from typing import Dict, Optional, Set

import websockets

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S"
)
logger = logging.getLogger("Relay")

DEFAULT_PORT = 8766


class DeviceSession:
    """设备会话"""
    def __init__(self, device_id: str, websocket, device_type: str):
        self.device_id = device_id
        self.websocket = websocket
        self.device_type = device_type  # "server" or "client"
        self.connected_at = time.time()
        self.partner: Optional['DeviceSession'] = None


class RelayServer:
    def __init__(self):
        # 已注册的服务端设备
        self.servers: Dict[str, DeviceSession] = {}
        # 已连接的客户端
        self.clients: Dict[str, DeviceSession] = {}
        # 所有连接
        self.connections: Dict[websockets.WebSocketServerProtocol, DeviceSession] = {}

    async def handle_connection(self, websocket):
        """处理新连接"""
        path = websocket.request.path if hasattr(websocket, 'request') else "/"
        logger.info(f"新连接: {websocket.remote_address} path={path}")

        session = None
        try:
            async for message in websocket:
                try:
                    data = json.loads(message)
                    msg_type = data.get("type", "")

                    if msg_type == "register":
                        # 注册为服务端
                        device_id = data.get("device_id", str(websocket.remote_address))
                        device_name = data.get("device_name", "Unknown")
                        session = DeviceSession(device_id, websocket, "server")
                        self.servers[device_id] = session
                        self.connections[websocket] = session

                        await websocket.send(json.dumps({
                            "type": "registered",
                            "device_id": device_id,
                            "status": "ok"
                        }))
                        logger.info(f"服务端注册: {device_id} ({device_name})")

                    elif msg_type == "connect":
                        # 客户端请求连接到服务端
                        target_id = data.get("device_id", "")
                        client_id = data.get("client_id", str(websocket.remote_address))

                        if session is None:
                            session = DeviceSession(client_id, websocket, "client")
                            self.clients[client_id] = session
                            self.connections[websocket] = session

                        target = self.servers.get(target_id)
                        if target and target.websocket.open:
                            # 建立配对
                            session.partner = target
                            target.partner = session

                            await websocket.send(json.dumps({
                                "type": "connected",
                                "target": target_id,
                                "status": "ok"
                            }))
                            await target.websocket.send(json.dumps({
                                "type": "client_connected",
                                "client": client_id
                            }))
                            logger.info(f"配对成功: {client_id} -> {target_id}")
                        else:
                            await websocket.send(json.dumps({
                                "type": "error",
                                "message": f"设备 {target_id} 不在线"
                            }))

                    elif msg_type == "list":
                        # 列出在线服务端
                        online = [
                            {
                                "device_id": sid,
                                "connected_at": s.connected_at
                            }
                            for sid, s in self.servers.items()
                            if s.websocket.open
                        ]
                        await websocket.send(json.dumps({
                            "type": "server_list",
                            "servers": online
                        }))

                    elif msg_type in ("key", "mouse", "ping"):
                        # 转发消息给配对的设备
                        if session and session.partner:
                            try:
                                await session.partner.websocket.send(message)
                            except websockets.exceptions.ConnectionClosed:
                                await websocket.send(json.dumps({
                                    "type": "error",
                                    "message": "对方已断开"
                                }))
                        elif msg_type == "ping":
                            await websocket.send(json.dumps({"type": "pong"}))

                    else:
                        # 其他消息直接转发
                        if session and session.partner:
                            await session.partner.websocket.send(message)

                except json.JSONDecodeError:
                    logger.warning(f"Invalid JSON from {websocket.remote_address}")
                except Exception as e:
                    logger.error(f"Error: {e}")

        except websockets.exceptions.ConnectionClosed:
            pass
        finally:
            self._cleanup(websocket)

    def _cleanup(self, websocket):
        """清理断开的连接"""
        session = self.connections.pop(websocket, None)
        if session:
            # 清理配对
            if session.partner:
                session.partner.partner = None
                asyncio.create_task(self._notify_disconnect(session.partner.websocket))

            # 从注册表移除
            if session.device_type == "server":
                self.servers.pop(session.device_id, None)
                logger.info(f"服务端断开: {session.device_id}")
            else:
                self.clients.pop(session.device_id, None)
                logger.info(f"客户端断开: {session.device_id}")

    async def _notify_disconnect(self, websocket):
        """通知对方断开"""
        try:
            await websocket.send(json.dumps({
                "type": "partner_disconnected"
            }))
        except Exception:
            pass


async def main(port: int = DEFAULT_PORT):
    relay = RelayServer()

    logger.info("=" * 50)
    logger.info("  Remote Keyboard Relay Server")
    logger.info("=" * 50)
    logger.info(f"  监听端口: {port}")
    logger.info("=" * 50)

    async with websockets.serve(relay.handle_connection, "0.0.0.0", port):
        logger.info(f"中继服务已启动 ws://0.0.0.0:{port}")
        await asyncio.Future()  # run forever


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Remote Keyboard Relay Server")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT, help=f"监听端口 (默认: {DEFAULT_PORT})")
    args = parser.parse_args()

    try:
        asyncio.run(main(args.port))
    except KeyboardInterrupt:
        logger.info("中继服务已停止")
