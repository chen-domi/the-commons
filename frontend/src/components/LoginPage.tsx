import React, { useState, useEffect, useRef } from 'react';
import { Eye, EyeOff, AlertCircle, ChevronRight, Search, X, ShieldCheck } from 'lucide-react';
import { supabase } from '../lib/supabase';
import { useAuth } from '../context/AuthContext';

const OSI_ADMIN_CODE = '2026';

// ── Shared shell ───────────────────────────────────────────────────────────────

function Shell({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen flex items-center justify-center p-4" style={{ backgroundColor: '#6B0000' }}>
      {children}
    </div>
  );
}

// ── Logo / header ──────────────────────────────────────────────────────────────

function Logo() {
  return (
    <div className="text-center mb-8">
      <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl mb-4 text-2xl font-black shadow-lg"
        style={{ backgroundColor: '#CFB87C', color: '#6B0000' }}>BC</div>
      <h1 className="text-3xl font-bold text-white">The Commons</h1>
      <p className="mt-2 text-sm" style={{ color: 'rgba(207,184,124,0.6)' }}>
        Boston College Student Organizations
      </p>
    </div>
  );
}

// ── Google button ──────────────────────────────────────────────────────────────

function GoogleButton({ onClick, loading }: { onClick: () => void; loading?: boolean }) {
  return (
    <button type="button" onClick={onClick} disabled={loading}
      className="w-full py-3 rounded-xl font-semibold text-sm flex items-center justify-center gap-3 transition-all hover:bg-gray-50 active:scale-95 bg-white border border-gray-200 text-gray-700 disabled:opacity-60">
      <svg width="18" height="18" viewBox="0 0 48 48">
        <path fill="#EA4335" d="M24 9.5c3.5 0 6.6 1.2 9.1 3.2l6.8-6.8C35.8 2.4 30.2 0 24 0 14.6 0 6.6 5.5 2.7 13.5l7.9 6.1C12.5 13.3 17.8 9.5 24 9.5z"/>
        <path fill="#4285F4" d="M46.6 24.5c0-1.6-.1-3.1-.4-4.5H24v8.5h12.7c-.6 3-2.3 5.5-4.8 7.2l7.6 5.9c4.5-4.1 7.1-10.2 7.1-17.1z"/>
        <path fill="#FBBC05" d="M10.6 28.4A14.8 14.8 0 0 1 9.5 24c0-1.5.3-3 .7-4.4l-7.9-6.1A23.9 23.9 0 0 0 0 24c0 3.9.9 7.5 2.7 10.7l7.9-6.3z"/>
        <path fill="#34A853" d="M24 48c6.2 0 11.4-2 15.2-5.5l-7.6-5.9c-2 1.4-4.6 2.2-7.6 2.2-6.2 0-11.5-3.8-13.4-9.4l-7.9 6.3C6.6 42.5 14.6 48 24 48z"/>
      </svg>
      {loading ? 'Redirecting…' : 'Continue with Google'}
    </button>
  );
}

// ── Landing screen ─────────────────────────────────────────────────────────────

function LandingScreen() {
  const { devLogin, authError, clearAuthError } = useAuth();
  const [googleLoading, setGoogleLoading] = useState(false);

  async function handleGoogle() {
    clearAuthError();
    setGoogleLoading(true);
    await supabase.auth.signInWithOAuth({
      provider: 'google',
      options: { redirectTo: window.location.origin },
    });
  }

  return (
    <Shell>
      <div className="w-full max-w-xs">
        <Logo />

        {authError && (
          <div className="mb-4 p-3 rounded-xl flex items-start gap-2.5"
            style={{ backgroundColor: 'rgba(255,255,255,0.12)', border: '1px solid rgba(255,255,255,0.2)' }}>
            <AlertCircle size={15} className="flex-shrink-0 mt-0.5" style={{ color: '#CFB87C' }} />
            <p className="text-sm" style={{ color: '#CFB87C' }}>{authError}</p>
          </div>
        )}

        <div className="space-y-3">
          <GoogleButton onClick={handleGoogle} loading={googleLoading} />
        </div>

        <p className="text-center mt-6 text-xs" style={{ color: 'rgba(255,255,255,0.3)' }}>
          Only @bc.edu accounts may access this system
        </p>

        {process.env.NODE_ENV === 'development' && (
          <button onClick={devLogin}
            className="block mx-auto mt-4 text-xs underline underline-offset-2 transition-opacity hover:opacity-60"
            style={{ color: 'rgba(255,255,255,0.25)' }}>
            dev: skip login
          </button>
        )}
      </div>
    </Shell>
  );
}

// ── Org + PIN step ─────────────────────────────────────────────────────────────

