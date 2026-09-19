import { useEffect, useState } from 'react';
import api from '../services/api';
import { useI18n } from '../context/LanguageContext';

export default function LocationSelect({ value, onChange, required = false }) {
  const { t } = useI18n();
  const [locations, setLocations] = useState([]);
  const selected = locations.find((l) => l.name === value);

  useEffect(() => {
    api.get('/api/locations').then(({ data }) => setLocations(data || [])).catch(() => {
      setLocations([
        { name: 'Markapur', latitude: 15.735, longitude: 79.27 },
        { name: 'Ongole', latitude: 15.5057, longitude: 80.0499 },
        { name: 'Guntur', latitude: 16.3067, longitude: 80.4365 },
        { name: 'Vijayawada', latitude: 16.5062, longitude: 80.648 },
        { name: 'Nellore', latitude: 14.4426, longitude: 79.9865 },
        { name: 'Tirupati', latitude: 13.6288, longitude: 79.4192 },
        { name: 'Kurnool', latitude: 15.8281, longitude: 78.0373 },
        { name: 'Hyderabad', latitude: 17.385, longitude: 78.4867 },
        { name: 'Chennai', latitude: 13.0827, longitude: 80.2707 },
        { name: 'Bangalore', latitude: 12.9716, longitude: 77.5946 },
      ]);
    });
  }, []);

  const mapSrc = selected
    ? `https://www.openstreetmap.org/export/embed.html?bbox=${selected.longitude - 0.08}%2C${selected.latitude - 0.06}%2C${selected.longitude + 0.08}%2C${selected.latitude + 0.06}&layer=mapnik&marker=${selected.latitude}%2C${selected.longitude}`
    : null;

  return (
    <div className="stack" style={{ gap: '0.75rem' }}>
      <label>
        {t('selectLocation')}
        <select
          value={value || ''}
          onChange={(e) => onChange(e.target.value)}
          required={required}
        >
          <option value="">— {t('selectLocation')} —</option>
          {locations.map((loc) => (
            <option key={loc.name} value={loc.name}>{loc.name}</option>
          ))}
        </select>
      </label>
      {mapSrc && (
        <div>
          <p className="muted" style={{ margin: '0 0 0.35rem' }}>{t('mapPreview')} — {selected.name}</p>
          <iframe
            title="location-map"
            className="location-map"
            src={mapSrc}
            loading="lazy"
            referrerPolicy="no-referrer-when-downgrade"
          />
        </div>
      )}
    </div>
  );
}
