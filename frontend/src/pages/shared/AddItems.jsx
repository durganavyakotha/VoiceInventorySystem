import { useCallback, useEffect, useRef, useState } from 'react';
import VoiceRecorder from '../../components/VoiceRecorder';
import { inventoryService } from '../../services/inventoryService';
import { useAuth } from '../../context/AuthContext';
import { useI18n } from '../../context/LanguageContext';

const UNITS = ['kg', 'g', 'L', 'ml', 'packets', 'bottles', 'pieces', 'bags'];
const CATEGORIES = ['Grains', 'Grocery', 'Beverages', 'Snacks', 'Personal Care', 'Dairy', 'General'];

export default function AddItems() {
  const { t, lang } = useI18n();
  const { user } = useAuth();
  const [tab, setTab] = useState('camera');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [voiceText, setVoiceText] = useState('');
  const [voiceCards, setVoiceCards] = useState([]);
  const [voiceFile, setVoiceFile] = useState(null);
  const [voicePreview, setVoicePreview] = useState(null);
  const [barcode, setBarcode] = useState('');
  const [barcodeName, setBarcodeName] = useState('');
  const [barcodeQty, setBarcodeQty] = useState(1);
  const [barcodeUnit, setBarcodeUnit] = useState('pieces');
  const [barcodeCategory, setBarcodeCategory] = useState('General');
  const [barcodeFile, setBarcodeFile] = useState(null);
  const [barcodePreview, setBarcodePreview] = useState(null);
  const [manual, setManual] = useState({
    productName: '',
    quantity: 1,
    threshold: 5,
    barcode: '',
    category: 'General',
    unit: 'kg',
    costPerUnit: 0,
  });
  const [capturedFile, setCapturedFile] = useState(null);
  const [preview, setPreview] = useState(null);
  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const streamRef = useRef(null);

  const stopCamera = useCallback(() => {
    streamRef.current?.getTracks().forEach((tr) => tr.stop());
    streamRef.current = null;
  }, []);

  useEffect(() => {
    if (tab !== 'camera') {
      stopCamera();
      return undefined;
    }
    let cancelled = false;
    (async () => {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
        if (cancelled) {
          stream.getTracks().forEach((tr) => tr.stop());
          return;
        }
        streamRef.current = stream;
        if (videoRef.current) videoRef.current.srcObject = stream;
      } catch {
        setError('Camera access denied or unavailable — you can still upload a photo');
      }
    })();
    return () => {
      cancelled = true;
      stopCamera();
    };
  }, [tab, stopCamera]);

  const capture = () => {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas) return;
    canvas.width = video.videoWidth || 640;
    canvas.height = video.videoHeight || 480;
    canvas.getContext('2d').drawImage(video, 0, 0);
    canvas.toBlob((blob) => {
      if (!blob) return;
      const file = new File([blob], `capture-${Date.now()}.jpg`, { type: 'image/jpeg' });
      setCapturedFile(file);
      setPreview(URL.createObjectURL(blob));
      setMessage('Photo captured');
    }, 'image/jpeg', 0.9);
  };

  const onUploadCamera = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setCapturedFile(file);
    setPreview(URL.createObjectURL(file));
  };

  const submitManual = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    if (!capturedFile) {
      setError(t('pictureRequired'));
      return;
    }
    try {
      const { data } = await inventoryService.addWithImage({
        productName: manual.productName,
        quantity: Number(manual.quantity),
        threshold: Number(manual.threshold),
        barcode: manual.barcode || undefined,
        category: manual.category,
        unit: manual.unit,
        costPerUnit: Number(manual.costPerUnit) || 0,
        file: capturedFile,
      });
      setMessage(`${data.productName} — ${data.quantity} ${data.unit}`);
      setManual({ productName: '', quantity: 1, threshold: 5, barcode: '', category: 'General', unit: 'kg', costPerUnit: 0 });
      setCapturedFile(null);
      setPreview(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Save failed');
    }
  };

  const submitBarcode = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    if (!barcodeFile) {
      setError(t('pictureRequired'));
      return;
    }
    try {
      const { data } = await inventoryService.barcodeWithImage({
        barcode,
        productName: barcodeName || undefined,
        quantity: Number(barcodeQty) || 1,
        unit: barcodeUnit,
        category: barcodeCategory,
        file: barcodeFile,
      });
      setMessage(`${data.productName} — ${data.quantity} ${data.unit}`);
    } catch (err) {
      setError(err.response?.data?.message || 'Barcode lookup failed');
    }
  };

  const submitVoice = async (e) => {
    e?.preventDefault();
    if (!voiceText.trim()) return;
    setError('');
    setMessage('');
    try {
      const { data } = await inventoryService.voiceCommand(
        voiceText.trim(),
        user?.language || lang,
        true
      );
      if (!data.success) {
        setError(data.spokenResponse || 'Could not understand');
        return;
      }
      if (data.action === 'PREVIEW_ADD' || data.productName) {
        const card = {
          id: `v-${Date.now()}`,
          productName: data.productName || '',
          quantity: data.quantity ?? 1,
          unit: data.unit || 'pieces',
          costPerUnit: data.costPerUnit ?? 0,
          confidence: data.confidence,
          matchedFrom: data.matchedFrom,
          spokenResponse: data.spokenResponse,
          editing: true,
        };
        setVoiceCards((prev) => [card, ...prev.filter((c) => c.productName !== card.productName)]);
        setMessage(data.spokenResponse || 'Item recognized — edit if needed, then confirm');
      } else {
        setMessage(data.spokenResponse || 'OK');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Voice command failed');
    }
  };

  const updateVoiceCard = (id, patch) => {
    setVoiceCards((prev) => prev.map((c) => (c.id === id ? { ...c, ...patch } : c)));
  };

  const confirmVoiceCard = async (card) => {
    if (!voiceFile) {
      setError(t('pictureRequired'));
      return;
    }
    if (!card.productName?.trim()) {
      setError('Product name is required');
      return;
    }
    setError('');
    try {
      const { data } = await inventoryService.addWithImage({
        productName: card.productName.trim(),
        quantity: Number(card.quantity) || 1,
        unit: card.unit || 'pieces',
        category: 'General',
        costPerUnit: Number(card.costPerUnit) || 0,
        file: voiceFile,
      });
      setMessage(`Saved ${data.productName} — ${data.quantity} ${data.unit} @ ₹${data.costPerUnit || 0}`);
      setVoiceCards((prev) => prev.filter((c) => c.id !== card.id));
      setVoiceText('');
    } catch (err) {
      setError(err.response?.data?.message || 'Save failed');
    }
  };

  const discardVoiceCard = (id) => {
    setVoiceCards((prev) => prev.filter((c) => c.id !== id));
  };

  return (
    <div className="stack">
      <h1>{t('addItems')}</h1>
      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}
      <div className="tabs">
        {['camera', 'barcode', 'voice'].map((tabKey) => (
          <button
            key={tabKey}
            type="button"
            className={`tab ${tab === tabKey ? 'active' : ''}`}
            onClick={() => setTab(tabKey)}
          >
            {t(tabKey)}
          </button>
        ))}
      </div>

      {tab === 'camera' && (
        <div className="stack">
          <div className="video-wrap">
            <video ref={videoRef} autoPlay playsInline muted />
            <canvas ref={canvasRef} hidden />
          </div>
          <div className="row">
            <button type="button" className="btn" onClick={capture}>{t('capturePhoto')}</button>
            <label className="btn btn-secondary" style={{ cursor: 'pointer' }}>
              {t('uploadPhoto')}
              <input type="file" accept="image/*" hidden onChange={onUploadCamera} />
            </label>
          </div>
          {preview && <img src={preview} alt="Preview" style={{ maxWidth: 280, borderRadius: 12 }} />}
          <form className="panel form-grid two" onSubmit={submitManual}>
            <label>
              {t('productName')}
              <input
                required
                value={manual.productName}
                onChange={(e) => setManual({ ...manual, productName: e.target.value })}
              />
            </label>
            <label>
              {t('quantity')}
              <input
                type="number"
                min={0}
                required
                value={manual.quantity}
                onChange={(e) => setManual({ ...manual, quantity: e.target.value })}
              />
            </label>
            <label>
              {t('unit')}
              <select value={manual.unit} onChange={(e) => setManual({ ...manual, unit: e.target.value })}>
                {UNITS.map((u) => <option key={u} value={u}>{u}</option>)}
              </select>
            </label>
            <label>
              Cost / unit (₹)
              <input
                type="number"
                min={0}
                step="0.01"
                value={manual.costPerUnit}
                onChange={(e) => setManual({ ...manual, costPerUnit: e.target.value })}
              />
            </label>
            <label>
              {t('category')}
              <select value={manual.category} onChange={(e) => setManual({ ...manual, category: e.target.value })}>
                {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
              </select>
            </label>
            <label>
              {t('threshold')}
              <input
                type="number"
                min={0}
                value={manual.threshold}
                onChange={(e) => setManual({ ...manual, threshold: e.target.value })}
              />
            </label>
            <label>
              {t('barcode')}
              <input value={manual.barcode} onChange={(e) => setManual({ ...manual, barcode: e.target.value })} />
            </label>
            <button className="btn" type="submit">{t('saveItem')}</button>
          </form>
        </div>
      )}

      {tab === 'barcode' && (
        <form className="panel form-grid" onSubmit={submitBarcode}>
          <label>
            {t('barcode')}
            <input value={barcode} onChange={(e) => setBarcode(e.target.value)} required />
          </label>
          <label>
            {t('productName')}
            <input value={barcodeName} onChange={(e) => setBarcodeName(e.target.value)} />
          </label>
          <label>
            {t('quantity')}
            <input type="number" min={1} value={barcodeQty} onChange={(e) => setBarcodeQty(e.target.value)} />
          </label>
          <label>
            {t('unit')}
            <select value={barcodeUnit} onChange={(e) => setBarcodeUnit(e.target.value)}>
              {UNITS.map((u) => <option key={u} value={u}>{u}</option>)}
            </select>
          </label>
          <label>
            {t('category')}
            <select value={barcodeCategory} onChange={(e) => setBarcodeCategory(e.target.value)}>
              {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
          </label>
          <label>
            {t('uploadPhoto')} *
            <input
              type="file"
              accept="image/*"
              required
              onChange={(e) => {
                const file = e.target.files?.[0];
                setBarcodeFile(file || null);
                setBarcodePreview(file ? URL.createObjectURL(file) : null);
              }}
            />
          </label>
          {barcodePreview && <img src={barcodePreview} alt="" style={{ maxWidth: 200, borderRadius: 12 }} />}
          <button className="btn" type="submit">{t('saveItem')}</button>
        </form>
      )}

      {tab === 'voice' && (
        <div className="panel stack">
          <p className="muted" style={{ marginTop: 0 }}>
            Type or speak in English for best accuracy: <code>10 kg rice add price 45</code>
            {' '}— name, quantity, unit and price are separated automatically. You can edit before confirm.
          </p>
          <label>
            {t('uploadPhoto')} * (required before confirm)
            <input
              type="file"
              accept="image/*"
              onChange={(e) => {
                const file = e.target.files?.[0];
                setVoiceFile(file || null);
                setVoicePreview(file ? URL.createObjectURL(file) : null);
              }}
            />
          </label>
          {voicePreview && <img src={voicePreview} alt="" style={{ maxWidth: 200, borderRadius: 12 }} />}
          <textarea
            rows={3}
            value={voiceText}
            onChange={(e) => setVoiceText(e.target.value)}
            placeholder="10 kg rice add price 45"
          />
          <div className="row">
            <VoiceRecorder
              onTranscript={setVoiceText}
              lang="en-IN"
              finalOnly
            />
            <button type="button" className="btn" onClick={submitVoice}>{t('sendCommand')}</button>
          </div>

          <div className="item-grid">
            {voiceCards.map((card) => (
              <div key={card.id} className="item-card voice-preview-card">
                <div className="item-card-body">
                  <span className="item-category">Edit &amp; confirm</span>
                  <label>
                    Name
                    <input
                      value={card.productName}
                      onChange={(e) => updateVoiceCard(card.id, { productName: e.target.value })}
                    />
                  </label>
                  <div className="form-grid two" style={{ gap: '0.5rem' }}>
                    <label>
                      Qty
                      <input
                        type="number"
                        min={1}
                        value={card.quantity}
                        onChange={(e) => updateVoiceCard(card.id, { quantity: e.target.value })}
                      />
                    </label>
                    <label>
                      Unit
                      <select
                        value={card.unit}
                        onChange={(e) => updateVoiceCard(card.id, { unit: e.target.value })}
                      >
                        {UNITS.map((u) => <option key={u} value={u}>{u}</option>)}
                      </select>
                    </label>
                    <label style={{ gridColumn: '1 / -1' }}>
                      Price (₹ / unit)
                      <input
                        type="number"
                        min={0}
                        step="0.01"
                        value={card.costPerUnit}
                        onChange={(e) => updateVoiceCard(card.id, { costPerUnit: e.target.value })}
                      />
                    </label>
                  </div>
                  {card.confidence != null && (
                    <p className="muted" style={{ margin: 0, fontSize: '0.8rem' }}>
                      Match {(card.confidence * 100).toFixed(0)}%
                      {card.matchedFrom && card.matchedFrom !== card.productName
                        ? ` (heard “${card.matchedFrom}”)`
                        : ''}
                    </p>
                  )}
                  <div className="row" style={{ gap: '0.35rem', marginTop: '0.5rem' }}>
                    <button type="button" className="btn btn-sm" onClick={() => confirmVoiceCard(card)}>
                      Confirm save
                    </button>
                    <button type="button" className="btn btn-sm btn-danger" onClick={() => discardVoiceCard(card.id)}>
                      Remove
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
