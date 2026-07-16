import { useAuth } from '../context/AuthContext';

/**
 * Combines auth context user with the session-scoped org role from localStorage.
 */
export function useCurrentUser() {
  const { user, loading } = useAuth();
  const currentOrg = localStorage.getItem('currentOrg') ?? user?.currentOrg ?? '';
  const currentRole = localStorage.getItem('currentRole') as 'eboard' | null;
  const isEboard = currentRole === 'eboard';
  const isAdmin = !!user?.isOSIAdmin;

  return {
    user,
    loading,
    currentOrg,
    currentRole,
    isEboard,
    isAdmin,
    /** True if the user can modify inventory (eboard or OSI admin) */
    canEdit: isAdmin || isEboard,
  };
}
