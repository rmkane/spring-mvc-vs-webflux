/**
 * API utility functions for making requests to the backend.
 * Automatically includes the ssl-client-subject-dn and ssl-client-issuer-dn headers.
 *
 * In Kubernetes: Headers come from ingress (via middleware)
 * In local dev: Headers come from environment variables (.env.local)
 */

/**
 * Determines the API base URL based on environment configuration.
 *
 * Priority:
 * 1. NEXT_PUBLIC_API_URL (explicit URL override)
 * 2. NEXT_PUBLIC_API_TYPE (mvc or webflux) - uses internal service names
 * 3. Default to MVC API (http://localhost:8080 for local, api-mvc:8080 for K8s)
 */
function getApiBaseUrl(): string {
  // Explicit URL override takes precedence
  if (process.env.NEXT_PUBLIC_API_URL) {
    return process.env.NEXT_PUBLIC_API_URL
  }

  // Check if API type is specified
  const apiType = process.env.NEXT_PUBLIC_API_TYPE?.toLowerCase()

  // In Kubernetes, use internal service names
  // In local dev, use localhost with different ports
  const isK8s = process.env.NODE_ENV === 'production' || process.env.KUBERNETES_SERVICE_HOST

  switch (apiType) {
    case 'webflux':
      return isK8s ? 'http://api-webflux:8081' : 'http://localhost:8081'
    case 'mvc':
    default:
      return isK8s ? 'http://api-mvc:8080' : 'http://localhost:8080'
  }
}

const API_BASE_URL = getApiBaseUrl()

// Environment variables
const SSL_CLIENT_SUBJECT_DN_ENV = 'SSL_CLIENT_SUBJECT_DN'
const SSL_CLIENT_ISSUER_DN_ENV = 'SSL_CLIENT_ISSUER_DN'

// Headers
const SSL_CLIENT_SUBJECT_DN_HEADER = 'ssl-client-subject-dn'
const SSL_CLIENT_ISSUER_DN_HEADER = 'ssl-client-issuer-dn'

/**
 * Attempts to get headers from Next.js headers() function.
 *
 * IMPORTANT: In Next.js, headers() must be called at the top level of Server Components.
 * This function tries to get headers, but it may not work if called from deep within
 * the call stack. For reliability, Server Components should call headers() at the top
 * level and pass it explicitly, or rely on environment variables for local dev.
 *
 * @returns Headers if available, undefined otherwise
 */
async function tryGetHeadersFromContext(): Promise<Headers | undefined> {
  try {
    // Dynamic import to avoid issues if not in Server Component context
    const { headers } = await import('next/headers')
    // In Next.js 16.1, headers() is synchronous
    return headers()
  } catch {
    // Not in a Server Component context (e.g., API routes, client components)
    return undefined
  }
}

/**
 * Gets an environment variable value, or returns undefined if not set.
 *
 * @param envVar - The environment variable name
 * @returns The environment variable value or undefined
 */
function getEnv(envVar: string): string | undefined {
  return process.env[envVar]
}

/**
 * Gets the Subject DN (Distinguished Name) from headers or environment variables.
 * In Kubernetes, the ingress passes this as a header. For local development, uses environment variable.
 *
 * This function tries multiple sources in order:
 * 1. Explicit headers parameter (from API routes or Server Components)
 * 2. Environment variable (local development)
 *
 * For Server Components, pass headers from next/headers:
 * ```ts
 * import { headers } from 'next/headers'
 * const headersList = headers()
 * getAllBooks(headersList)
 * ```
 *
 * @param headers - Optional headers object to read from (from incoming request or next/headers)
 * @returns The Subject DN, or throws an error if not found
 */
function getSubjectDn(headers?: Headers | Record<string, string>): string {
  // Try to get from explicit headers parameter first (API routes or Server Components)
  if (headers) {
    const headerValue =
      headers instanceof Headers
        ? headers.get(SSL_CLIENT_SUBJECT_DN_HEADER)
        : headers[SSL_CLIENT_SUBJECT_DN_HEADER]
    if (headerValue) {
      return headerValue
    }
  }

  // Try to get from environment variable (local development)
  const envValue = getEnv(SSL_CLIENT_SUBJECT_DN_ENV)
  if (envValue) {
    return envValue
  }

  // If neither is available, throw an error
  throw new Error(
    `${SSL_CLIENT_SUBJECT_DN_ENV} is not set. ` +
      `In Kubernetes, pass headers from next/headers() to API functions. ` +
      `For local development, set it in .env.local`
  )
}

