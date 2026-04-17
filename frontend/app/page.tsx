/**
 * 根路由 — 展示说明页面
 * 访问 /{ownerUsername}/chat 进入指定 owner 的对话页面
 */

export default function RootPage() {
  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
      <div className="text-center space-y-2">
        <h1 className="text-xl font-semibold text-gray-700">Dossier</h1>
        <p className="text-sm text-gray-400">
          Visit <code className="bg-gray-100 px-1 rounded">/{'{ownerUsername}'}/chat</code> to start a conversation
        </p>
      </div>
    </div>
  )
}
