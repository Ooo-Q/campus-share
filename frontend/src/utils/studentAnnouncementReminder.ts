const STORAGE_KEY = 'student_last_announcement_count'

export function getLastRecordedAnnouncementCount(): number | null {
  const stored = localStorage.getItem(STORAGE_KEY)
  return stored ? parseInt(stored, 10) : null
}

export function acknowledgeStudentAnnouncements(currentCount: number) {
  localStorage.setItem(STORAGE_KEY, String(Math.max(0, currentCount)))
}

export function getNewAnnouncementCount(currentCount: number): number {
  const lastCount = getLastRecordedAnnouncementCount()

  if (lastCount === null) {
    return currentCount
  }

  if (currentCount < lastCount) {
    acknowledgeStudentAnnouncements(currentCount)
    return 0
  }

  return Math.max(0, currentCount - lastCount)
}
