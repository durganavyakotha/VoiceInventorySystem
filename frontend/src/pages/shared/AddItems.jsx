import { useCallback, useEffect, useRef, useState } from 'react';
import VoiceRecorder from '../../components/VoiceRecorder';
import { inventoryService } from '../../services/inventoryService';

export default function AddItems() {
  const [tab, setTab] = useState('camera');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [voiceText, setVoiceText] = useState('');
  const [voiceResult, setVoiceResult] = useState(null);
  const [barcode, setBarcode] = useState('');
  const [barcodeName, setBarcodeName] = useState('');
  const [barcodeQty, setBarcodeQty] = useState(1);
  const [manual, setManual] = useState({
    productName: '',
    quantity: 1,
    threshold: 5,
    barcode: '',
    category: '',
  });
  const [capturedFile, setCapturedFile] = useState(null);
  const [preview, setPreview] = useState(null);
  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const streamRef = useRef(null);

  const stopCamera = useCallback(() => {
    streamRef.current?.getTracks().forEach((t) => t.stop());
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
          stream.getTracks().forEach((t) => t.stop());
          return;
        }
        streamRef.current = stream;
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
        }
      } catch {
        setError('Camera access denied or unavailable');
      }
    })();
    return () => {
      cancelled = true;
      stopCamera();
    };
  }, [tab, stopCamera]);

  useEffect(() => {
    if (tab !== 'barcode' || !('BarcodeDetector' in window)) return undefined;
    let cancelled = false;
    let detector;
    let raf;
    (async () => {
      try {
        detector = new window.BarcodeDetector({ formats: ['ean_13', 'ean_8', 'code_128', 'qr_code', 'upc_a'] });
        const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
        if (cancelled) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }
        const video = document.createElement('video');
        video.srcObject = stream;
        video.setAttribute('playsinline', 'true');
        await video.play();
        const scan = async () => {
          if (cancelled) return;
          try {
            const codes = await detector.detect(video);
            if (codes?.[0]?.rawValue) {
              setBarcode(codes[0].rawValue);
            }
          } catch {
            /* ignore frame errors */
          }
          raf = requestAnimationFrame(scan);
        };
        scan();
        streamRef.current = stream;
      } catch {
        /* BarcodeDetector optional */
      }
    })();
    return () => {
      cancelled = true;
      if (raf) cancelAnimationFrame(raf);
    };
  }, [tab]);

  const capture = () => {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas) return;
    canvas.width = video.videoWidth || 640;
    canvas.height = video.videoHeight || 480;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(video, 0, 0);
    canvas.toBlob((blob) => {
      if (!blob) return;
      const file = new File([blob], `capture-${Date.now()}.jpg`, { type: 'image/jpeg' });
      setCapturedFile(file);
      setPreview(URL.createObjectURL(blob));
      setMessage('Photo captured — confirm product details below');
    }, 'image/jpeg', 0.9);
  };

  const submitManual = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    try {
      const { data } = await inventoryService.add({
        productName: manual.productName,
        quantity: Number(manual.quantity),
        threshold: Number(manual.threshold),
        barcode: manual.barcode || undefined,
        category: manual.category || undefined,
      });
      if (capturedFile && data?.id) {
        await inventoryService.uploadImage(data.id, capturedFile);
      }
      setMessage(`Saved ${data.productName}`);
      setManual({ productName: '', quantity: 1, threshold: 5, barcode: '', category: '' });
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
    try {
      const { data } = await inventoryService.barcode(
        barcode,
        barcodeName || undefined,
        Number(barcodeQty) || 1
      );
      setMessage(`Barcode item: ${data.productName} (qty ${data.quantity})`);
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
      const { data } = await inventoryService.voiceCommand(voiceText.trim());
      setVoiceResult(data);
      setMessage(data.spokenResponse || (data.success ? 'Command processed' : 'Command failed'));
    } catch (err) {
      setError(err.response?.data?.message || 'Voice command failed');
    }
  };

  return (
    <div className="stack">
      <h1>Add items</h1>
      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}
      <div className="tabs">
        {['camera', 'barcode', 'voice'].map((t) => (
          <button
            key={t}
            type="button"
            className={`tab ${tab === t ? 'active' : ''}`}
            onClick={() => setTab(t)}
          >
            {t[0].toUpperCase() + t.slice(1)}
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
            <button type="button" className="btn" onClick={capture}>Capture photo</button>
          </div>
          {preview && <img src={preview} alt="Capture preview" style={{ maxWidth: 280, borderRadius: 12 }} />}
          <form className="panel form-grid two" onSubmit={submitManual}>
            <p style={{ gridColumn: '1 / -1', margin: 0 }} className="muted">
              Recognition stub: confirm the product details before saving.
            </p>
            <label>
              Product name
              <input
                required
                value={manual.productName}
                onChange={(e) => setManual({ ...manual, productName: e.target.value })}
              />
            </label>
            <label>
              Quantity
              <input
                type="number"
                min={0}
                required
                value={manual.quantity}
                onChange={(e) => setManual({ ...manual, quantity: e.target.value })}
              />
            </label>
            <label>
              Threshold
              <input
                type="number"
                min={0}
                value={manual.threshold}
                onChange={(e) => setManual({ ...manual, threshold: e.target.value })}
              />
            </label>
            <label>
              Category
              <input
                value={manual.category}
                onChange={(e) => setManual({ ...manual, category: e.target.value })}
              />
            </label>
            <label style={{ gridColumn: '1 / -1' }}>
              Barcode (optional)
              <input
                value={manual.barcode}
                onChange={(e) => setManual({ ...manual, barcode: e.target.value })}
              />
            </label>
            <button className="btn" type="submit">Save item</button>
          </form>
        </div>
      )}

      {tab === 'barcode' && (
        <form className="panel form-grid" onSubmit={submitBarcode}>
          <p className="muted" style={{ marginTop: 0 }}>
            Enter a barcode or use BarcodeDetector if your browser supports it.
          </p>
          <label>
            Barcode
            <input value={barcode} onChange={(e) => setBarcode(e.target.value)} required />
          </label>
          <label>
            Product name (if new)
            <input value={barcodeName} onChange={(e) => setBarcodeName(e.target.value)} />
          </label>
          <label>
            Quantity
            <input type="number" min={1} value={barcodeQty} onChange={(e) => setBarcodeQty(e.target.value)} />
          </label>
          <button className="btn" type="submit">Submit barcode</button>
        </form>
      )}

      {tab === 'voice' && (
        <div className="panel stack">
          <p className="muted" style={{ marginTop: 0 }}>
            Try: “Add 10 rice” or “How much oil do I have?”
          </p>
          <textarea
            rows={4}
            value={voiceText}
            onChange={(e) => setVoiceText(e.target.value)}
            placeholder="Voice command transcript"
          />
          <div className="row">
            <VoiceRecorder onTranscript={setVoiceText} />
            <button type="button" className="btn" onClick={submitVoice}>Send command</button>
          </div>
          {voiceResult && (
            <div className="alert alert-info">
              <div><strong>Action:</strong> {voiceResult.action}</div>
              <div>{voiceResult.spokenResponse}</div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
