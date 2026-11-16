#!/usr/bin/env python3
"""
Python client for the Darija Translator REST service.

Mini Project 2 - Ali Beraouz (138123)

Usage:
    python darija_client.py "Hello, how are you?"          # one translation
    python darija_client.py "Bonjour" --script latin       # Darija in Latin letters
    echo "Good morning" | python darija_client.py          # read from stdin
    python darija_client.py                                # interactive mode

Settings (command-line options or environment variables):
    --url       DARIJA_API_URL   (default http://localhost:8080/darija/api)
    --user      DARIJA_USERNAME  (default ali)
    --password  DARIJA_PASSWORD  (default darija2026)
"""
from __future__ import annotations

import argparse
import os
import sys
from dataclasses import dataclass

import requests

DEFAULT_URL = "http://localhost:8080/darija/api"


class TranslatorError(Exception):
    """Raised when the server cannot translate the text."""

    def __init__(self, message: str, status: int = 0):
        super().__init__(message)
        self.status = status


@dataclass
class Translation:
    source_text: str
    detected_language: str
    translation: str
    script: str
    model: str
    duration_ms: int


class DarijaTranslatorClient:
    """Calls the TranslatorResource endpoint with HTTP Basic authentication."""

    def __init__(self, base_url: str, username: str, password: str, timeout: float = 90):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.session = requests.Session()
        self.session.auth = (username, password)
        self.session.headers.update({"Accept": "application/json"})

    def translate(self, text: str, script: str = "arabic") -> Translation:
        """POST /translator/translate and return the Darija translation."""
        try:
            response = self.session.post(
                f"{self.base_url}/translator/translate",
                json={"text": text, "script": script},
                timeout=self.timeout,
            )
        except requests.Timeout as exc:
            raise TranslatorError("The server took too long to answer.") from exc
        except requests.ConnectionError as exc:
            raise TranslatorError(f"Cannot reach the server at {self.base_url}. Is it running?") from exc

        if response.status_code == 401:
            raise TranslatorError("Wrong username or password.", 401)
        if response.status_code != 200:
            raise TranslatorError(_error_message(response), response.status_code)

        data = response.json()
        return Translation(
            source_text=data.get("sourceText", text),
            detected_language=data.get("detectedLanguage", "unknown"),
            translation=data["translation"],
            script=data.get("script", script),
            model=data.get("model", ""),
            duration_ms=int(data.get("durationMs", 0)),
        )

    def health(self) -> dict:
        """GET /health (public, no authentication)."""
        response = requests.get(f"{self.base_url}/health", timeout=10)
        response.raise_for_status()
        return response.json()


def _error_message(response: requests.Response) -> str:
    try:
        return response.json().get("message") or f"HTTP {response.status_code}"
    except ValueError:
        return f"HTTP {response.status_code}"


def _print_translation(result: Translation) -> None:
    print(result.translation)
    print(
        f"  (detected: {result.detected_language} | model: {result.model} | "
        f"{result.duration_ms / 1000:.1f} s)",
        file=sys.stderr,
    )


def _interactive(client: DarijaTranslatorClient, script: str) -> None:
    print("Darija Translator - type a sentence and press Enter.")
    print("Commands: :latin  :arabic  :quit\n")
    while True:
        try:
            line = input(f"[{script}] > ").strip()
        except (EOFError, KeyboardInterrupt):
            print()
            return
        if not line:
            continue
        if line in (":q", ":quit", ":exit"):
            return
        if line in (":latin", ":arabic"):
            script = line[1:]
            print(f"Script set to {script}.")
            continue
        try:
            _print_translation(client.translate(line, script))
        except TranslatorError as exc:
            print(f"Error: {exc}", file=sys.stderr)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Translate text to Moroccan Darija.")
    parser.add_argument("text", nargs="?", help="text to translate (omit for interactive mode)")
    parser.add_argument("--script", choices=["arabic", "latin"], default="arabic",
                        help="write the Darija in Arabic script (default) or Latin letters")
    parser.add_argument("--url", default=os.getenv("DARIJA_API_URL", DEFAULT_URL), help="API base URL")
    parser.add_argument("--user", default=os.getenv("DARIJA_USERNAME", "ali"), help="Basic auth username")
    parser.add_argument("--password", default=os.getenv("DARIJA_PASSWORD", "darija2026"),
                        help="Basic auth password")
    args = parser.parse_args(argv)

    # Arabic output on Windows consoles
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    client = DarijaTranslatorClient(args.url, args.user, args.password)

    text = args.text
    if text is None and not sys.stdin.isatty():
        text = sys.stdin.read().strip()

    if not text:
        _interactive(client, args.script)
        return 0

    try:
        _print_translation(client.translate(text, args.script))
        return 0
    except TranslatorError as exc:
        print(f"Error: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
