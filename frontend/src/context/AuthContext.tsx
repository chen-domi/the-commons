import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { endCurrentSession, getCurrentUser } from '../api/authApi';
import {
  getMyMemberships,
  joinOrganization,
  leaveOrganization,
} from '../api/organizationApi';
import { AuthUser } from '../types';

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
    let cancelled = false;

    async function loadSession() {
      try {
        const authenticatedUser = await getCurrentUser();
        if (cancelled) return;

        if (!authenticatedUser) {
          setUser(null);
          setNeedsOrgSelection(false);
          return;
        }

        const memberships = await getMyMemberships();
        if (cancelled) return;

        const organizations = memberships.map((membership) => ({
          org: membership.organizationName,
          role: 'eboard' as const,
        }));
        const savedOrg = localStorage.getItem('currentOrg') ?? '';
        const savedOrgIsValid =
          authenticatedUser.globalRole === 'ADMIN' ||
          organizations.some((membership) => membership.org === savedOrg);
        const currentOrg = savedOrgIsValid
          ? savedOrg
          : organizations[0]?.org ?? '';

        if (currentOrg) {
          localStorage.setItem('currentOrg', currentOrg);
          localStorage.setItem('currentRole', 'eboard');
        } else {
          localStorage.removeItem('currentOrg');
          localStorage.removeItem('currentRole');
        }

        setUser({
          id: authenticatedUser.email,
          name: authenticatedUser.name,
          email: authenticatedUser.email,
          organizations,
          currentOrg,
          isOSIAdmin: authenticatedUser.globalRole === 'ADMIN',
        });
        setNeedsOrgSelection(!currentOrg);
      } catch (error) {
        if (cancelled) return;
        setUser(null);
        setAuthError(
          error instanceof Error
            ? error.message
            : 'Could not check login status'
        );
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    loadSession();
    return () => { cancelled = true; };
  }, []);

  const selectOrg = useCallback((orgName: string, role: 'eboard') => {
    localStorage.setItem('currentOrg', orgName);
    localStorage.setItem('currentRole', role);
    setUser((previous) => ({
      ...(previous ?? demoUser()),
      currentOrg: orgName,
      isOSIAdmin: previous?.isOSIAdmin ?? false,
      organizations: previous?.organizations.some((item) => item.org === orgName)
        ? previous.organizations
        : [...(previous?.organizations ?? []), { org: orgName, role }],
    }));
    setNeedsOrgSelection(false);
  }, []);

  const logout = useCallback(async () => {
    try {
      await endCurrentSession();
    } catch (error) {
      setAuthError(error instanceof Error ? error.message : 'Could not sign out');
      return;
    }

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
    await joinOrganization(orgName, pin);
    return 'eboard';
  }, []);

  const leaveOrg = useCallback(async (orgName: string) => {
    await leaveOrganization(orgName);

    setUser((previous) => {
      if (!previous) return previous;

      const organizations = previous.organizations.filter(
        (membership) => membership.org !== orgName
      );
      const currentOrg = previous.currentOrg === orgName
        ? organizations[0]?.org ?? ''
        : previous.currentOrg;

      if (currentOrg) {
        localStorage.setItem('currentOrg', currentOrg);
        localStorage.setItem('currentRole', 'eboard');
      } else {
        localStorage.removeItem('currentOrg');
        localStorage.removeItem('currentRole');
      }

      setNeedsOrgSelection(!currentOrg && !previous.isOSIAdmin);

      return {
        ...previous,
        organizations,
        currentOrg,
      };
    });
  }, []);
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
