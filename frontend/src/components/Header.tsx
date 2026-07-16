import { Package, MapPin, LogOut, ChevronDown } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Header() {
  const { user, logout, switchOrg } = useAuth();

  const isAdmin = !!user?.isOSIAdmin;
  const orgs = user?.organizations ?? [];

  return (
    <header style={{ background: isAdmin
      ? 'linear-gradient(135deg, #1a2744 0%, #0f172a 100%)'
      : 'linear-gradient(135deg, #8B0000 0%, #5a0000 100%)' }}>
      <div className="max-w-6xl mx-auto px-4 py-5">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          {/* Title block */}
          <div>
            <div className="flex items-center gap-3 mb-1">
              <div className="w-11 h-11 rounded-xl flex items-center justify-center shadow-md flex-shrink-0"
                style={{ backgroundColor: '#CFB87C' }}>
                <Package size={22} style={{ color: '#8B0000' }} />
              </div>
              <div>
                <div className="flex items-center gap-2 flex-wrap">
                  <h1 className="text-2xl font-bold text-white tracking-tight">The Commons</h1>
                </div>
                <p className="text-sm text-red-200 mt-0.5">
                  Smart Inventory &amp; Sharing Marketplace for BC Student Organizations
                </p>
              </div>
            </div>
            <div className="flex items-center gap-1 mt-1.5 ml-14 text-xs text-red-300">
              <MapPin size={11} />
              <span>Boston College Student Organizations</span>
            </div>
          </div>

          {/* Right side */}
          <div className="flex items-center gap-3 flex-shrink-0">
            {user && (
              <div className="text-right hidden sm:block">
                <p className="text-sm font-semibold text-white mb-1">{user.name}</p>

                {/* Org switcher */}
                {isAdmin ? (
                  <span className="text-xs font-bold px-2 py-1 rounded-full"
                    style={{ backgroundColor: '#CFB87C', color: '#8B0000' }}>
                    OSI Admin
                  </span>
                ) : orgs.length > 1 ? (
                  <div className="relative inline-block">
                    <select
                      value={user.currentOrg}
                      onChange={(e) => switchOrg(e.target.value)}
                      className="appearance-none text-xs font-semibold pl-2.5 pr-6 py-1 rounded-full cursor-pointer focus:outline-none"
                      style={{ backgroundColor: '#CFB87C', color: '#8B0000' }}
                    >
                      {orgs.map((o) => (
                        <option key={o.org} value={o.org}>{o.org}</option>
                      ))}
                    </select>
                    <ChevronDown size={11} className="absolute right-1.5 top-1/2 -translate-y-1/2 pointer-events-none"
                      style={{ color: '#8B0000' }} />
                  </div>
                ) : (
                  <span className="text-xs font-semibold px-2 py-1 rounded-full inline-block"
                    style={{ backgroundColor: '#CFB87C', color: '#8B0000' }}>
                    {user.currentOrg}
                  </span>
                )}
              </div>
            )}

            {user && (
              <button
                onClick={logout}
                title="Sign out"
                className="flex items-center gap-1.5 px-3 py-2.5 rounded-xl text-sm font-semibold border border-white/20 text-white/80 hover:bg-white/10 transition-colors"
              >
                <LogOut size={15} />
                <span className="hidden sm:inline">Sign out</span>
              </button>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}
