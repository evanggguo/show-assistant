/**
 * Root route — displays an informational landing page
 * Visit /{ownerUsername}/chat to open the chat page for a specific owner
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