function OrgPinStep() {
  const { user, joinOrg, selectOrg } = useAuth();
  const [orgs, setOrgs] = useState<string[]>([]);
  const [loadingOrgs, setLoadingOrgs] = useState(true);
  const [selectedOrg, setSelectedOrg] = useState('');
  const [orgQuery, setOrgQuery] = useState('');
  const [showOrgList, setShowOrgList] = useState(false);
  const [pin, setPin] = useState('');
  const [showPin, setShowPin] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // OSI Admin flow
  const [showAdminField, setShowAdminField] = useState(false);
  const [adminCode, setAdminCode] = useState('');
  const [adminError, setAdminError] = useState('');
  const [adminLoading, setAdminLoading] = useState(false);

  const dropdownRef = useRef<HTMLDivElement>(null);

  // Fetch all org names
  useEffect(() => {
    supabase
      .from('organizations')
      .select('name')
      .order('name')
      .then(({ data }) => {
        if (data) setOrgs(data.map((r: any) => r.name));
        setLoadingOrgs(false);
      });
  }, []);

  // Close dropdown on outside click
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setShowOrgList(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const filteredOrgs = orgQuery.trim()
    ? orgs.filter((o) => o.toLowerCase().includes(orgQuery.toLowerCase()))
    : orgs;

  function handleSelectOrg(name: string) {
    setSelectedOrg(name);
    setOrgQuery(name);
    setShowOrgList(false);
    setError('');
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedOrg) { setError('Please select an organization.'); return; }
    if (pin.length !== 4) { setError('Please enter your 4-digit PIN.'); return; }
    setLoading(true);
    setError('');
    try {
      await joinOrg(selectedOrg, pin);
      selectOrg(selectedOrg, 'eboard');
    } catch (err: any) {
      setError(err.message ?? 'Something went wrong. Please try again.');
      setLoading(false);
    }
  }

  async function handleAdminSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (adminCode !== OSI_ADMIN_CODE) { setAdminError('Incorrect admin code.'); return; }
    setAdminLoading(true);
    // Mark this user as OSI admin in their profile
    const { data: { user: su } } = await supabase.auth.getUser();
    if (su) {
      await supabase.from('profiles').update({ is_osi_admin: true }).eq('id', su.id);
    }
    selectOrg('OSI', 'eboard');
  }

  const inputClass = 'w-full px-4 py-3 rounded-xl text-sm border-2 border-gray-200 focus:outline-none focus:border-red-800 placeholder-gray-400 text-gray-800';

  return (
    <Shell>
      <div className="w-full max-w-sm">
        <div className="text-center mb-6">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl mb-4 text-2xl font-black shadow-lg"
            style={{ backgroundColor: '#CFB87C', color: '#6B0000' }}>BC</div>
          <h1 className="text-2xl font-bold text-white">Welcome, {user?.name?.split(' ')[0]}</h1>
          <p className="mt-1 text-sm" style={{ color: 'rgba(207,184,124,0.55)' }}>
            Select your org and enter your e-board PIN
          </p>
        </div>

        <div className="bg-white rounded-2xl p-7 shadow-2xl">
          {!showAdminField ? (
            <form onSubmit={handleSubmit} className="space-y-5">
              {/* Org selector */}
              <div>
                <label className="block text-xs font-semibold text-gray-600 mb-1.5">Organization</label>
                <div className="relative" ref={dropdownRef}>
                  <div className="relative">
                    <Search size={13} className="absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none text-gray-400" />
                    <input
                      type="text"
                      placeholder={loadingOrgs ? 'Loading orgs…' : 'Search organizations…'}
                      value={orgQuery}
                      disabled={loadingOrgs}
                      autoComplete="off"
                      onChange={(e) => {
                        setOrgQuery(e.target.value);
                        setSelectedOrg('');
                        setShowOrgList(true);
                        setError('');
                      }}
                      onFocus={() => setShowOrgList(true)}
                      className={`${inputClass} pl-9 pr-9`}
                    />
                    {orgQuery && (
                      <button type="button"
                        onClick={() => { setOrgQuery(''); setSelectedOrg(''); setShowOrgList(false); }}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                        <X size={14} />
                      </button>
                    )}
                  </div>

                  {showOrgList && filteredOrgs.length > 0 && (
                    <ul className="absolute z-10 w-full mt-1 bg-white border-2 border-gray-200 rounded-xl shadow-lg overflow-y-auto max-h-48">
                      {filteredOrgs.map((org) => (
                        <li key={org}>
                          <button type="button"
                            onMouseDown={() => handleSelectOrg(org)}
                            className="w-full text-left px-4 py-2.5 text-sm text-gray-700 hover:bg-red-50 hover:text-red-800 transition-colors">
                            {org}
                          </button>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>

              {/* PIN input */}
              <div>
                <label className="block text-xs font-semibold text-gray-600 mb-1.5">E-Board PIN</label>
                <div className="relative">
                  <input
                    type={showPin ? 'text' : 'password'}
                    inputMode="numeric"
                    maxLength={4}
                    placeholder="• • • •"
                    value={pin}
                    onChange={(e) => { setPin(e.target.value.replace(/\D/g, '')); setError(''); }}
                    className={`${inputClass} tracking-[0.6em] text-center font-bold text-lg placeholder-gray-300 pr-12`}
                  />
                  <button type="button"
                    onClick={() => setShowPin((v) => !v)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors">
                    {showPin ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
              </div>

              {error && (
                <p className="flex items-center gap-1.5 text-xs text-red-600">
                  <AlertCircle size={12} />{error}
                </p>
              )}

              <button type="submit" disabled={loading || !selectedOrg || pin.length !== 4}
                className="w-full py-3 rounded-xl text-sm font-bold text-white flex items-center justify-center gap-2 transition-all hover:opacity-90 active:scale-95 disabled:opacity-40"
                style={{ backgroundColor: '#6B0000' }}>
                {loading ? 'Verifying…' : <> Access Dashboard <ChevronRight size={16} /> </>}
              </button>

              {/* OSI Admin toggle */}
              <button type="button"
                onClick={() => setShowAdminField(true)}
                className="w-full text-center text-xs text-gray-400 hover:text-gray-600 transition-colors pt-1">
                OSI Admin access
              </button>
            </form>
          ) : (
            <form onSubmit={handleAdminSubmit} className="space-y-5">
              <div className="flex items-center gap-2 mb-1">
                <ShieldCheck size={16} style={{ color: '#8B0000' }} />
                <p className="font-semibold text-gray-800 text-sm">OSI Admin Access</p>
              </div>
              <p className="text-xs text-gray-500 -mt-3">Enter your admin code to get global access.</p>

              <div>
                <label className="block text-xs font-semibold text-gray-600 mb-1.5">Admin Code</label>
                <input
                  type="password"
                  inputMode="numeric"
                  maxLength={4}
                  placeholder="• • • •"
                  value={adminCode}
                  autoFocus
                  onChange={(e) => { setAdminCode(e.target.value.replace(/\D/g, '')); setAdminError(''); }}
                  className={`${inputClass} tracking-[0.6em] text-center font-bold text-lg placeholder-gray-300`}
                />
                {adminError && (
                  <p className="flex items-center gap-1.5 text-xs text-red-600 mt-1.5">
                    <AlertCircle size={12} />{adminError}
                  </p>
                )}
              </div>

              <button type="submit" disabled={adminLoading || adminCode.length !== 4}
                className="w-full py-3 rounded-xl text-sm font-bold text-white flex items-center justify-center gap-2 transition-all hover:opacity-90 active:scale-95 disabled:opacity-40"
                style={{ backgroundColor: '#6B0000' }}>
                {adminLoading ? 'Verifying…' : <> Enter as Admin <ChevronRight size={16} /> </>}
              </button>

              <button type="button"
                onClick={() => { setShowAdminField(false); setAdminCode(''); setAdminError(''); }}
                className="w-full text-center text-xs text-gray-400 hover:text-gray-600 transition-colors">
                ← Back to org login
              </button>
            </form>
          )}
        </div>

        {/* Sign out link */}
        <button type="button"
          onClick={() => {
            localStorage.removeItem('currentOrg');
            localStorage.removeItem('currentRole');
            supabase.auth.signOut();
          }}
          className="block text-center mx-auto mt-4 text-xs transition-opacity hover:opacity-60"
          style={{ color: 'rgba(255,255,255,0.35)' }}>
          Sign out
        </button>
      </div>
    </Shell>
  );
}

// ── OSI Admin bypass ───────────────────────────────────────────────────────────

function OsiAdminStep() {
  const { selectOrg } = useAuth();
  return (
    <Shell>
      <div className="w-full max-w-xs text-center">
        <Logo />
        <div className="bg-white rounded-2xl p-7 shadow-2xl">
          <p className="text-sm font-semibold text-gray-700 mb-1">OSI Admin Access</p>
          <p className="text-xs text-gray-500 mb-5">You have global access — no PIN required.</p>
          <button
            onClick={() => selectOrg('OSI', 'eboard')}
            className="w-full py-3 rounded-xl text-sm font-bold text-white flex items-center justify-center gap-2 transition-all hover:opacity-90 active:scale-95"
            style={{ backgroundColor: '#6B0000' }}>
            Enter Dashboard <ChevronRight size={16} />
          </button>
        </div>
      </div>
    </Shell>
  );
}

// ── Coordinator ────────────────────────────────────────────────────────────────

export default function LoginPage() {
  const { user, needsOrgSelection } = useAuth();

  // Not logged in yet
  if (!user) return <LandingScreen />;

  // Logged in, OSI admin — skip PIN
  if (needsOrgSelection && user.isOSIAdmin) return <OsiAdminStep />;

  // Logged in, needs to pick org + enter PIN
  if (needsOrgSelection) return <OrgPinStep />;

  // Shouldn't reach here — App.tsx handles showing the dashboard
  return null;
}
