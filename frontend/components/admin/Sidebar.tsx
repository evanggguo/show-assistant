'use client'

import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { User, BookOpen, FileText, LogOut } from 'lucide-react'
import { useAdminAuth } from '@/hooks/useAdminAuth'

const navItems = [
  { href: '/admin/profile', label: 'Owner 信息', icon: User },
  { href: '/admin/knowledge', label: '知识库', icon: BookOpen },
  { href: '/admin/documents', label: '文档管理', icon: FileText },
]

export default function Sidebar() {
  const pathname = usePathname()
  const router = useRouter()
  const { logout } = useAdminAuth()

  const handleLogout = () => {
    logout()
    router.replace('/admin/login')
  }

  return (
    <aside className="w-56 bg-white border-r border-gray-100 flex flex-col shrink-0">
      <div className="px-5 py-4 border-b border-gray-100">
        <span className="text-sm font-semibold text-gray-700">管理后台</span>
      </div>
      <nav className="flex-1 py-4 px-3 space-y-1">
        {navItems.map(({ href, label, icon: Icon }) => (
          <Link
            key={href}
            href={href}
            className={[
              'flex items-center gap-3 px-3 py-2 rounded-xl text-sm transition-colors',
              pathname === href
                ? 'bg-blue-50 text-blue-600 font-medium'
                : 'text-gray-600 hover:bg-gray-50',
            ].join(' ')}
          >
            <Icon className="w-4 h-4 shrink-0" />
            {label}
          </Link>
        ))}
      </nav>
      <div className="px-3 py-4 border-t border-gray-100">
        <button
          onClick={handleLogout}
          className="flex items-center gap-3 px-3 py-2 rounded-xl text-sm
                     text-gray-500 hover:bg-gray-50 w-full transition-colors"
        >
          <LogOut className="w-4 h-4 shrink-0" />
          退出登录
        </button>
      </div>
    </aside>
  )
}
