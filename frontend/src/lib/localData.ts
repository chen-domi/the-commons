import { inventoryItems } from '../data/inventory';
import { InventoryItem, ItemRequest } from '../types';

const INVENTORY_KEY = 'commons.inventory';
const REQUESTS_KEY = 'commons.requests';
const PINS_KEY = 'commons.organizationPins';

function read<T>(key: string, fallback: T): T {
  try {
    const value = localStorage.getItem(key);
    return value ? JSON.parse(value) as T : fallback;
  } catch {
    return fallback;
  }
}

function write<T>(key: string, value: T): T {
  localStorage.setItem(key, JSON.stringify(value));
  return value;
}

export const localData = {
  getInventory(): InventoryItem[] {
    return read(INVENTORY_KEY, inventoryItems);
  },

  saveInventory(items: InventoryItem[]): InventoryItem[] {
    return write(INVENTORY_KEY, items);
  },

  getRequests(): ItemRequest[] {
    return read(REQUESTS_KEY, []);
  },

  saveRequests(requests: ItemRequest[]): ItemRequest[] {
    return write(REQUESTS_KEY, requests);
  },

  getPin(org: string): string {
    return read<Record<string, string>>(PINS_KEY, {})[org] ?? '1234';
  },

  setPin(org: string, pin: string): void {
    const pins = read<Record<string, string>>(PINS_KEY, {});
    write(PINS_KEY, { ...pins, [org]: pin });
  },
};
