## Security Fix: Improve OAuth State Parameter Security

**Vulnerability:** The OAuth state parameter in `launchLogin` (in `RedditOAuthHelper.kt`) uses `System.currentTimeMillis()`:
`"&state=rdtube_auth_${System.currentTimeMillis()}"`
This is predictable and doesn't provide adequate protection against Cross-Site Request Forgery (CSRF) during the OAuth flow. A state parameter should ideally be cryptographically secure and hard to guess, and verified in the callback.

**Proposed Solution:**
1. Generate a cryptographically secure random string using `java.security.SecureRandom` and `UUID.randomUUID()` or `Base64` encoding.
2. Save this state in `SharedPreferences` before initiating the OAuth flow.
3. In `handleOAuthCallback`, extract the `state` from the callback URI, verify it against the saved state from `SharedPreferences`. If they don't match, abort the login process to prevent CSRF attacks.

This provides standard CSRF protection in OAuth 2.0 flows.
