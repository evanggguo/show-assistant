'use client'

import { AlertCircle, RefreshCw, X } from 'lucide-react'

interface ErrorAlertProps {
  message: string
  onDismiss?: () => void
  onRetry?: () => void
  className?: string
}

/**
 * 统一错误提示组件
 * 显示友好的错误信息，可选支持关闭和重试操作
 */
export default function ErrorAlert({ message, onDismiss, onRetry, className = '' }: ErrorAlertProps) {
  return (
    <div className={`flex items-start gap-3 bg-red-50 border border-red-200 rounded-xl px-4 py-3 text-sm text-red-700 ${className}`}>
      <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
      <span className="flex-1">{message}</span>
      <div className="flex items-center gap-2 flex-shrink-0">
        {onRetry && (
          <button
            onClick={onRetry}
            className="flex items-center gap-1 text-xs text-red-600 hover:text-red-800 font-medium transition-colors"
            aria-label="重试"
          >
            <RefreshCw className="w-3 h-3" />
            重试
          </button>
        )}
        {onDismiss && (
          <button
            onClick={onDismiss}
            className="hover:text-red-900 transition-colors"
            aria-label="关闭"
          >
            <X className="w-4 h-4" />
          </button>
        )}
      </div>
    </div>
  )
}
