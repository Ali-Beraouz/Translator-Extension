<?php
// Connection settings for the Darija Translator REST service.
// Environment variables (DARIJA_API_URL, DARIJA_USERNAME, DARIJA_PASSWORD) override these defaults.

return [
    'base_url' => getenv('DARIJA_API_URL') ?: 'http://localhost:8080/darija/api',
    'username' => getenv('DARIJA_USERNAME') ?: 'ali',
    'password' => getenv('DARIJA_PASSWORD') ?: 'darija2026',
    'timeout'  => 90, // seconds
];
