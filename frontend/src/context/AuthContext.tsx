import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { AuthUser } from '../types';
import { localData } from '../lib/localData';

export interface AuthContextValue {
  user: AuthUser | null;
  loading: boolean;
  needsOrgSelection: boolean;
  authError: string | null;
  selectOrg: (orgName: string, role: 'eboard') => void;
  logout: () => Promise<void>;
  switchOrg: (org: string) => void;
  joinOrg: (orgName: string, pin: string) => Promise<'eboard'>;
  leaveOrg: (orgName: string) => Promise<void>;
  clearAuthError: () => void;
  devLogin: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function demoUser(): AuthUser {
  const currentOrg = localStorage.getItem('currentOrg') || 'UGBC';
  return {
    id: 'local-demo-user',
    name: 'Demo User',
    email: 'demo@bc.edu',
    organizations: [{ org: currentOrg, role: 'eboard' }],
    currentOrg,
    isOSIAdmin: currentOrg === 'OSI',
  };
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [needsOrgSelection, setNeedsOrgSelection] = useState(false);
  const [authError, setAuthError] = useState<string | null>(null);

  const devLogin = useCallback(() => {
    localStorage.setItem('currentOrg', localStorage.getItem('currentOrg') || 'UGBC');
    localStorage.setItem('currentRole', 'eboard');
    setUser(demoUser());
    setNeedsOrgSelection(false);
  }, []);

  useEffect(() => {
    // Temporary local session: open the dashboard immediately until Spring auth is connected.
    devLogin();
    setLoading(false);
  }, [devLogin]);

  const selectOrg = useCallback((orgName: string, role: 'eboard') => {
    localStorage.setItem('currentOrg', orgName);
    localStorage.setItem('currentRole', role);
    setUser((previous) => ({
      ...(previous ?? demoUser()),
      currentOrg: orgName,
      isOSIAdmin: orgName === 'OSI',
      organizations: previous?.organizations.some((item) => item.org === orgName)
        ? previous.organizations
        : [...(previous?.organizations ?? []), { org: orgName, role }],
    }));
    setNeedsOrgSelection(false);
  }, []);

  const logout = useCallback(async () => {
    localStorage.removeItem('currentOrg');
    localStorage.removeItem('currentRole');
    setUser(null);
    setNeedsOrgSelection(false);
    setAuthError(null);
  }, []);

  const switchOrg = useCallback((org: string) => {
    if (!org) {
      setNeedsOrgSelection(true);
      return;
    }
    selectOrg(org, 'eboard');
  }, [selectOrg]);

  const joinOrg = useCallback(async (orgName: string, pin: string): Promise<'eboard'> => {
    if (pin !== localData.getPin(orgName)) throw new Error('Incorrect PIN. Try 1234 for local demo data.');
    return 'eboard';
  }, []);

  const leaveOrg = useCallback(async (_orgName: string) => {}, []);
  const clearAuthError = useCallback(() => setAuthError(null), []);

  return <AuthContext.Provider value={{
    user, loading, needsOrgSelection, authError, selectOrg, logout, switchOrg,
    joinOrg, leaveOrg, clearAuthError, devLogin,
  }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}
