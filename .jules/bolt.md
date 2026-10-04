## 2024-10-04 - Kotlin String.format Overhead
**Learning:** `String.format` is surprisingly slow in Android/Java environments for frequent operations (like playback updates) because it instantiates a `Formatter`, parses the format string, and creates temporary objects.
**Action:** Always prefer Kotlin's native string interpolation (e.g., `"$minutes:${if (seconds < 10) "0$seconds" else seconds}"`) in tight loops or rapidly updating UI components to reduce CPU cycles and GC pressure.
