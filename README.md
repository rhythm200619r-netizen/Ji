# Ji – Micro-Commitments Reminder App

Ji is a privacy-first Android keyboard that detects time-based commitments in text you type ("call you tomorrow at 5", "kal shaam 5 baje bhejta hoon") and offers a one-tap reminder — entirely on-device.

## Non-Negotiable Rules

These rules apply to **every** task and contribution. No exceptions.

1. **No network.** Do NOT add `INTERNET` or `ACCESS_NETWORK_STATE` permissions. No analytics, crash reporters, ad SDKs, or any library that makes network calls.
2. **No Accessibility Service.** Ji reads only text typed through its own keyboard (`InputMethodService`) or text the user explicitly shares/selects to Ji.
3. **Never log, persist, or transmit typed text.** Typed text lives only in memory and is discarded right after detection. No typed text in logcat, files, caches, or crash dumps.
4. **Ignore sensitive fields.** Disable all detection when the input field is a password, OTP, email-address, or payment field (check `EditorInfo.inputType` and `IME_FLAG_NO_PERSONALIZED_LEARNING`). Also disable detection in incognito fields.
5. **Store minimum data only:** task snippet (max 120 chars), due time, category, status, `created_at`. Never the full message, sender, or app name.
6. **Encrypt local storage** (Room + SQLCipher; key held in Android Keystore).
7. **One-tap delete everything** must exist and truly wipe the database, preferences, and scheduled alarms.
8. **Confirm before create.** Default is a suggestion chip the user taps. Auto-create is an opt-in setting, off by default.
9. **Do not request permissions beyond those listed in SPEC.md.** If a new permission seems needed, STOP and ask.
10. **Precision over recall.** A wrong chip is worse than a missed one.

## Allowed Permissions (v1)

| Permission | Purpose |
|---|---|
| `POST_NOTIFICATIONS` | Show reminder notifications |
| `SCHEDULE_EXACT_ALARM` | Fire reminders at the exact promised time |
| `RECEIVE_BOOT_COMPLETED` | Reschedule alarms after device reboot |
| `VIBRATE` | Haptic feedback on notifications |

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (app), Jetpack Glance (widget)
- **Keyboard:** `InputMethodService`
- **Storage:** Room + SQLCipher (encrypted, key in Android Keystore)
- **Alarms:** `AlarmManager` (exact alarms) + `BroadcastReceiver` for boot
- **Detection:** Rule-based time parser (English, Hindi, Hinglish); ML classifier (TFLite, int8) planned for Phase 3
- **Min SDK:** 26 · **Target SDK:** latest stable

## Repo Layout

```
ji/
├── SPEC.md            # Full product specification
├── README.md          # This file
├── android/           # Kotlin app + keyboard service
├── ml/                # ML training pipeline
│   ├── data/          # Training data
│   └── export/        # Exported TFLite models
└── parser-tests/      # Time-phrase test cases (English, Hindi, Hinglish)
```

## Working Agreement

- Work on **one phase at a time**. Do not start the next phase unprompted.
- Write tests first for the parser, then the implementation.
- After any change to `AndroidManifest.xml`, list permissions and confirm they match the allowed list.
- Keep commits small, with one working step per commit.
- When unsure about product behavior, ask instead of guessing.

