// Small client for the Darija Translator REST service, used by the side panel.

export const DEFAULT_SETTINGS = {
  serverUrl: 'http://localhost:8080/darija/api',
  username: 'ali',
  password: 'darija2026',
  script: 'arabic',
};

export async function loadSettings() {
  const stored = await chrome.storage.local.get(Object.keys(DEFAULT_SETTINGS));
  return { ...DEFAULT_SETTINGS, ...stored };
}

export async function saveSettings(settings) {
  const clean = { ...settings, serverUrl: settings.serverUrl.trim().replace(/\/+$/, '') };
  await chrome.storage.local.set(clean);
  return clean;
}

/** Base64 of a UTF-8 string (btoa alone fails on non-Latin-1 characters). */
function base64(text) {
  const bytes = new TextEncoder().encode(text);
  let binary = '';
  bytes.forEach((b) => { binary += String.fromCharCode(b); });
  return btoa(binary);
}

export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

async function readError(response) {
  try {
    const body = await response.json();
    if (body && body.message) return body.message;
  } catch (_) { /* not JSON */ }
  return `HTTP ${response.status}`;
}

/** POST {serverUrl}/translator/translate with HTTP Basic auth. */
export async function translate(text, script, settings) {
  let response;
  try {
    response = await fetch(`${settings.serverUrl}/translator/translate`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
        Authorization: `Basic ${base64(`${settings.username}:${settings.password}`)}`,
      },
      body: JSON.stringify({ text, script }),
      // We send the Authorization header ourselves. 'omit' stops Chrome from showing its
      // native login pop-up when the server answers 401 (wrong password).
      credentials: 'omit',
      signal: AbortSignal.timeout(90_000),
    });
  } catch (err) {
    if (err.name === 'TimeoutError') throw new ApiError('The server took too long to answer.', 0);
    throw new ApiError(`Cannot reach the server at ${settings.serverUrl}. Is it running?`, 0);
  }

  if (response.status === 401) {
    throw new ApiError('Wrong username or password (check Settings).', 401);
  }
  if (!response.ok) {
    throw new ApiError(await readError(response), response.status);
  }
  return response.json();
}

/** GET {serverUrl}/health (public) to check that the server is reachable. */
export async function health(settings) {
  const response = await fetch(`${settings.serverUrl}/health`, {
    credentials: 'omit',
    signal: AbortSignal.timeout(8_000),
  });
  if (!response.ok) throw new ApiError(await readError(response), response.status);
  return response.json();
}
