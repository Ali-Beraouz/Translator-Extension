<?php
/**
 * PHP web client for the Darija Translator REST service.
 * Run:  php -S localhost:8000   (from this folder), then open http://localhost:8000
 *
 * Mini Project 2 - Ali Beraouz (138123)
 */
declare(strict_types=1);

require __DIR__ . '/DarijaTranslatorClient.php';
$config = require __DIR__ . '/config.php';

$client = new DarijaTranslatorClient($config['base_url'], $config['username'], $config['password'], $config['timeout']);

$text = '';
$script = 'arabic';
$result = null;
$error = null;

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $text = trim((string) ($_POST['text'] ?? ''));
    $script = ($_POST['script'] ?? 'arabic') === 'latin' ? 'latin' : 'arabic';

    if ($text === '') {
        $error = 'Please type some text to translate.';
    } else {
        try {
            $result = $client->translate($text, $script);
        } catch (DarijaTranslatorException $e) {
            $error = $e->getMessage();
        }
    }
}

function e(string $value): string
{
    return htmlspecialchars($value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Darija Translator · PHP client</title>
    <style>
        :root { --red: #c1272d; --green: #006233; --border: #e3ded6; }
        * { box-sizing: border-box; }
        body { margin: 0; font: 15px/1.5 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; background: #f6f4f1; color: #1f1d1a; }
        .wrap { max-width: 720px; margin: 40px auto; padding: 0 16px; }
        h1 { margin: 0 0 4px; font-size: 24px; }
        .sub { margin: 0 0 24px; color: #6f6a63; }
        .card { background: #fff; border: 1px solid var(--border); border-radius: 12px; padding: 20px; margin-bottom: 16px; }
        textarea { width: 100%; min-height: 140px; font: inherit; padding: 10px 12px; border: 1px solid var(--border); border-radius: 8px; resize: vertical; }
        .row { display: flex; gap: 16px; align-items: center; justify-content: space-between; margin-top: 12px; flex-wrap: wrap; }
        button { background: var(--red); color: #fff; border: 0; border-radius: 8px; padding: 10px 22px; font: inherit; font-weight: 600; cursor: pointer; }
        button:hover { background: #9e1f24; }
        .label { font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: .04em; color: #6f6a63; }
        .result { border-left: 4px solid var(--green); }
        .translation { font-size: 22px; line-height: 1.8; white-space: pre-wrap; margin: 8px 0; }
        .meta { color: #6f6a63; font-size: 13px; }
        .error { background: #fbeaea; border: 1px solid var(--red); border-radius: 12px; padding: 12px 16px; margin-bottom: 16px; }
        footer { color: #6f6a63; font-size: 12px; text-align: center; }
    </style>
</head>
<body>
<div class="wrap">
    <h1>Darija Translator</h1>
    <p class="sub">PHP client · any language → الدارجة المغربية · server: <?= e($config['base_url']) ?></p>

    <form method="post" class="card">
        <label for="text" class="label">Text to translate</label>
        <textarea id="text" name="text" dir="auto" maxlength="5000" required
                  placeholder="e.g. Hello, how are you? / Bonjour, ça va ?"><?= e($text) ?></textarea>
        <div class="row">
            <div>
                <span class="label">Script:</span>
                <label><input type="radio" name="script" value="arabic" <?= $script === 'arabic' ? 'checked' : '' ?>> العربية</label>
                <label><input type="radio" name="script" value="latin" <?= $script === 'latin' ? 'checked' : '' ?>> Latin (Arabizi)</label>
            </div>
            <button type="submit">Translate</button>
        </div>
    </form>

    <?php if ($error !== null): ?>
        <div class="error" role="alert"><?= e($error) ?></div>
    <?php endif; ?>

    <?php if ($result !== null): ?>
        <div class="card result">
            <span class="label">Darija</span>
            <p class="translation" dir="<?= $result['script'] === 'arabic' ? 'rtl' : 'ltr' ?>" lang="ary"><?= e($result['translation']) ?></p>
            <p class="meta">
                Detected language: <?= e((string) $result['detectedLanguage']) ?>
                · model <?= e((string) $result['model']) ?>
                · <?= number_format(((int) $result['durationMs']) / 1000, 1) ?> s
            </p>
        </div>
    <?php endif; ?>

    <footer>Mini Project 2 · Ali Beraouz (138123)</footer>
</div>
</body>
</html>
