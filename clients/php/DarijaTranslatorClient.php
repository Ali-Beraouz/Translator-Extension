<?php
/**
 * PHP client for the Darija Translator REST service (HTTP Basic authentication).
 * Uses the cURL extension when available, otherwise PHP streams.
 *
 * Mini Project 2 - Ali Beraouz (138123)
 */

class DarijaTranslatorException extends RuntimeException
{
}

class DarijaTranslatorClient
{
    private string $baseUrl;
    private string $username;
    private string $password;
    private int $timeout;

    public function __construct(string $baseUrl, string $username, string $password, int $timeout = 90)
    {
        $this->baseUrl = rtrim($baseUrl, '/');
        $this->username = $username;
        $this->password = $password;
        $this->timeout = $timeout;
    }

    /**
     * Calls POST /translator/translate.
     *
     * @param string $text   text in any language
     * @param string $script "arabic" or "latin"
     * @return array{sourceText:string, detectedLanguage:string, translation:string, script:string, model:string, durationMs:int}
     * @throws DarijaTranslatorException
     */
    public function translate(string $text, string $script = 'arabic'): array
    {
        $payload = json_encode(['text' => $text, 'script' => $script], JSON_UNESCAPED_UNICODE);
        [$status, $body] = $this->request('POST', '/translator/translate', $payload, true);

        if ($status === 401) {
            throw new DarijaTranslatorException('Wrong username or password.', 401);
        }
        $data = json_decode($body, true);
        if ($status !== 200) {
            $message = is_array($data) && isset($data['message']) ? $data['message'] : "HTTP $status";
            throw new DarijaTranslatorException($message, $status);
        }
        if (!is_array($data) || !isset($data['translation'])) {
            throw new DarijaTranslatorException('Unexpected response from the server.', $status);
        }
        return $data;
    }

    /** Calls the public GET /health endpoint. */
    public function health(): array
    {
        [$status, $body] = $this->request('GET', '/health', null, false);
        $data = json_decode($body, true);
        if ($status !== 200 || !is_array($data)) {
            throw new DarijaTranslatorException("Health check failed (HTTP $status)", $status);
        }
        return $data;
    }

    /** @return array{0:int, 1:string} HTTP status and body */
    private function request(string $method, string $path, ?string $body, bool $authenticate): array
    {
        $url = $this->baseUrl . $path;
        $headers = ['Accept: application/json'];
        if ($body !== null) {
            $headers[] = 'Content-Type: application/json; charset=UTF-8';
        }

        if (function_exists('curl_init')) {
            $ch = curl_init($url);
            curl_setopt_array($ch, [
                CURLOPT_CUSTOMREQUEST => $method,
                CURLOPT_RETURNTRANSFER => true,
                CURLOPT_HTTPHEADER => $headers,
                CURLOPT_TIMEOUT => $this->timeout,
                CURLOPT_CONNECTTIMEOUT => 10,
            ]);
            if ($authenticate) {
                curl_setopt($ch, CURLOPT_HTTPAUTH, CURLAUTH_BASIC);
                curl_setopt($ch, CURLOPT_USERPWD, $this->username . ':' . $this->password);
            }
            if ($body !== null) {
                curl_setopt($ch, CURLOPT_POSTFIELDS, $body);
            }
            $response = curl_exec($ch);
            if ($response === false) {
                $error = curl_error($ch);
                throw new DarijaTranslatorException("Cannot reach the server at {$this->baseUrl} ($error)", 0);
            }
            $status = (int) curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
            return [$status, (string) $response];
        }

        // Fallback without the cURL extension
        if ($authenticate) {
            $headers[] = 'Authorization: Basic ' . base64_encode($this->username . ':' . $this->password);
        }
        $context = stream_context_create(['http' => [
            'method' => $method,
            'header' => implode("\r\n", $headers),
            'content' => $body ?? '',
            'timeout' => $this->timeout,
            'ignore_errors' => true,
        ]]);
        $response = @file_get_contents($url, false, $context);
        if ($response === false) {
            throw new DarijaTranslatorException("Cannot reach the server at {$this->baseUrl}", 0);
        }
        $status = 0;
        foreach ($http_response_header ?? [] as $line) {
            if (preg_match('#^HTTP/\S+\s+(\d{3})#', $line, $m)) {
                $status = (int) $m[1];
            }
        }
        return [$status, $response];
    }
}
