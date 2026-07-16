import { useState, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';

interface PinVerificationResult {
  verifyPin: (orgName: string, pin: string) => Promise<void>;
  loading: boolean;
  error: string | null;
  clearError: () => void;
}

/**
 * Verifies an e-board PIN for the given organization.
 * On success, stores org + role in localStorage and updates auth context.
 */
export function usePinVerification(): PinVerificationResult {
  const { joinOrg, selectOrg } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const verifyPin = useCallback(async (orgName: string, pin: string) => {
    if (!orgName) { setError('Please select an organization.'); return; }
    if (pin.length !== 4) { setError('Please enter your 4-digit PIN.'); return; }

    setLoading(true);
    setError(null);
    try {
      await joinOrg(orgName, pin);
      selectOrg(orgName, 'eboard');
    } catch (err: any) {
      setError(err.message ?? 'Something went wrong. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [joinOrg, selectOrg]);

  const clearError = useCallback(() => setError(null), []);

  return { verifyPin, loading, error, clearError };
}
