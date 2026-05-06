export enum NotificationType {
  LIKE = 'LIKE',
  COMMENT = 'COMMENT',
  REPLY = 'REPLY'
}

export enum TargetType {
  AUDIO = 'AUDIO',
  COMMENT = 'COMMENT',
  REPLY = 'REPLY'
}

export interface NotificationDTO {
  id: number;
  receiverUserId: number;
  actorUserId: number;
  type: NotificationType;
  content: string;
  targetType: TargetType;
  targetId: number;
  createdAt: string; // ISO 8601 字符串，如 "2025-04-01T12:00:00Z"
  isRead: boolean; // 是否已读
}
