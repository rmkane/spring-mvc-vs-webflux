/**
 * API utility functions for making requests to the backend.
 * Automatically includes the ssl-client-subject-dn and ssl-client-issuer-dn headers from environment variables.
 */

// Alternatively, port 8081 can be used for WebFlux
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

// Environment variables
const SSL_CLIENT_SUBJECT_DN_ENV = 'SSL_CLIENT_SUBJECT_DN'
const SSL_CLIENT_ISSUER_DN_ENV = 'SSL_CLIENT_ISSUER_DN'

// Headers
const SSL_CLIENT_SUBJECT_DN_HEADER = 'ssl-client-subject-dn'
const SSL_CLIENT_ISSUER_DN_HEADER = 'ssl-client-issuer-dn'

/**
 * Gets a required environment variable value.
 * Throws an error if the variable is not set.
 *
 * @param envVar - The environment variable name
 * @returns The environment variable value
 */
function getRequiredEnv(envVar: string): string {
  const value = process.env[envVar]
  if (!value) {
    throw new Error(`${envVar} environment variable is not set. Please set it in .env.local`)
  }
  return value
}

/**
 * Gets the Subject DN (Distinguished Name) from environment variables.
 * For local development, uses SSL_CLIENT_SUBJECT_DN from .env.local
 */
function getSubjectDn(): string {
  return getRequiredEnv(SSL_CLIENT_SUBJECT_DN_ENV)
}

/**
 * Gets the Issuer DN (Distinguished Name) from environment variables.
 * For local development, uses SSL_CLIENT_ISSUER_DN from .env.local
 * This is required.
 */
function getIssuerDn(): string {
  return getRequiredEnv(SSL_CLIENT_ISSUER_DN_ENV)
}

/**
 * Creates fetch options with the ssl-client-subject-dn and ssl-client-issuer-dn headers automatically included.
 * This should be used for server-side requests (API routes, Server Components).
 *
 * @param options - Additional fetch options to merge
 * @returns Fetch options with SSL client DN headers included
 */
export function createApiRequestOptions(options: RequestInit = {}): RequestInit {
  const subjectDn = getSubjectDn()
  const issuerDn = getIssuerDn()

  return {
    ...options,
    headers: {
      ...((options.headers as Record<string, string>) || {}),
      [SSL_CLIENT_SUBJECT_DN_HEADER]: subjectDn,
      [SSL_CLIENT_ISSUER_DN_HEADER]: issuerDn,
    },
  }
}

/**
 * Makes a fetch request to the backend API with the ssl-client-subject-dn and ssl-client-issuer-dn headers automatically included.
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
