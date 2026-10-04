## 2024-10-04 - SharedPreferences commit() for large JSON payloads
**Learning:** The codebase was using synchronous `.commit()` calls to save large serialized JSON strings (watched and liked post histories of up to 1000 items) on the main thread, causing UI jank.
**Action:** Use `.apply()` for asynchronous writes when saving history items to SharedPreferences to prevent main thread blocking.
