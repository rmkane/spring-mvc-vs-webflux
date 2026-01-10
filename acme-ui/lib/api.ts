/**
 * API utility functions for making requests to the backend.
 * Automatically includes the x-dn header from environment variables.
 */

// Alternatively, port 8081 can be used for WebFlux
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'
const DN_HEADER = 'x-dn'

/**
 * Gets the DN (Distinguished Name) from environment variables.
 * For local development, uses LOCAL_DN from .env.local
 */
function getDn(): string {
  const dn = process.env.LOCAL_DN
  if (!dn) {
    throw new Error('LOCAL_DN environment variable is not set. Please set it in .env.local')
  }
  return dn
}

/**
 * Creates fetch options with the x-dn header automatically included.
 * This should be used for server-side requests (API routes, Server Components).
 *
 * @param options - Additional fetch options to merge
 * @returns Fetch options with x-dn header included
 */
export function createApiRequestOptions(options: RequestInit = {}): RequestInit {
  const dn = getDn()
  return {
    ...options,
    headers: {
      ...options.headers,
      [DN_HEADER]: dn,
    },
  }
}

/**
 * Makes a fetch request to the backend API with the x-dn header automatically included.
 * This should be used for server-side requests (API routes, Server Components).
 *
 * @param path - API path (e.g., '/api/books')
 * @param options - Additional fetch options
 * @returns Promise resolving to the response
 */
export async function apiRequest(path: string, options: RequestInit = {}): Promise<Response> {
  const url = path.startsWith('http') ? path : `${API_BASE_URL}${path}`
  return fetch(url, createApiRequestOptions(options))
}

/**
 * Makes a fetch request and parses the JSON response.
 *
 * @param path - API path (e.g., '/api/books')
 * @param options - Additional fetch options
 * @returns Promise resolving to the parsed JSON data
 */
export async function apiRequestJson<T = unknown>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const response = await apiRequest(path, options)
  if (!response.ok) {
    throw new Error(`API request failed: ${response.status} ${response.statusText}`)
  }
  return response.json()
}
