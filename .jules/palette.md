## 2024-05-18 - MinimalButton Accessibility
**Learning:** Custom clickable components without a specific Semantic Role assigned will not be announced correctly by screen readers. Applying `role = Role.Button` to the `clickable` modifier ensures it is recognized correctly as a button.
**Action:** Add `role = Role.Button` to the `clickable` modifier for custom interactive button components.
## 2024-05-18 - Redundant Icon Content Descriptions
**Learning:** Adding `contentDescription` to icons that are directly adjacent to `Text` components containing the same wording is an anti-pattern. Screen readers will announce the information twice (e.g., "Liked, Liked"), creating a noisy experience. Icons in this context should remain decorative (`contentDescription = null`).
**Action:** Verify if text alternatives exist near visual elements before assigning a `contentDescription` to avoid duplicate screen reader announcements.
## $(date +%Y-%m-%d) - Dynamic Content Descriptions
**Learning:** State-dependent buttons (like Mute/Unmute or Lock/Unlock) need dynamic `contentDescription`s to accurately reflect the action they perform when toggled, rather than statically describing the button's purpose. Screen readers use this context to clarify the action to the user.
**Action:** When working with toggleable state buttons, update `contentDescription` to be dynamic (e.g., `if (isMuted) "Unmute" else "Mute"`) instead of using a static string.
