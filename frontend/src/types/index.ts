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

export interface ScanResult {
  item: InventoryItem;
  action: 'Checked Out' | 'Checked In';
}
