export interface TicketComment {
  commentId: number;
  ticketId: number;
  authorId: number;
  commentText: string;
  createdAt: string;
}

export interface Ticket {
  ticketId: number;
  clientId: number;
  accountId?: number;
  subject: string;
  description: string;
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'REOPENED';
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  category: string;
  assignedTo?: number;
  createdAt: string;
  updatedAt?: string;
  resolvedAt?: string;
  closedAt?: string;
  comments: TicketComment[];
}

export interface TicketRequest {
  clientId: number;
  accountId?: number;
  subject: string;
  description: string;
  priority: string;
  category: string;
}

export interface TicketCommentRequest {
  commentText: string;
}