/**
 * Gets the Issuer DN (Distinguished Name) from headers or environment variables.
 * In Kubernetes, the ingress passes this as a header. For local development, uses environment variable.
 *
 * This function tries multiple sources in order:
 * 1. Explicit headers parameter (from API routes or Server Components)
 * 2. Environment variable (local development)
 *
 * For Server Components, pass headers from next/headers:
 * ```ts
 * import { headers } from 'next/headers'
 * const headersList = headers()
 * getAllBooks(headersList)
 * ```
 *
 * @param headers - Optional headers object to read from (from incoming request or next/headers)
 * @returns The Issuer DN, or throws an error if not found
 */
function getIssuerDn(headers?: Headers | Record<string, string>): string {
  // Try to get from explicit headers parameter first (API routes or Server Components)
  if (headers) {
    const headerValue =
      headers instanceof Headers
        ? headers.get(SSL_CLIENT_ISSUER_DN_HEADER)
        : headers[SSL_CLIENT_ISSUER_DN_HEADER]
    if (headerValue) {
      return headerValue
    }
  }

  // Try to get from environment variable (local development)
  const envValue = getEnv(SSL_CLIENT_ISSUER_DN_ENV)
  if (envValue) {
    return envValue
  }

  // If neither is available, throw an error
  throw new Error(
    `${SSL_CLIENT_ISSUER_DN_ENV} is not set. ` +
      `In Kubernetes, pass headers from next/headers() to API functions. ` +
      `For local development, set it in .env.local`
  )
}

/**
 * Creates fetch options with the ssl-client-subject-dn and ssl-client-issuer-dn headers automatically included.
 * This should be used for server-side requests (API routes, Server Components).
 *
 * @param options - Additional fetch options to merge
 * @param incomingHeaders - Optional headers from the incoming request (for Kubernetes/ingress)
 *                         If not provided, will try to get from Next.js headers() (Server Components)
 * @returns Fetch options with SSL client DN headers included
 */
export async function createApiRequestOptions(
  options: RequestInit = {},
  incomingHeaders?: Headers | Record<string, string>
): Promise<RequestInit> {
  // If headers not provided, try to get from Next.js context (Server Components)
  let headersToUse = incomingHeaders
  if (!headersToUse) {
    headersToUse = await tryGetHeadersFromContext()
  }

  const subjectDn = getSubjectDn(headersToUse)
  const issuerDn = getIssuerDn(headersToUse)

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
 * @param incomingHeaders - Optional headers from the incoming request (for Kubernetes/ingress)
 *                         If not provided, will try to get from Next.js headers() (Server Components)
 * @returns Promise resolving to the response
 */
export async function apiRequest(
  path: string,
  options: RequestInit = {},
  incomingHeaders?: Headers | Record<string, string>
): Promise<Response> {
  const url = path.startsWith('http') ? path : `${API_BASE_URL}${path}`
  const requestOptions = await createApiRequestOptions(options, incomingHeaders)

  const requestDetails: Record<string, unknown> = {
    url,
    method: options.method || 'GET',
    hasHeaders: !!incomingHeaders,
    apiBaseUrl: API_BASE_URL,
    path,
    headers: requestOptions.headers,
  }

  if (requestOptions.body !== undefined) {
    requestDetails.body = parseBody(requestOptions.body as string)
  }

  // Debug logging
  console.log('API Request:', requestDetails)

  return fetch(url, requestOptions)
}

function parseBody(body: string | undefined): unknown {
  if (!body) {
    return undefined
  }
  try {
    return JSON.parse(body)
  } catch {
    return body
  }
}
/**
 * Makes a fetch request and parses the JSON response.
 *
 * @param path - API path (e.g., '/api/books')
 * @param options - Additional fetch options
 * @param incomingHeaders - Optional headers from the incoming request (for Kubernetes/ingress)
 * @returns Promise resolving to the parsed JSON data
 */
export async function apiRequestJson<T = unknown>(
  path: string,
  options: RequestInit = {},
  incomingHeaders?: Headers | Record<string, string>
): Promise<T> {
  const response = await apiRequest(path, options, incomingHeaders)
  if (!response.ok) {
    const errorText = await response.text().catch(() => 'Unknown error')
    let errorMessage = `API request failed: ${response.status} ${response.statusText}`
    try {
      const errorJson = JSON.parse(errorText)
      errorMessage = errorJson.message || errorJson.detail || errorMessage
    } catch {
      if (errorText) {
        errorMessage = `${errorMessage}: ${errorText}`
      }
    }
    throw new Error(errorMessage)
  }
  return response.json()
}
