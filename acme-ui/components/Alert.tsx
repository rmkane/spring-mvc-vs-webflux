interface AlertProps {
  type?: 'error' | 'success' | 'info' | 'warning'
  message: string
  className?: string
}

export function Alert({ type = 'error', message, className = '' }: AlertProps) {
  const styles = {
    error:
      'bg-red-100 dark:bg-red-900/30 border-red-400 dark:border-red-700 text-red-700 dark:text-red-300',
    success:
      'bg-green-100 dark:bg-green-900/30 border-green-400 dark:border-green-700 text-green-700 dark:text-green-300',
    info: 'bg-blue-100 dark:bg-blue-900/30 border-blue-400 dark:border-blue-700 text-blue-700 dark:text-blue-300',
    warning:
      'bg-yellow-100 dark:bg-yellow-900/30 border-yellow-400 dark:border-yellow-700 text-yellow-700 dark:text-yellow-300',
  }

  return (
    <div
      className={`rounded border p-4 ${styles[type]} ${className}`}
      role="alert"
      aria-live="polite"
    >
      {message}
    </div>
  )
}
