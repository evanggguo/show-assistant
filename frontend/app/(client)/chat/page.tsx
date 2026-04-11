/**
 * /chat 页面 — 客户端主入口
 * 对应 TDD 4.3 前端主页面
 *
 * 此页面是纯客户端渲染（通过 ChatPage 组件中的 'use client'），
 * 因此无需在此文件重复声明 'use client'。
 */

import ChatPage from '@/components/chat/ChatPage'

export default function ChatRoute() {
  return <ChatPage />
}
