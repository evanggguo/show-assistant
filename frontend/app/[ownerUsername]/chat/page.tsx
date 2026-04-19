/**
 * /{ownerUsername}/chat — Client-facing chat page
 * Identifies the owner via the URL ownerUsername param; provides multi-owner data isolation
 */

import ChatPage from '@/components/chat/ChatPage'

interface Props {
  params: Promise<{ ownerUsername: string }>
}

export default async function ChatRoute({ params }: Props) {
  const { ownerUsername } = await params
  return <ChatPage ownerUsername={ownerUsername} />
}
