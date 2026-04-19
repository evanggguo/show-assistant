/**
 * Error message formatter utility.
 * Maps technical error messages to user-friendly display text.
 */

export function toFriendlyMessage(err: unknown): string {
  const raw = err instanceof Error ? err.message : String(err ?? '')

  // Network / connection errors
  if (
    raw.includes('Failed to fetch') ||
    raw.includes('NetworkError') ||
    raw.includes('net::ERR') ||
    raw.includes('ECONNREFUSED')
  ) {
    return 'Network connection failed. Please check your connection and try again.'
  }

  // HTTP status code errors
  if (raw.includes('502') || raw.includes('Bad Gateway')) {
    return 'Service temporarily unavailable. Please try again later.'
  }
  if (raw.includes('503') || raw.includes('Service Unavailable')) {
    return 'Service is under maintenance. Please try again later.'
  }
  if (raw.includes('500') || raw.includes('Internal Server Error')) {
    return 'An unexpected server error occurred. Please try again later.'
  }
  if (raw.includes('504') || raw.includes('Gateway Timeout')) {
    return 'Request timed out. Please try again later.'
  }
  if (raw.includes('401') || raw.includes('Unauthorized')) {
    return 'Your session has expired. Please log in again.'
  }
  if (raw.includes('403') || raw.includes('Forbidden')) {
    return 'Insufficient permissions. Please contact your administrator.'
  }
  if (raw.includes('404') || raw.includes('Not Found')) {
    return 'The requested resource was not found.'
  }

  // SSE / streaming errors
  if (raw.includes('SSE') || raw.includes('response body is empty')) {
    return 'Connection interrupted. Please refresh the page and try again.'
  }

  // Business errors (already user-friendly, use as-is)
  if (raw && raw.length > 0 && raw.length < 80) {
    return raw
  }

  return 'Operation failed. Please try again later.'
}
