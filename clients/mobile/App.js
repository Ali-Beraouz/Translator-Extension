// Darija Translator - React Native (Expo) client
// Mini Project 2 - Ali Beraouz (138123)

import { useState } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  Share,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import { DEFAULT_SETTINGS } from './src/config';
import { health, translate } from './src/api';

const MAX_CHARS = 5000;
const COLORS = {
  red: '#c1272d',
  green: '#006233',
  bg: '#f6f4f1',
  card: '#ffffff',
  text: '#1f1d1a',
  muted: '#6f6a63',
  border: '#e3ded6',
};

export default function App() {
  const [text, setText] = useState('');
  const [script, setScript] = useState('arabic');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const [showSettings, setShowSettings] = useState(false);
  const [settings, setSettings] = useState(DEFAULT_SETTINGS);
  const [settingsMsg, setSettingsMsg] = useState('');

  async function onTranslate() {
    const trimmed = text.trim();
    setError('');
    if (!trimmed) {
      setError('Type some text first.');
      return;
    }
    if (trimmed.length > MAX_CHARS) {
      setError(`Text is too long (maximum ${MAX_CHARS} characters).`);
      return;
    }
    setLoading(true);
    try {
      setResult(await translate(trimmed, script, settings));
    } catch (err) {
      setResult(null);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function onShare() {
    if (result) {
      await Share.share({ message: result.translation });
    }
  }

  async function onTestConnection() {
    setSettingsMsg('Testing…');
    try {
      const h = await health(settings);
      setSettingsMsg(`Connected ✓ · model ${h.model}${h.geminiConfigured ? '' : ' (GEMINI_API_KEY missing on server)'}`);
    } catch (err) {
      setSettingsMsg(err.message);
    }
  }

  const isArabic = result && result.script === 'arabic';

  return (
    <View style={styles.screen}>
      <StatusBar barStyle="dark-content" />
      <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView
          contentContainerStyle={styles.content}
          contentInsetAdjustmentBehavior="automatic"
          keyboardShouldPersistTaps="handled"
        >
          <View style={styles.header}>
            <View style={styles.logo}>
              <Text style={styles.logoText}>د</Text>
            </View>
            <View>
              <Text style={styles.title}>Darija Translator</Text>
              <Text style={styles.subtitle}>Any language → الدارجة المغربية</Text>
            </View>
          </View>

          <View style={styles.card}>
            <Text style={styles.label}>TEXT TO TRANSLATE</Text>
            <TextInput
              style={styles.input}
              multiline
              value={text}
              onChangeText={setText}
              placeholder="e.g. Hello, how are you? / Bonjour, ça va ?"
              placeholderTextColor={COLORS.muted}
              textAlignVertical="top"
              maxLength={MAX_CHARS}
            />
            <View style={styles.rowBetween}>
              <Text style={styles.muted}>{text.length} / {MAX_CHARS}</Text>
              <View style={styles.segmented}>
                {[
                  ['arabic', 'العربية'],
                  ['latin', 'Latin'],
                ].map(([value, label]) => (
                  <Pressable
                    key={value}
                    onPress={() => setScript(value)}
                    style={[styles.segment, script === value && styles.segmentActive]}
                    accessibilityRole="radio"
                    accessibilityState={{ selected: script === value }}
                  >
                    <Text style={[styles.segmentText, script === value && styles.segmentTextActive]}>{label}</Text>
                  </Pressable>
                ))}
              </View>
            </View>
            <View style={styles.row}>
              <Pressable
                onPress={onTranslate}
                disabled={loading}
                style={({ pressed }) => [styles.button, styles.primary, (pressed || loading) && styles.pressed]}
              >
                {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.primaryText}>Translate</Text>}
              </Pressable>
              <Pressable
                onPress={() => {
                  setText('');
                  setResult(null);
                  setError('');
                }}
                style={({ pressed }) => [styles.button, pressed && styles.pressed]}
              >
                <Text style={styles.buttonText}>Clear</Text>
              </Pressable>
            </View>
          </View>

          {error ? (
            <View style={styles.error}>
              <Text style={styles.errorText}>{error}</Text>
            </View>
          ) : null}

          {result ? (
            <View style={[styles.card, styles.result]}>
              <View style={styles.rowBetween}>
                <Text style={styles.label}>DARIJA</Text>
                <Text style={styles.chip}>
                  {result.detectedLanguage && result.detectedLanguage !== 'unknown'
                    ? `from ${result.detectedLanguage}`
                    : 'translated'}
                </Text>
              </View>
              <Text
                selectable
                style={[styles.translation, isArabic && styles.translationRtl]}
              >
                {result.translation}
              </Text>
              <View style={styles.rowBetween}>
                <Text style={styles.muted}>
                  {result.model} · {(result.durationMs / 1000).toFixed(1)} s
                </Text>
                <Pressable onPress={onShare} style={({ pressed }) => [styles.button, pressed && styles.pressed]}>
                  <Text style={styles.buttonText}>Share / Copy</Text>
                </Pressable>
              </View>
            </View>
          ) : null}

          <View style={styles.card}>
            <Pressable onPress={() => setShowSettings(!showSettings)}>
              <Text style={styles.settingsTitle}>{showSettings ? '▾' : '▸'} Settings</Text>
            </Pressable>
            {showSettings ? (
              <View style={styles.settingsBody}>
                <Text style={styles.label}>SERVER URL</Text>
                <TextInput
                  style={styles.field}
                  value={settings.serverUrl}
                  onChangeText={(v) => setSettings({ ...settings, serverUrl: v })}
                  autoCapitalize="none"
                  autoCorrect={false}
                  keyboardType="url"
                />
                <Text style={styles.label}>USERNAME</Text>
                <TextInput
                  style={styles.field}
                  value={settings.username}
                  onChangeText={(v) => setSettings({ ...settings, username: v })}
                  autoCapitalize="none"
                  autoCorrect={false}
                />
                <Text style={styles.label}>PASSWORD</Text>
                <TextInput
                  style={styles.field}
                  value={settings.password}
                  onChangeText={(v) => setSettings({ ...settings, password: v })}
                  secureTextEntry
                  autoCapitalize="none"
                />
                <Pressable onPress={onTestConnection} style={({ pressed }) => [styles.button, pressed && styles.pressed]}>
                  <Text style={styles.buttonText}>Test connection</Text>
                </Pressable>
                {settingsMsg ? <Text style={styles.muted}>{settingsMsg}</Text> : null}
              </View>
            ) : null}
          </View>

          <Text style={styles.footer}>Mini Project 2 · Ali Beraouz (138123)</Text>
        </ScrollView>
      </KeyboardAvoidingView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: COLORS.bg,
    // Android draws edge-to-edge: keep content below the status bar
    paddingTop: Platform.OS === 'android' ? StatusBar.currentHeight : 0,
  },
  flex: { flex: 1 },
  content: { padding: 16, gap: 12 },
  header: { flexDirection: 'row', alignItems: 'center', gap: 12, marginBottom: 4 },
  logo: {
    width: 44,
    height: 44,
    borderRadius: 10,
    backgroundColor: COLORS.red,
    alignItems: 'center',
    justifyContent: 'center',
  },
  logoText: { color: '#fff', fontSize: 26, fontWeight: '700' },
  title: { fontSize: 20, fontWeight: '700', color: COLORS.text },
  subtitle: { fontSize: 13, color: COLORS.muted },
  card: {
    backgroundColor: COLORS.card,
    borderColor: COLORS.border,
    borderWidth: 1,
    borderRadius: 12,
    padding: 14,
    gap: 10,
  },
  label: { fontSize: 12, fontWeight: '600', color: COLORS.muted, letterSpacing: 0.5 },
  muted: { fontSize: 12, color: COLORS.muted },
  input: {
    minHeight: 120,
    borderWidth: 1,
    borderColor: COLORS.border,
    borderRadius: 8,
    padding: 10,
    fontSize: 16,
    color: COLORS.text,
  },
  field: {
    borderWidth: 1,
    borderColor: COLORS.border,
    borderRadius: 8,
    paddingHorizontal: 10,
    paddingVertical: 8,
    fontSize: 15,
    color: COLORS.text,
  },
  row: { flexDirection: 'row', gap: 8, alignItems: 'center' },
  rowBetween: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  segmented: {
    flexDirection: 'row',
    borderWidth: 1,
    borderColor: COLORS.border,
    borderRadius: 8,
    overflow: 'hidden',
  },
  segment: { paddingHorizontal: 12, paddingVertical: 5 },
  segmentActive: { backgroundColor: COLORS.green },
  segmentText: { fontSize: 13, color: COLORS.text },
  segmentTextActive: { color: '#fff' },
  button: {
    borderWidth: 1,
    borderColor: COLORS.border,
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 9,
    alignItems: 'center',
    justifyContent: 'center',
    alignSelf: 'flex-start',
  },
  buttonText: { color: COLORS.text, fontSize: 14 },
  primary: { backgroundColor: COLORS.red, borderColor: COLORS.red, minWidth: 110 },
  primaryText: { color: '#fff', fontWeight: '700', fontSize: 15 },
  pressed: { opacity: 0.7 },
  error: {
    backgroundColor: '#fbeaea',
    borderColor: COLORS.red,
    borderWidth: 1,
    borderRadius: 12,
    padding: 12,
  },
  errorText: { color: COLORS.text },
  result: { borderLeftWidth: 4, borderLeftColor: COLORS.green },
  chip: {
    fontSize: 11,
    color: COLORS.green,
    backgroundColor: '#e1efe7',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 999,
    overflow: 'hidden',
  },
  translation: { fontSize: 20, lineHeight: 32, color: COLORS.text },
  translationRtl: { fontSize: 22, textAlign: 'right', writingDirection: 'rtl' },
  settingsTitle: { fontWeight: '600', fontSize: 15, color: COLORS.text },
  settingsBody: { gap: 8, marginTop: 4 },
  footer: { textAlign: 'center', color: COLORS.muted, fontSize: 11, marginTop: 4 },
});
