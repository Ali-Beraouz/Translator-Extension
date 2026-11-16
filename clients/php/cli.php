<?php
/**
 * Command-line version of the PHP client.
 * Usage: php cli.php "Text to translate" [arabic|latin]
 */
declare(strict_types=1);

require __DIR__ . '/DarijaTranslatorClient.php';
$config = require __DIR__ . '/config.php';

if ($argc < 2) {
    fwrite(STDERR, "Usage: php cli.php \"Text to translate\" [arabic|latin]\n");
    exit(1);
}

$client = new DarijaTranslatorClient($config['base_url'], $config['username'], $config['password'], $config['timeout']);

try {
    $result = $client->translate($argv[1], $argv[2] ?? 'arabic');
    echo $result['translation'], PHP_EOL;
    fwrite(STDERR, sprintf("(detected: %s, model: %s, %.1f s)\n",
        $result['detectedLanguage'], $result['model'], $result['durationMs'] / 1000));
} catch (DarijaTranslatorException $e) {
    fwrite(STDERR, 'Error: ' . $e->getMessage() . PHP_EOL);
    exit(2);
}
