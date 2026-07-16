import { Leaf, Trophy } from 'lucide-react';
import { InventoryItem } from '../types';
import { CATEGORY_CO2_KG, DEFAULT_CO2 } from '../data/inventory';
import { useAuth } from '../context/AuthContext';

interface OrgStat {
  org: string;
  co2Saved: number;
  totalBorrows: number;
  itemsShared: number;
}

function computeStats(items: InventoryItem[]): OrgStat[] {
  const map = new Map<string, OrgStat>();

  for (const item of items) {
    if (!map.has(item.org)) {
      map.set(item.org, { org: item.org, co2Saved: 0, totalBorrows: 0, itemsShared: 0 });
    }
    const stat = map.get(item.org)!;
    const borrows = item.borrowCount ?? 0;
    const factor = CATEGORY_CO2_KG[item.category] ?? DEFAULT_CO2;
    stat.co2Saved += borrows * factor;
    stat.totalBorrows += borrows;
    if (item.shared) stat.itemsShared += 1;
  }

  return Array.from(map.values()).sort((a, b) => b.co2Saved - a.co2Saved);
}

function formatCO2(kg: number): string {
  if (kg === 0) return '0 kg';
  if (kg >= 1000) return `${(kg / 1000).toFixed(1)} t`;
  return `${kg % 1 === 0 ? kg : kg.toFixed(1)} kg`;
}

const MEDAL_COLORS = [
  { bg: '#FEF9C3', border: '#CFB87C', text: '#92400e', label: '🥇 1st' },
  { bg: '#F1F5F9', border: '#94a3b8', text: '#475569', label: '🥈 2nd' },
  { bg: '#FFF7ED', border: '#c2713f', text: '#7c2d12', label: '🥉 3rd' },
];

interface PodiumCardProps {
  stat: OrgStat;
  rank: number;
  isCurrentOrg: boolean;
}

function PodiumCard({ stat, rank, isCurrentOrg }: PodiumCardProps) {
  const colors = MEDAL_COLORS[rank - 1];
  const height = rank === 1 ? 'pt-4' : rank === 2 ? 'pt-8' : 'pt-10';

  return (
    <div className={`flex-1 flex flex-col items-center ${height}`}>
      <div className="w-full rounded-2xl border-2 p-4 flex flex-col items-center text-center transition-all"
        style={{
          backgroundColor: colors.bg,
          borderColor: isCurrentOrg ? '#8B0000' : colors.border,
          boxShadow: isCurrentOrg ? '0 0 0 2px #8B000033' : undefined,
        }}>
        <span className="text-lg font-bold mb-1" style={{ color: colors.text }}>{colors.label}</span>
        <p className="text-sm font-bold text-gray-800 leading-tight mb-2">{stat.org}</p>
        <div className="flex items-center gap-1 mb-1">
          <Leaf size={13} className="text-green-600" />
          <span className="text-base font-bold text-green-700">{formatCO2(stat.co2Saved)}</span>
        </div>
        <span className="text-xs text-gray-500">CO₂ saved</span>
        <div className="mt-2 pt-2 border-t border-black/10 w-full flex justify-around text-xs text-gray-500">
          <span><strong className="text-gray-700">{stat.totalBorrows}</strong> borrows</span>
          <span><strong className="text-gray-700">{stat.itemsShared}</strong> shared</span>
        </div>
        {isCurrentOrg && (
          <span className="mt-2 text-xs font-semibold px-2 py-0.5 rounded-full"
            style={{ backgroundColor: '#8B0000', color: 'white' }}>
            Your org
          </span>
        )}
      </div>
    </div>
  );
}

export default function Leaderboard({ items }: { items: InventoryItem[] }) {
  const { user } = useAuth();
  const stats = computeStats(items);
  const hasAnyBorrows = stats.some((s) => s.totalBorrows > 0);

  if (stats.length === 0) {
    return (
      <div className="text-center py-16 text-gray-400">
        <Trophy size={40} className="mx-auto mb-3 opacity-30" />
        <p className="font-medium">No organizations in the system yet.</p>
      </div>
    );
  }

  const podium = stats.slice(0, 3);
  const rest = stats.slice(3);

  // Reorder podium: 2nd, 1st, 3rd for visual podium effect
  const podiumOrder = podium.length === 3
    ? [podium[1], podium[0], podium[2]]
    : podium.length === 2
      ? [podium[1], podium[0]]
      : podium;
  const podiumRanks = podium.length === 3 ? [2, 1, 3] : podium.length === 2 ? [2, 1] : [1];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-2">
        <Trophy size={20} style={{ color: '#CFB87C' }} />
        <h2 className="text-lg font-bold text-gray-800">Sustainability Leaderboard</h2>
        <span className="ml-auto text-xs text-gray-400 italic">Ranked by CO₂ saved through lending</span>
      </div>

      {!hasAnyBorrows && (
        <div className="flex items-center gap-3 px-4 py-3 rounded-xl border"
          style={{ backgroundColor: '#f0fdf4', borderColor: '#86efac' }}>
          <Leaf size={15} className="text-green-600 flex-shrink-0" />
          <p className="text-sm text-green-800">
            No borrows recorded yet — scores will appear once items are checked out via QR scan.
          </p>
        </div>
      )}

      {/* Podium */}
      <div className="flex items-end gap-3">
        {podiumOrder.map((stat, i) => (
          <PodiumCard
            key={stat.org}
            stat={stat}
            rank={podiumRanks[i]}
            isCurrentOrg={stat.org === user?.currentOrg}
          />
        ))}
      </div>

      {/* Rest of rankings */}
      {rest.length > 0 && (
        <div className="rounded-2xl border border-gray-100 overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50">
                <th className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider text-left">Rank</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider text-left">Organization</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider text-right">CO₂ Saved</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider text-right">Borrows</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider text-right">Shared</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {rest.map((stat, i) => {
                const rank = i + 4;
                const isCurrentOrg = stat.org === user?.currentOrg;
                return (
                  <tr key={stat.org}
                    className="bg-white hover:bg-gray-50 transition-colors"
                    style={isCurrentOrg ? { outline: '2px solid #8B0000', outlineOffset: '-2px' } : undefined}>
                    <td className="px-4 py-3 font-semibold text-gray-400">#{rank}</td>
                    <td className="px-4 py-3">
                      <span className="font-medium text-gray-800">{stat.org}</span>
                      {isCurrentOrg && (
                        <span className="ml-2 text-xs font-semibold px-1.5 py-0.5 rounded-full"
                          style={{ backgroundColor: '#fff1f2', color: '#8B0000' }}>
                          You
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <span className="flex items-center justify-end gap-1 text-green-700 font-semibold">
                        <Leaf size={11} />{formatCO2(stat.co2Saved)}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right text-gray-600">{stat.totalBorrows}</td>
                    <td className="px-4 py-3 text-right text-gray-600">{stat.itemsShared}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Footer note */}
      <p className="text-center text-xs text-gray-400 italic">
        CO₂ estimates based on lifecycle carbon footprint analysis per item category. 1 borrow = ~1 item not purchased new.
      </p>
    </div>
  );
}
