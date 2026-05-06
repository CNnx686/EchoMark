export interface AudioResponseDto {
  id: number;
  userId: number;
  userName: string;
  audioUrl: string;
  photoUrl: string;
  latitude: number;
  longitude: number;
  title: string;
  description: string;
  tags: string[];
  uploadTime: string; // ISO 8601 字符串，如 "2025-12-29T01:23:45Z"
  publishTime: string; // 同上
  status: string;
  visitCount: number;
}

export interface ApiResponse<T> {
  code: number;
  msg?: string;
  data: T;
}

/**
 * 回复（Reply）数据结构
 */
export interface ReplyResponse {
  replyId: number;
  userId?: number;
  username: string;
  content: string;
  createTime: string; // ISO 8601 格式字符串，例如 "2025-12-29T10:00:00"
  likes?: number;
  userLiked?: boolean;
}

/**
 * 评论（含回复列表）
 */
export interface CommentWithRepliesResponse {
  commentId: number;
  userId: number;
  username: string;
  content: string;
  createTime: string; // ISO 8601
  likes?: number;
  userLiked?: boolean;
  replies?: ReplyResponse[]; // 回复列表
}

/**
 * 音频详情（包含评论）
 */
export interface AudioResponseDetail {
  audioId: number;
  likes: number;
  userLiked: boolean;
  comments: CommentWithRepliesResponse[]; // 评论列表
}

/*评论提交响应数据*/
export interface CommentResponseData {
  id: number;
  audioId: number;
  userId: number;
  username: string;
  content: string;
  createTime: string; // ISO 8601 格式字符串
}

/*回复提交响应数据*/
export interface ReplyResponseData {
  id: number;
  content: string;
  username: string;
  createTime: string; // ISO 8601 格式字符串
}

/**
 * AI推荐响应
 */
export interface RecommendationResponse {
  reason: string; // 推荐理由
  audioIds: number[]; // 推荐的音频ID列表
}
