import React, { useState, useEffect } from 'react';
import { Eye, EyeOff, AlertCircle, CheckCircle, ShieldCheck } from 'lucide-react';
import { supabase } from '../lib/supabase';
import { useCurrentUser } from '../hooks/useCurrentUser';
import { useAuth } from '../context/AuthContext';

export default function Settings() {
  const { currentOrg, isAdmin } = useCurrentUser();
  const { switchOrg } = useAuth();

  const [currentPin, setCurrentPin] = useState('');
  const [newPin, setNewPin] = useState('');
  const [confirmPin, setConfirmPin] = useState('');
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  // Display the masked current PIN for reference (reveal on demand)
  const [maskedPin, setMaskedPin] = useState<string | null>(null);
  const [showMasked, setShowMasked] = useState(false);
  const [loadingMasked, setLoadingMasked] = useState(true);

  useEffect(() => {
    if (!currentOrg || currentOrg === 'OSI') { setLoadingMasked(false); return; }
    supabase
      .from('organizations')
      .select('eboard_pin')
      .eq('name', currentOrg)
      .single()
      .then(({ data }) => {
        if (data) setMaskedPin(data.eboard_pin);
        setLoadingMasked(false);
      });
  }, [currentOrg]);

  const inputClass = 'w-full px-4 py-3 rounded-xl text-sm border-2 border-gray-200 focus:outline-none focus:border-red-800 placeholder-gray-400 text-gray-800 bg-white';

  async function handleChangePin(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (newPin.length !== 4) { setError('New PIN must be exactly 4 digits.'); return; }
    if (newPin !== confirmPin) { setError('PINs do not match.'); return; }

    // Verify current PIN against DB (unless OSI admin)
    if (!isAdmin) {
      const { data } = await supabase
        .from('organizations')
        .select('eboard_pin')
        .eq('name', currentOrg)
        .single();
      if (!data || data.eboard_pin !== currentPin) {
        setError('Current PIN is incorrect.');
        return;
      }
    }

    setLoading(true);
    const { error: updateError } = await supabase
      .from('organizations')
      .update({ eboard_pin: newPin })
      .eq('name', currentOrg);

    if (updateError) {
      setError('Failed to update PIN. Please try again.');
    } else {
      setMaskedPin(newPin);
      setSuccess('PIN updated successfully!');
      setCurrentPin('');
      setNewPin('');
      setConfirmPin('');
    }
    setLoading(false);
  }

  const orgDisplay = currentOrg === 'OSI' ? 'All Organizations (OSI Admin)' : currentOrg;

  return (
    <div className="max-w-md mx-auto py-4">
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
          style={{ backgroundColor: '#fff1f2' }}>
          <ShieldCheck size={20} style={{ color: '#8B0000' }} />
        </div>
        <div>
          <h2 className="font-bold text-gray-800">Organization Settings</h2>
          <p className="text-xs text-gray-500 mt-0.5">{orgDisplay}</p>
        </div>
      </div>

      {/* Current PIN display */}
      {currentOrg !== 'OSI' && (
        <div className="bg-gray-50 rounded-2xl p-5 mb-6 border border-gray-100">
          <p className="text-xs font-semibold text-gray-500 mb-2 uppercase tracking-wide">Current E-Board PIN</p>
          <div className="flex items-center gap-3">
            <span className="text-2xl font-mono font-bold tracking-[0.4em] text-gray-800 flex-1">
              {loadingMasked ? '…' : showMasked ? (maskedPin ?? '????') : '••••'}
            </span>
            <button type="button"
              onClick={() => setShowMasked((v) => !v)}
              className="p-2 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-200 transition-colors">
              {showMasked ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          </div>
          <p className="text-xs text-gray-400 mt-2">Share this PIN with e-board members to give them access.</p>
        </div>
      )}

      {/* Change PIN form */}
      <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm">
        <h3 className="font-semibold text-gray-700 mb-4 text-sm">Change PIN</h3>
        <form onSubmit={handleChangePin} className="space-y-4">
          {/* Current PIN — not required for OSI admin */}
          {!isAdmin && (
            <div>
              <label className="block text-xs font-semibold text-gray-600 mb-1.5">Current PIN</label>
              <div className="relative">
                <input
                  type={showCurrent ? 'text' : 'password'}
                  inputMode="numeric"
                  maxLength={4}
                  placeholder="• • • •"
                  value={currentPin}
                  onChange={(e) => { setCurrentPin(e.target.value.replace(/\D/g, '')); setError(''); }}
                  className={`${inputClass} tracking-[0.6em] text-center font-bold text-lg placeholder-gray-300 pr-12`}
                />
                <button type="button" onClick={() => setShowCurrent((v) => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors">
                  {showCurrent ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-gray-600 mb-1.5">New PIN</label>
            <div className="relative">
              <input
                type={showNew ? 'text' : 'password'}
                inputMode="numeric"
                maxLength={4}
                placeholder="• • • •"
                value={newPin}
                onChange={(e) => { setNewPin(e.target.value.replace(/\D/g, '')); setError(''); setSuccess(''); }}
                className={`${inputClass} tracking-[0.6em] text-center font-bold text-lg placeholder-gray-300 pr-12`}
              />
              <button type="button" onClick={() => setShowNew((v) => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors">
                {showNew ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-600 mb-1.5">Confirm New PIN</label>
            <input
              type="password"
              inputMode="numeric"
              maxLength={4}
              placeholder="• • • •"
              value={confirmPin}
              onChange={(e) => { setConfirmPin(e.target.value.replace(/\D/g, '')); setError(''); }}
              className={`${inputClass} tracking-[0.6em] text-center font-bold text-lg placeholder-gray-300`}
            />
          </div>

          {error && (
            <p className="flex items-center gap-1.5 text-xs text-red-600">
              <AlertCircle size={12} />{error}
            </p>
          )}
          {success && (
            <p className="flex items-center gap-1.5 text-xs text-green-600">
              <CheckCircle size={12} />{success}
            </p>
          )}

          <button type="submit"
            disabled={loading || newPin.length !== 4 || confirmPin.length !== 4 || (!isAdmin && currentPin.length !== 4)}
            className="w-full py-3 rounded-xl text-sm font-bold text-white flex items-center justify-center gap-2 transition-all hover:opacity-90 active:scale-95 disabled:opacity-40"
            style={{ backgroundColor: '#8B0000' }}>
            {loading ? 'Updating…' : 'Update PIN'}
          </button>
        </form>
      </div>

      {/* Switch org */}
      <div className="mt-6 p-5 bg-white rounded-2xl border border-gray-100 shadow-sm">
        <h3 className="font-semibold text-gray-700 mb-1 text-sm">Switch Organization</h3>
        <p className="text-xs text-gray-500 mb-4">Return to the org + PIN selection screen.</p>
        <button
          onClick={() => switchOrg('')}
          className="w-full py-2.5 rounded-xl text-sm font-semibold border-2 border-gray-200 text-gray-600 hover:bg-gray-50 transition-colors">
          Switch Organization
        </button>
      </div>
    </div>
  );
}
