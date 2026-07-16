import { InventoryItem } from '../types';

export const inventoryItems: InventoryItem[] = [
  {
    id: 1,
    qrCode: 'BC-CAB-TABLE-001',
    name: 'Folding Tables (set of 6)',
    category: 'Furniture',
    org: 'Campus Activities Board',
    location: 'Carney Hall, Suite 147',
    quantity: 6,
    lastUsed: 'Student Involvement Fair',
    shared: true,
  },
  {
    id: 2,
    qrCode: 'BC-CAB-SOUND-001',
    name: 'Sound System & Speakers',
    category: 'AV Equipment',
    org: 'Campus Activities Board',
    location: 'Carney Storage',
    quantity: 1,
    lastUsed: 'Mudstock',
    shared: false,
  },
  {
    id: 3,
    qrCode: 'BC-UGBC-BANNER-001',
    name: 'Event Banners (x4)',
    category: 'Signage',
    org: 'UGBC',
    location: 'Carney Hall, Suite 147',
    quantity: 4,
    lastUsed: 'Homecoming',
    shared: true,
  },
  {
    id: 4,
    qrCode: 'BC-ICI-TENT-001',
    name: 'Pop-up Tents (x3)',
    category: 'Outdoor',
    org: 'Il Circolo Italiano',
    location: 'Mod Lot Storage',
    quantity: 3,
    lastUsed: 'ALC Showdown',
    shared: true,
  },
  {
    id: 5,
    qrCode: 'BC-ASO-LIGHTS-001',
    name: 'String Lights (200ft)',
    category: 'Décor',
    org: 'African Student Organization',
    location: 'Carney Storage',
    quantity: 2,
    lastUsed: 'Stokes Set',
    shared: false,
  },
  {
    id: 6,
    qrCode: 'BC-ARC-FIRSTAID-001',
    name: 'First Aid Kits (x5)',
    category: 'Safety',
    org: 'American Red Cross of BC',
    location: 'Carney Hall, Suite 147',
    quantity: 5,
    lastUsed: 'Student Involvement Fair',
    shared: true,
  },
  {
    id: 7,
    qrCode: 'BC-ENV-COMPOST-001',
    name: 'Compost Bins (x8)',
    category: 'Sustainability',
    org: 'Environmental Club',
    location: 'Mod Lot Storage',
    quantity: 8,
    lastUsed: 'Mudstock',
    shared: true,
  },
];

export const CATEGORY_CO2_KG: Record<string, number> = {
  'AV Equipment':   8,
  'Furniture':      5,
  'Outdoor':        4,
  'Safety':         3,
  'Décor':          2,
  'Signage':        1.5,
  'Sustainability': 1,
};

export const DEFAULT_CO2 = 2;

export const categoryColors: Record<string, string> = {
  Furniture: 'bg-blue-100 text-blue-700',
  'AV Equipment': 'bg-purple-100 text-purple-700',
  Signage: 'bg-orange-100 text-orange-700',
  Outdoor: 'bg-green-100 text-green-700',
  'Décor': 'bg-pink-100 text-pink-700',
  Safety: 'bg-red-100 text-red-700',
  Sustainability: 'bg-emerald-100 text-emerald-700',
};

export const demoScanItems = [
  { qr: 'BC-CAB-TABLE-001', label: 'Folding Tables', org: 'Campus Activities Board' },
  { qr: 'BC-CAB-SOUND-001', label: 'Sound System', org: 'Campus Activities Board' },
  { qr: 'BC-UGBC-BANNER-001', label: 'Event Banners', org: 'UGBC' },
  { qr: 'BC-ICI-TENT-001', label: 'Pop-up Tents', org: 'Il Circolo Italiano' },
];
