import { useEffect, useRef, useState } from 'react';
import { Camera, X, CheckCircle, QrCode, CameraOff } from 'lucide-react';
import { Html5Qrcode } from 'html5-qrcode';
import { ScanResult } from '../types';
import { demoScanItems } from '../data/inventory';

interface QRScannerModalProps {
  onClose: () => void;
  onScan: (qrCode: string) => void;
  scanResult: ScanResult | null;
  checkedOutItems: string[];
}

export default function QRScannerModal({
  onClose,
  onScan,
  scanResult,
  checkedOutItems,
}: QRScannerModalProps) {
  return (
    <div
      className="fixed inset-0 flex items-center justify-center z-50 p-4"
      style={{ backgroundColor: 'rgba(0,0,0,0.75)' }}
      onClick={onClose}
    >
      <div
        className="bg-white rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
          <div className="flex items-center gap-2">
            <Camera size={20} style={{ color: '#8B0000' }} />
            <span className="font-semibold text-gray-800">QR Code Scanner</span>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 transition-colors">
            <X size={20} />
          </button>
        </div>

        {scanResult ? (
          <SuccessScreen scanResult={scanResult} />
        ) : (
          <ScannerView onScan={onScan} checkedOutItems={checkedOutItems} />
        )}
      </div>
    </div>
  );
}

// ── Success screen ─────────────────────────────────────────────────────────────

function SuccessScreen({ scanResult }: { scanResult: ScanResult }) {
  return (
    <div className="flex flex-col items-center px-6 py-10 text-center">
      <div className="w-20 h-20 rounded-full bg-green-100 flex items-center justify-center mb-4">
        <CheckCircle size={44} className="text-green-500" />
      </div>
      <h3 className="text-xl font-bold text-gray-800 mb-1">{scanResult.action}!</h3>
      <p className="text-gray-600 font-medium mb-1">{scanResult.item.name}</p>
      <p className="text-sm text-gray-400 mb-2">{scanResult.item.org}</p>
      <span className="text-xs font-mono bg-gray-100 text-gray-500 px-3 py-1 rounded-full">
        {scanResult.item.qrCode}
      </span>
    </div>
  );
}

// ── Scanner view ───────────────────────────────────────────────────────────────

function ScannerView({ onScan, checkedOutItems }: { onScan: (qr: string) => void; checkedOutItems: string[] }) {
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [cameraReady, setCameraReady] = useState(false);
  const runningRef = useRef(false);
  const scannedRef = useRef(false);

  useEffect(() => {
    // Clear any previously injected camera HTML (handles React StrictMode double-invoke)
    const el = document.getElementById('qr-reader-feed');
    if (el) el.innerHTML = '';

    const qr = new Html5Qrcode('qr-reader-feed');

    qr.start(
      { facingMode: 'environment' },
      { fps: 10, qrbox: { width: 220, height: 220 } },
      (decodedText) => {
        if (scannedRef.current) return;
        scannedRef.current = true;
        runningRef.current = false;
        qr.stop().catch(() => {}).finally(() => onScan(decodedText));
      },
      () => {}
    )
      .then(() => { runningRef.current = true; setCameraReady(true); })
      .catch(() => setCameraError('Camera access denied. Use the demo buttons below.'));

    return () => {
      if (runningRef.current) {
        runningRef.current = false;
        qr.stop().catch(() => {});
      }
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="px-5 py-5">
      {/* Camera feed */}
      <div className="relative rounded-xl overflow-hidden mb-4" style={{ minHeight: 240, backgroundColor: '#111' }}>
        <div id="qr-reader-feed" className="w-full" />

        {/* Corner brackets overlay */}
        {cameraReady && !cameraError && (
          <>
            {([
              { top: 12, left: 12, bt: 3, bl: 3, bb: 0, br: 0 },
              { top: 12, right: 12, bt: 3, bl: 0, bb: 0, br: 3 },
              { bottom: 12, left: 12, bt: 0, bl: 3, bb: 3, br: 0 },
              { bottom: 12, right: 12, bt: 0, bl: 0, bb: 3, br: 3 },
            ] as any[]).map((c, i) => (
              <div key={i} className="absolute w-7 h-7 pointer-events-none"
                style={{
                  top: c.top, left: c.left, right: c.right, bottom: c.bottom,
                  borderColor: '#CFB87C', borderStyle: 'solid',
                  borderTopWidth: c.bt, borderLeftWidth: c.bl,
                  borderBottomWidth: c.bb, borderRightWidth: c.br,
                }} />
            ))}
            <p className="absolute bottom-3 left-0 right-0 text-center text-xs text-gray-400 pointer-events-none">
              Align QR code within frame
            </p>
          </>
        )}

        {/* Camera error state */}
        {cameraError && (
          <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 p-4 text-center">
            <CameraOff size={28} className="text-gray-500" />
            <p className="text-sm text-gray-400">{cameraError}</p>
          </div>
        )}

        {/* Loading state */}
        {!cameraReady && !cameraError && (
          <div className="absolute inset-0 flex items-center justify-center">
            <p className="text-sm text-gray-500">Starting camera…</p>
          </div>
        )}
      </div>

      {/* Demo buttons */}
      <p className="text-xs text-gray-400 uppercase tracking-wider font-semibold mb-3 text-center">
        Demo — Quick Scan
      </p>
      <div className="space-y-2">
        {demoScanItems.map(({ qr, label, org }) => {
          const isOut = checkedOutItems.includes(qr);
          return (
            <button key={qr} onClick={() => onScan(qr)}
              className="w-full flex items-center justify-between px-4 py-3 rounded-xl border transition-all hover:shadow-md active:scale-95"
              style={{ borderColor: isOut ? '#ef4444' : '#CFB87C', backgroundColor: isOut ? '#fef2f2' : '#fffbeb' }}>
              <div className="text-left">
                <p className="text-sm font-semibold text-gray-800">{label}</p>
                <p className="text-xs text-gray-500">{org}</p>
              </div>
              <div className="flex items-center gap-2">
                <span className="text-xs font-semibold px-2 py-0.5 rounded-full"
                  style={isOut
                    ? { backgroundColor: '#fee2e2', color: '#dc2626' }
                    : { backgroundColor: '#dcfce7', color: '#16a34a' }}>
                  {isOut ? 'Check In' : 'Check Out'}
                </span>
                <QrCode size={15} className="text-gray-400" />
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}
