/**
 * /{ownerUsername}/chat — 客户端对话页面
 * 通过 URL 中的 ownerUsername 识别展示者，实现多 owner 数据隔离
 */

import ChatPage from '@/components/chat/ChatPage'

interface Props {
  params: Promise<{ ownerUsername: string }>
}

export default async function ChatRoute({ params }: Props) {
  const { ownerUsername } = await params
  return <ChatPage ownerUsername={ownerUsername} />
}
