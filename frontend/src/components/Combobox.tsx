import React, { useState, useRef, useEffect } from 'react';
import { ChevronDown, X } from 'lucide-react';

interface ComboboxProps {
  options: string[];
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  /** If provided, a "clear" option appears at the top that sets value to '' */
  allOptionLabel?: string;
  className?: string;
  style?: React.CSSProperties;
  required?: boolean;
}

export default function Combobox({
  options,
  value,
  onChange,
  placeholder = 'Type or select…',
  allOptionLabel,
  className = '',
  style,
  required,
}: ComboboxProps) {
  const [query, setQuery] = useState(value);
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Keep query in sync when value changes externally (e.g. tab reset)
  useEffect(() => { setQuery(value); }, [value]);

  // Close on outside click
  useEffect(() => {
    function handler(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        // On blur: if query doesn't match any option, keep it as a free-text value
        // (useful for location "Other"). If it matches, confirm the selection.
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const filtered = options.filter((o) => o.toLowerCase().includes(query.toLowerCase()));

  function select(opt: string) {
    onChange(opt);
    setQuery(opt);
    setOpen(false);
  }

  function clear() {
    onChange('');
    setQuery('');
    inputRef.current?.focus();
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === 'Enter') {
      e.preventDefault();
      if (filtered.length > 0) select(filtered[0]);
      else { onChange(query); setOpen(false); } // free-text
    }
    if (e.key === 'Escape') setOpen(false);
    if (e.key === 'ArrowDown') { e.preventDefault(); setOpen(true); }
  }

  const showClear = !allOptionLabel && value;

  return (
    <div ref={ref} className={`relative ${className}`}>
      <div className="relative">
        <input
          ref={inputRef}
          type="text"
          value={query}
          placeholder={placeholder}
          required={required && !value}
          onChange={(e) => { setQuery(e.target.value); setOpen(true); onChange(e.target.value); }}
          onFocus={() => setOpen(true)}
          onKeyDown={handleKeyDown}
          className="w-full px-3.5 py-2.5 pr-8 text-sm rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:border-transparent bg-white"
          style={style}
        />
        <span className="absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none text-gray-400">
          {showClear
            ? <button type="button" onMouseDown={clear} className="pointer-events-auto hover:text-gray-600"><X size={13} /></button>
            : <ChevronDown size={13} />
          }
        </span>
      </div>

      {open && (
        <ul className="absolute z-30 left-0 right-0 top-full mt-1 bg-white border border-gray-200 rounded-xl shadow-lg overflow-y-auto max-h-52">
          {allOptionLabel && (
            <li onMouseDown={() => select('')}
              className="px-3.5 py-2.5 text-sm cursor-pointer hover:bg-amber-50 transition-colors italic"
              style={{ color: value === '' ? '#8B0000' : '#9ca3af', fontWeight: value === '' ? 600 : 400 }}>
              {allOptionLabel}
            </li>
          )}
          {filtered.length === 0 ? (
            <li className="px-3.5 py-2.5 text-sm text-gray-400 text-center">No matches</li>
          ) : (
            filtered.map((opt) => (
              <li key={opt} onMouseDown={() => select(opt)}
                className="px-3.5 py-2.5 text-sm cursor-pointer hover:bg-amber-50 transition-colors"
                style={{ color: opt === value ? '#8B0000' : '#374151', fontWeight: opt === value ? 600 : 400 }}>
                {opt}
              </li>
            ))
          )}
        </ul>
      )}
    </div>
  );
}
