'use client'

/**
 * TypingIndicator — shown while waiting for the first streaming token.
 * Three bouncing dots with staggered delay, matching the assistant bubble layout.
 */
export default function TypingIndicator() {
  return (
    <div className="mb-6">
      <div className="flex items-center gap-1.5 h-6 px-0.5">
        {([0, 160, 320] as const).map((delay, i) => (
          <span
            key={i}
            className="w-2 h-2 rounded-full bg-gray-400 animate-bounce"
            style={{ animationDelay: `${delay}ms` }}
            aria-hidden="true"
          />
        ))}
      </div>
      <span className="sr-only">AI is thinking</span>
    </div>
  )
}
