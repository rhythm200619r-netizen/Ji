# Ji: Micro-Commitments Reminder App (Android v1)

## One-line product
Ji is a privacy-first Android keyboard that notices time-based commitments in text the user types ("call you tomorrow at 5", "kal shaam 5 baje bhejta hoon") and offers a one-tap reminder, entirely on-device.

## NON-NEGOTIABLE RULES (the agent must follow these in every task)
1. **No network.** Do NOT add `INTERNET` or `ACCESS_NETWORK_STATE` permissions. No analytics, crash reporters, ad SDKs, or any library that makes network calls.
2. **No Accessibility Service.** Ji reads only text typed through its own keyboard (InputMethodService) or text the user explicitly shares/selects to Ji.
3. **Never log, persist, or transmit typed text.** Typed text lives only in memory and is discarded right after detection. No typed text in logcat, files, caches, or crash dumps.
4. **Ignore sensitive fields.** Disable all detection when the input field is a password, OTP, email-address, or payment field (check `EditorInfo.inputType` and `IME_FLAG_NO_PERSONALIZED_LEARNING`). Also disable detection in incognito fields.
5. **Store minimum data only:** task snippet (max 120 chars), due time, category, status, created_at. Never the full message, sender, or app name.
6. **Encrypt local storage** (Room + SQLCipher; key held in Android Keystore).
7. **One-tap delete everything** must exist and truly wipe the database, preferences and scheduled alarms.
8. **Confirm before create.** Default is a suggestion chip the user taps. Auto-create is an opt-in setting, off by default.
9. **Do not request permissions beyond those listed in this file.** If a new permission seems needed, STOP and ask.
10. **Precision over recall.** A wrong chip is worse than a missed one.

## Allowed permissions (v1)
- `POST_NOTIFICATIONS`
- `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM` (confirm Play policy eligibility before release)
- `RECEIVE_BOOT_COMPLETED`
- `VIBRATE`

## Tech stack
- Kotlin, Jetpack Compose (app UI), Jetpack Glance (widget)
- `InputMethodService` for the keyboard
- Room + SQLCipher
- AlarmManager (exact alarms) + BroadcastReceiver for boot
- Detection: rule-based time parser (English, Hindi, Hinglish) first; ML classifier added in phase 3 (TFLite, int8)
- Min SDK 26, target latest stable SDK
- Unit tests: JUnit; parser must have a table-driven test file

## v1 features
1. Ji keyboard with a standard QWERTY layout and a suggestion chip above the keys
2. Time-phrase detection: "at 5", "tomorrow morning", "in 2 hours", "kal shaam 5 baje", "parso", "next Monday"
3. Commitment check: "I'll send it by 5" = yes; "the meeting is at 5" = no (rules first, ML later)
4. Chip: "Remind: <snippet>, <time>?" with accept / dismiss
5. Category tag: Professional, Personal, Parcel, Urgent (keyword rules first)
6. Screens: Today, Upcoming, Done, Add manually (pop-up), Settings/Privacy
7. Share-to-Ji and text-selection action as a fallback to the keyboard
8. Home-screen widget: Today / Urgent / Upcoming
9. Onboarding step that guides the user to whitelist Ji from battery optimization
10. Privacy screen: lists exactly what is stored, plus Delete everything

## Out of scope for v1
Reading incoming messages or notifications, cloud sync, accounts, workspace integration, AI duration estimates (planned v2, opt-in), iOS.

## Design
Clean and minimal. Category colors: Professional = blue, Personal = green, Parcel = amber, Urgent = red. Support light and dark mode. Large tap targets. No ads.

## Repo layout
```
ji/
├── SPEC.md
├── android/          # Kotlin app + keyboard service
├── ml/               # Dockerfile, notebooks, training scripts, exported TFLite
│   ├── data/
│   └── export/
└── parser-tests/     # time-phrase test cases (English, Hindi, Hinglish)
```

## Working agreement for the agent
- Work on ONE phase at a time. Do not start the next phase unprompted.
- Write tests first for the parser, then the implementation.
- After any change to `AndroidManifest.xml`, list the permissions and confirm they match the allowed list above.
- Keep commits small, with one working step per commit.
- When unsure about product behavior, ask instead of guessing.

---

## PHASE PROMPTS (paste one at a time into Antigravity)

### Phase 0: Setup
> Read SPEC.md fully. Create the repo layout described there. Create a new Android project in `android/` with Kotlin, Jetpack Compose, min SDK 26, package `app.ji`. Set up the manifest with only the allowed permissions. Add a README that summarizes the non-negotiable rules. Do not implement any features yet. Show me the final manifest.

### Phase 1a: Time parser (tests first)
> Following SPEC.md, create a pure-Kotlin `TimePhraseParser` module with no Android dependencies. First write a table-driven test file in `parser-tests/` with at least 150 cases covering English, Hindi (Devanagari) and Hinglish (romanized) phrases, such as "at 5", "5 baje", "kal shaam", "tomorrow morning", "in 2 hours", "2 ghante mein", "next Monday", "parso subah". Each case gives the input, the reference time, and the expected resolved date-time or null. Show me the test file for review BEFORE writing the parser.

Then:
> Implement the parser so all tests pass. Default assumptions: morning = 9:00, afternoon = 2:00 pm, evening = 6:00, night = 9:00. Report which cases fail and why.

### Phase 1b: Keyboard and chip
> Implement a minimal `InputMethodService` keyboard with a QWERTY layout, a number row, and a suggestion strip above the keys. On each text change, pass the current sentence to `TimePhraseParser`. If a time is found, show a chip "Remind: <snippet>, <time>?". Follow rule 4 in SPEC.md: disable all detection for password, OTP, email and payment fields. Do not store or log the typed text anywhere. Add an onboarding screen that helps the user enable and select the keyboard.

### Phase 2: Reminders core
> Add Room with SQLCipher for reminders (snippet, due time, category, status, created_at). Tapping the chip saves a reminder and schedules an exact alarm. Add the notification, a boot receiver that reschedules alarms, and a Today screen in Compose. Add the manual-add pop-up. Follow SPEC.md data rules (snippet max 120 chars).

### Phase 3: ML classifier
> See the `ml/` plan: train a small text classifier (commitment vs not, plus category), export to int8 TFLite under 15 MB, and integrate it behind an interface so the rule-based fallback still works. Target precision of 90% or more on the held-out test set.

### Phase 4: Polish and privacy
> Add the widget, share-to-Ji action, category colors, the Privacy screen with Delete everything, and battery-optimization guidance. Verify the manifest still matches the allowed permissions.
