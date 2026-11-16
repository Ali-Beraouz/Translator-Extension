// Client for the Darija Translator REST service (HTTP Basic authentication).

const BASE64_CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';

/** Base64 of a UTF-8 string (no dependency on btoa). */
export function base64(text) {
  const bytes = utf8Bytes(text);
  let out = '';
  for (let i = 0; i < bytes.length; i += 3) {
    const b0 = bytes[i];
    const b1 = i + 1 < bytes.length ? bytes[i + 1] : 0;
    const b2 = i + 2 < bytes.length ? bytes[i + 2] : 0;
    const n = (b0 << 16) | (b1 << 8) | b2;
    out += BASE64_CHARS[(n >> 18) & 63] + BASE64_CHARS[(n >> 12) & 63];
    out += i + 1 < bytes.length ? BASE64_CHARS[(n >> 6) & 63] : '=';
    out += i + 2 < bytes.length ? BASE64_CHARS[n & 63] : '=';
  }
  return out;
}

function utf8Bytes(text) {
  const bytes = [];
  for (const ch of text) {
    const cp = ch.codePointAt(0);
    if (cp < 0x80) bytes.push(cp);
    else if (cp < 0x800) bytes.push(0xc0 | (cp >> 6), 0x80 | (cp & 63));
    else if (cp < 0x10000) bytes.push(0xe0 | (cp >> 12), 0x80 | ((cp >> 6) & 63), 0x80 | (cp & 63));
    else bytes.push(0xf0 | (cp >> 18), 0x80 | ((cp >> 12) & 63), 0x80 | ((cp >> 6) & 63), 0x80 | (cp & 63));
  }
  return bytes;
}

export class ApiError extends Error {
  constructor(message, status = 0) {
    super(message);
    this.status = status;
  }
}

async function fetchWithTimeout(url, options, ms) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), ms);
  try {
    return await fetch(url, { ...options, signal: controller.signal });
  } catch (err) {
    if (err.name === 'AbortError') throw new ApiError('The server took too long to answer.');
    throw new ApiError(`Cannot reach ${url.split('/darija')[0]}. Check the server URL and that the phone is on the same Wi-Fi.`);
  } finally {
    clearTimeout(timer);
  }
}

async function readError(response) {
  try {
    const body = await response.json();
    if (body && body.message) return body.message;
  } catch (_) {
    // not JSON
  }
  return `HTTP ${response.status}`;
}

function cleanUrl(url) {
  return url.trim().replace(/\/+$/, '');
}

/** POST {serverUrl}/translator/translate */
export async function translate(text, script, settings) {
  const response = await fetchWithTimeout(
    `${cleanUrl(settings.serverUrl)}/translator/translate`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
        Authorization: `Basic ${base64(`${settings.username}:${settings.password}`)}`,
      },
      body: JSON.stringify({ text, script }),
    },
    90000,
  );
  if (response.status === 401) throw new ApiError('Wrong username or password (see Settings).', 401);
  if (!response.ok) throw new ApiError(await readError(response), response.status);
  return response.json();
}

/** GET {serverUrl}/health (public) */
export async function health(settings) {
  const response = await fetchWithTimeout(`${cleanUrl(settings.serverUrl)}/health`, {}, 8000);
  if (!response.ok) throw new ApiError(await readError(response), response.status);
  return response.json();
}
