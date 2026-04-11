/**
 * 根路由 — 重定向到 /chat 页面
 * 使用 Next.js 的 redirect 函数做服务端重定向
 */

import { redirect } from 'next/navigation'

export default function RootPage() {
  redirect('/chat')
}
