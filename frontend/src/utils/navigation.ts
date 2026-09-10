export function defaultHomePath(role?: string): string {
  if (role === 'ADMIN') return '/admin'
  if (role === 'STUDENT') return '/student'
  return '/login'
}
