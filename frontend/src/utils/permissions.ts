interface OrgMembership {
  org: string;
  role: 'member' | 'eboard';
}

interface User {
  isOSIAdmin: boolean;
  organizations: OrgMembership[];
  currentOrg: string;
}

interface Item {
  org: string;
}

function currentOrgRole(user: User): 'member' | 'eboard' | null {
  const membership = user.organizations.find((o) => o.org === user.currentOrg);
  return membership?.role ?? null;
}

export function canViewFullInventory(user: User): boolean {
  if (user.isOSIAdmin) return true;
  return currentOrgRole(user) === 'eboard';
}

export function canEditItem(item: Item, user: User): boolean {
  if (user.isOSIAdmin) return true;
  return currentOrgRole(user) === 'eboard' && item.org === user.currentOrg;
}

export function canCheckOutItem(item: Item, user: User): boolean {
  if (user.isOSIAdmin) return true;
  // Eboard can check out any item; members can only check out their own org's items
  const role = currentOrgRole(user);
  if (role === 'eboard') return true;
  return item.org === user.currentOrg;
}

export function canCheckInItem(item: Item, user: User): boolean {
  // Same rules as checkout — whoever can take it out can bring it back
  return canCheckOutItem(item, user);
}
