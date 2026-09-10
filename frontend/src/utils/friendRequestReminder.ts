/** 进入「消息-好友申请」Tab 后合并进集合，用于首页/Tab 角标只提示尚未点进该 Tab 看过的待处理申请 */

const LS_STUDENT = 'student_seen_friend_request_ids'
const LS_ADMIN = 'admin_seen_friend_request_ids'

function storageKey(role: 'student' | 'admin') {
  return role === 'admin' ? LS_ADMIN : LS_STUDENT
}

function parseSeenIds(key: string): Set<number> {
  try {
    const arr = JSON.parse(localStorage.getItem(key) || '[]') as unknown
    if (!Array.isArray(arr)) return new Set()
    return new Set(arr.filter((x): x is number => typeof x === 'number'))
  } catch {
    return new Set()
  }
}

export function pendingIncomingFriendRequestsNotAcknowledged(
  requests: Array<{ id: number; status: string; isFromMe?: boolean }>,
  role: 'student' | 'admin',
): number {
  const seen = parseSeenIds(storageKey(role))
  return requests.filter(r => r.status === 'PENDING' && !r.isFromMe && !seen.has(r.id)).length
}

export function acknowledgeIncomingFriendRequestIds(ids: number[], role: 'student' | 'admin') {
  const key = storageKey(role)
  const set = parseSeenIds(key)
  for (const id of ids) set.add(id)
  localStorage.setItem(key, JSON.stringify([...set]))
}
