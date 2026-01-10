'use client'

import { useEffect } from 'react'

import { Alert } from '@/components/Alert'

export interface Toast {
  id: string
  message: string
  type: 'success' | 'error' | 'info' | 'warning'
}

interface ToastProps {
  toast: Toast
  onDismiss: (id: string) => void
}

export function Toast({ toast, onDismiss }: ToastProps) {
  useEffect(() => {
    const timer = setTimeout(() => {
      onDismiss(toast.id)
    }, 5000) // Auto-dismiss after 5 seconds

    return () => clearTimeout(timer)
  }, [toast.id, onDismiss])

  return (
    <div className="animate-slide-in mb-2">
      <Alert type={toast.type} message={toast.message} />
    </div>
  )
}
