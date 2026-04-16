/**
 * 错误信息友好化工具
 * 将技术性错误消息映射为用户友好的提示文字
 */

export function toFriendlyMessage(err: unknown): string {
  const raw = err instanceof Error ? err.message : String(err ?? '')

  // 网络/连接错误
  if (
    raw.includes('Failed to fetch') ||
    raw.includes('NetworkError') ||
    raw.includes('net::ERR') ||
    raw.includes('ECONNREFUSED')
  ) {
    return '网络连接失败，请检查网络后重试'
  }

  // HTTP 状态码错误
  if (raw.includes('502') || raw.includes('Bad Gateway')) {
    return '服务暂时不可用，请稍后重试'
  }
  if (raw.includes('503') || raw.includes('Service Unavailable')) {
    return '服务维护中，请稍后再试'
  }
  if (raw.includes('500') || raw.includes('Internal Server Error')) {
    return '服务器出现异常，请稍后重试'
  }
  if (raw.includes('504') || raw.includes('Gateway Timeout')) {
    return '请求超时，请稍后重试'
  }
  if (raw.includes('401') || raw.includes('未授权')) {
    return '登录已过期，请重新登录'
  }
  if (raw.includes('403') || raw.includes('Forbidden')) {
    return '权限不足，请联系管理员'
  }
  if (raw.includes('404') || raw.includes('Not Found')) {
    return '请求的资源不存在'
  }

  // SSE / 流式错误
  if (raw.includes('SSE') || raw.includes('响应体为空')) {
    return '连接中断，请刷新页面后重试'
  }

  // 业务错误（已有友好提示，直接使用）
  if (raw && raw.length > 0 && raw.length < 80) {
    return raw
  }

  return '操作失败，请稍后重试'
}
