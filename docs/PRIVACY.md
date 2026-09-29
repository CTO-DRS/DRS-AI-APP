# DRS AI — Privacy & data map

## Offline attestation

- Chat, RAG, vision and STT inference run entirely on device via llama.cpp /
  whisper.cpp. There is no code path that sends prompts, documents, images,
  audio or transcripts anywhere.
- The `INTERNET` permission exists for exactly one feature: the optional,
  user-initiated, one-time GGUF model download in the Model Center, gated by
  the Privacy Center kill-switch (disabled by default).
- No analytics, no crash reporting, no telemetry, no ads, no accounts.

## Where your data lives

| Data | Location | Removal |
|------|----------|---------|
| Chats & sessions | App-private Room DB (`drs_ai.db`) | Delete session / clear all in Settings |
| Memories | App-private Room DB | Memory tab or Settings |
| Models | `files/models/*.bin` (app-private) | Delete in Model Center |
| Document text + vectors | App-private Room DB | Delete document / clear all |
| Diagnostics export | `files/diagnostics/*.json` | Local file, you choose to share it |
| Session exports | Cache + system share sheet | You choose destination |

Uninstalling the app removes all of the above automatically.

## Network contracts per feature

| Feature | Network |
|---------|---------|
| Chat / RAG / Vision | never |
| STT (whisper) | never |
| TTS (system engine) | offline voices only |
| Model download | HTTPS, one-time, only if allowed in Privacy Center |
| Crash reports | none exist |

## Security

- App lock: PIN stored as PBKDF2-HmacSHA256 (150k iterations, 16-byte salt).
- No plaintext PIN ever touches storage.
