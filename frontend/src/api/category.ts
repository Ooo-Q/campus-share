import request from './request'

export interface Category {
  id: number
  name: string
  sortOrder?: number
}

export function fetchCategories() {
  return request.get<{ success: boolean; data: Category[] }>('/categories')
}

