// Darija Translator - side panel logic
import { loadSettings, saveSettings, translate, health, DEFAULT_SETTINGS } from './api.js';

const MAX_CHARS = 5000;
const $ = (id) => document.getElementById(id);

const els = {
  source: $('sourceText'),
  charCount: $('charCount'),
  translateBtn: $('translateBtn'),
  clearBtn: $('clearBtn'),
  errorBox: $('errorBox'),
  resultCard: $('resultCard'),
  detected: $('detected'),
  translation: $('translation'),
  meta: $('meta'),
  copyBtn: $('copyBtn'),
  serverDot: $('serverDot'),
  serverUrl: $('serverUrl'),
  username: $('username'),
  password: $('password'),
  saveBtn: $('saveBtn'),
  testBtn: $('testBtn'),
  settingsMsg: $('settingsMsg'),
};

let settings = { ...DEFAULT_SETTINGS };
let lastHandledSelection = 0;
let busy = false;

// ---------- helpers ----------

function selectedScript() {
  return document.querySelector('input[name="script"]:checked').value;
}

function setScript(value) {
  const radio = document.querySelector(`input[name="script"][value="${value}"]`);
  if (radio) radio.checked = true;
}

function updateCharCount() {
  const n = els.source.value.length;
  els.charCount.textContent = `${n} / ${MAX_CHARS}`;
  els.charCount.style.color = n > MAX_CHARS ? 'var(--red)' : '';
}

function showError(message) {
  els.errorBox.textContent = message;
  els.errorBox.hidden = !message;
}

function setLoading(on) {
  busy = on;
  els.translateBtn.disabled = on;
  els.translateBtn.classList.toggle('loading', on);
  els.translateBtn.querySelector('.btn-text').textContent = on ? 'Translating…' : 'Translate';
}

function showResult(data) {
  const isArabic = data.script === 'arabic';
  els.translation.textContent = data.translation;
  els.translation.dir = isArabic ? 'rtl' : 'ltr';
  els.detected.textContent = data.detectedLanguage && data.detectedLanguage !== 'unknown'
    ? `from ${data.detectedLanguage}` : 'translated';
  els.meta.textContent = `${data.model} · ${(data.durationMs / 1000).toFixed(1)} s`;
  els.resultCard.hidden = false;
}

function setServerDot(state, title) {
  els.serverDot.className = `dot ${state}`;
  els.serverDot.title = title;
}

// ---------- actions ----------

async function doTranslate() {
  if (busy) return;
  const text = els.source.value.trim();
  showError('');
  if (!text) {
    showError('Type or select some text first.');
    return;
  }
  if (text.length > MAX_CHARS) {
    showError(`Text is too long (${text.length} characters, maximum ${MAX_CHARS}).`);
    return;
  }

  setLoading(true);
  try {
    const script = selectedScript();
    const data = await translate(text, script, settings);
    showResult(data);
    setServerDot('up', 'Server reachable');
    if (script !== settings.script) {
      settings = await saveSettings({ ...settings, script });
    }
  } catch (err) {
    els.resultCard.hidden = true;
    showError(err.message);
    if (err.status === 0) setServerDot('down', 'Server unreachable');
  } finally {
    setLoading(false);
  }
}

async function copyTranslation() {
  try {
    await navigator.clipboard.writeText(els.translation.textContent);
    els.copyBtn.textContent = 'Copied ✓';
  } catch (_) {
    els.copyBtn.textContent = 'Copy failed';
  }
  setTimeout(() => { els.copyBtn.textContent = 'Copy'; }, 1500);
}

/** Pastes the text selected on the page (sent by background.js) and translates it. */
async function handlePendingSelection(pending) {
  if (!pending || !pending.text || pending.at <= lastHandledSelection) return;
  lastHandledSelection = pending.at;
  await chrome.storage.session.remove('pendingSelection');

  els.source.value = pending.text;
  updateCharCount();
  doTranslate();
}

async function checkServer() {
  try {
    const h = await health(settings);
    const keyNote = h.geminiConfigured ? '' : ' (warning: GEMINI_API_KEY not set on server)';
    setServerDot(h.geminiConfigured ? 'up' : 'down', `Server UP · model ${h.model}${keyNote}`);
    return `Connected ✓ · model ${h.model}${keyNote}`;
  } catch (_) {
    setServerDot('down', 'Server unreachable');
    return `Cannot reach ${settings.serverUrl}`;
  }
}

async function onSaveSettings() {
  const candidate = {
    serverUrl: els.serverUrl.value || DEFAULT_SETTINGS.serverUrl,
    username: els.username.value,
    password: els.password.value,
    script: selectedScript(),
  };

  // Hosts other than localhost:8080 need an extra permission (asked once, inside this click).
  try {
    const origin = new URL(candidate.serverUrl).origin;
    const granted = await chrome.permissions.request({ origins: [`${origin}/*`] });
    if (!granted) {
      els.settingsMsg.textContent = `Permission to access ${origin} was refused.`;
      return;
    }
  } catch (_) {
    els.settingsMsg.textContent = 'Invalid server URL.';
    return;
  }

  settings = await saveSettings(candidate);
  els.serverUrl.value = settings.serverUrl;
  els.settingsMsg.textContent = 'Saved. ' + await checkServer();
}

// ---------- init ----------

async function init() {
  settings = await loadSettings();
  els.serverUrl.value = settings.serverUrl;
  els.username.value = settings.username;
  els.password.value = settings.password;
  setScript(settings.script);
  updateCharCount();

  els.translateBtn.addEventListener('click', doTranslate);
  els.clearBtn.addEventListener('click', () => {
    els.source.value = '';
    updateCharCount();
    els.resultCard.hidden = true;
    showError('');
    els.source.focus();
  });
  els.copyBtn.addEventListener('click', copyTranslation);
  els.source.addEventListener('input', updateCharCount);
  els.source.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
      e.preventDefault();
      doTranslate();
    }
  });
  els.saveBtn.addEventListener('click', onSaveSettings);
  els.testBtn.addEventListener('click', async () => {
    els.settingsMsg.textContent = 'Testing…';
    els.settingsMsg.textContent = await checkServer();
  });

  // Selected text sent while the panel is already open...
  chrome.storage.onChanged.addListener((changes, area) => {
    if (area === 'session' && changes.pendingSelection) {
      handlePendingSelection(changes.pendingSelection.newValue);
    }
  });
  // ...or just before the panel was opened.
  const { pendingSelection } = await chrome.storage.session.get('pendingSelection');
  await handlePendingSelection(pendingSelection);

  checkServer();
}

init();
