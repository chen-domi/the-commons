export interface InventoryItem {
  id: number;
  qrCode: string;
  name: string;
  category: string;
  org: string;
  location: string;
  quantity: number;
  lastUsed: string;
  shared: boolean;
  checkedOut?: boolean;
  createdAt?: string;
  borrowCount?: number;
  checkoutPurpose?: string;
  checkoutDueDate?: string;
  checkedOutBy?: string;
}

export interface AuthUser {
  id?: string;
  name: string;
  email: string;
  organizations: Array<{
    org: string;
    role: 'member' | 'eboard';
  }>;
  currentOrg: string;
  isOSIAdmin: boolean;
}

export interface ItemRequest {
  id: number;
  org: string;
  itemName: string;
  category: string | null;
  notes: string | null;
  status?: 'pending' | 'fulfilled';
  createdBy?: string;
  createdAt: string;
}

export type BorrowingRequestStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'DENIED'
  | 'CANCELLED'
  | 'RETURNED';

export interface BorrowingRequest {
  id: number;
  inventoryItemId: number;
  itemName: string;
  qrCode: string;
  lendingOrganization: string;
  borrowingOrganization: string;
  quantity: number;
  purpose: string;
  startDate: string;
  dueDate: string;
  status: BorrowingRequestStatus;
  requestedByName: string;
  reviewedByName: string | null;
  createdAt: string;
  reviewedAt: string | null;
  cancelledAt: string | null;
  returnedAt: string | null;
}

export interface BorrowingNotificationSummary {
  pendingIncomingRequests: number;
  approvedOutgoingBorrows: number;
}

export interface ScanResult {
  item: InventoryItem;
  action: 'Checked Out' | 'Checked In';
}
