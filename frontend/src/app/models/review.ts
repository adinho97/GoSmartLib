export interface Review {
  id: number;
  rating: number;
  comment: string;
  reviewerUserId?: number;
  reviewerUserName?: string;
  canManage?: boolean;
  canEdit?: boolean;
  createdAt: string;
}
