## 2024-05-18 - MinimalButton Accessibility
**Learning:** Custom clickable components without a specific Semantic Role assigned will not be announced correctly by screen readers. Applying `role = Role.Button` to the `clickable` modifier ensures it is recognized correctly as a button.
**Action:** Add `role = Role.Button` to the `clickable` modifier for custom interactive button components.
## 2024-05-18 - Redundant Icon Content Descriptions
**Learning:** Adding `contentDescription` to icons that are directly adjacent to `Text` components containing the same wording is an anti-pattern. Screen readers will announce the information twice (e.g., "Liked, Liked"), creating a noisy experience. Icons in this context should remain decorative (`contentDescription = null`).
**Action:** Verify if text alternatives exist near visual elements before assigning a `contentDescription` to avoid duplicate screen reader announcements.
## 2024-05-19 - Dynamic Icon Content Descriptions
**Learning:** Providing dynamic `contentDescription` strings based on state (e.g., "Unlock rotation" / "Lock rotation") provides clearer context for screen reader users compared to static labels (e.g., "Rotation").
**Action:** Always verify if an icon button has multiple states and apply dynamic `contentDescription` to communicate current state and action clearly.
