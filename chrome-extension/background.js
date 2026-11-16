// Darija Translator - background service worker (Manifest V3)
// Ali Beraouz (138123)
//
// 1. Adds a "Translate to Darija" item to the right-click menu when text is selected.
// 2. On click: opens the side panel and hands it the selected text through
//    chrome.storage.session. The side panel pastes it and translates it automatically.
// 3. Clicking the toolbar icon also opens the side panel.

const MENU_ID = 'translate-to-darija';

chrome.runtime.onInstalled.addListener(() => {
  chrome.contextMenus.removeAll(() => {
    chrome.contextMenus.create({
      id: MENU_ID,
      title: 'Translate "%s" to Darija',
      contexts: ['selection'],
    });
  });

  chrome.sidePanel
    .setPanelBehavior({ openPanelOnActionClick: true })
    .catch((err) => console.error('setPanelBehavior failed:', err));
});

chrome.contextMenus.onClicked.addListener((info, tab) => {
  if (info.menuItemId !== MENU_ID || !tab) return;

  // sidePanel.open() must run synchronously inside the user gesture (before any await).
  chrome.sidePanel
    .open({ windowId: tab.windowId })
    .catch((err) => console.error('sidePanel.open failed:', err));

  deliverSelection(info, tab);
});

/**
 * Sends the selected text to the side panel.
 * info.selectionText collapses line breaks, so we first try to read the exact selection
 * from the page (allowed by activeTab after a context-menu click). Pages such as
 * chrome:// or the Web Store block scripts; then we fall back to info.selectionText.
 */
async function deliverSelection(info, tab) {
  let text = info.selectionText || '';

  try {
    const target = { tabId: tab.id };
    if (typeof info.frameId === 'number') target.frameIds = [info.frameId];

    const [injection] = await chrome.scripting.executeScript({
      target,
      func: () => (window.getSelection ? window.getSelection().toString() : ''),
    });
    if (injection && typeof injection.result === 'string' && injection.result.trim()) {
      text = injection.result;
    }
  } catch (err) {
    // Not allowed on this page: keep info.selectionText.
  }

  await chrome.storage.session.set({
    pendingSelection: { text: text.trim(), at: Date.now() },
  });
}
