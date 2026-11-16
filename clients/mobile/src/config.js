// Default connection settings (can also be changed in the app's Settings section).
//
// IMPORTANT: on a phone, "localhost" is the phone itself. Replace the IP below with the
// LAN IP address of the computer running the REST service (same Wi-Fi network):
//   Windows: ipconfig     (IPv4 Address, e.g. 192.168.1.20)
//   macOS:   ipconfig getifaddr en0
//   Linux:   hostname -I
// Android emulator: use http://10.0.2.2:8080/darija/api
export const DEFAULT_SETTINGS = {
  serverUrl: 'http://192.168.1.20:8080/darija/api',
  username: 'ali',
  password: 'darija2026',
};
