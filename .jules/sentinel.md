## 2024-05-27 - Implement CSRF Protection for OAuth
**Vulnerability:** OAuth state parameter used a predictable `System.currentTimeMillis()` value, vulnerable to CSRF attacks.
**Learning:** Always use a cryptographically secure random value for OAuth state parameter and persist it across process boundaries using shared preferences or similar mechanisms to verify during the callback.
**Prevention:** Follow OAuth 2.0 security guidelines and utilize `java.security.SecureRandom` or UUIDs to generate state parameters, ensuring they are verified in the callback process.
