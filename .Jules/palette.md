## 2024-10-04 - Dynamic Content Descriptions & Button Roles in Compose
**Learning:** Found that custom composable buttons using `.clickable` need an explicit `role = Role.Button` so screen readers identify them as interactive elements. Additionally, toggle buttons often use static descriptions (e.g., "Mute") regardless of state, which is confusing for a11y users.
**Action:** Always verify `role = Role.Button` is set on custom `.clickable` components and ensure stateful buttons have dynamic `contentDescription`s (e.g., "Unmute" vs "Mute") reflecting the action to be performed.
