# POS QRIS Android

Modern Android POS for retail and distribution, designed for phones and tablets.

## UI/UX baseline
- Kotlin + Jetpack Compose + Material 3
- Adaptive layouts for compact, medium and expanded screens
- Tablet-first two-pane POS experience when space allows
- Large touch targets for cashier workflows
- Explicit loading, empty, offline, payment-pending and error states
- Portrait and landscape support
- Accessibility-aware typography and spacing

## Architecture
Feature-oriented modules sit on top of shared auth, permissions, networking, local database and hardware layers.

```text
feature/
  auth/ dashboard/ pos/ products/ inventory/ customers/ suppliers/
  purchasing/ payments/ qris/ cashier/ reports/ settings/
core/
  auth/ permissions/ database/ network/ ui/ printer/
hardware/
  bluetooth/ usb/
```

## Authorization
The Android app uses server-provided authorization state to control navigation and actions. UI restrictions are not treated as security boundaries; Supabase RLS and server-side functions remain authoritative.

Owner-only administration includes user management, role assignment, branch configuration and sensitive store/payment settings.

## Printing
Receipt output is separated from business logic through `PrinterManager`, with adapters for Bluetooth and USB ESC/POS-compatible printers. Printer configuration includes paper width, auto-print, copies and test print.

## Configuration
Never commit Supabase service-role keys, provider secrets, webhook secrets or Android signing keys. Client configuration must contain only values intended for public client use.
