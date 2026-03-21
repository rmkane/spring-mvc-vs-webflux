/**
 * Auth header names for backend API requests.
 * Aligns with backend SecurityConstants and acme.security.headers config.
 *
 * Override via env (e.g. when backend uses different header names):
 * - ACME_HEADER_SUBJECT_DN
 * - ACME_HEADER_ISSUER_DN
 */

const DEFAULT_SUBJECT_HEADER = 'x-amzn-mtls-clientcert-subject'
const DEFAULT_ISSUER_HEADER = 'x-amzn-mtls-clientcert-issuer'

export const SSL_CLIENT_SUBJECT_HEADER =
  process.env.ACME_HEADER_SUBJECT_DN ?? DEFAULT_SUBJECT_HEADER

export const SSL_CLIENT_ISSUER_HEADER = process.env.ACME_HEADER_ISSUER_DN ?? DEFAULT_ISSUER_HEADER
