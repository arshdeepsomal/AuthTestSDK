# Architectural recommendations

## Decouple construction from business logic
- **Inject dependencies instead of constructing them internally.** `AuthManager` currently instantiates both the delegate and `SessionManager`, which makes testing and reuse harder outside of Android entry points. Accept `AuthApi` and `SessionManager` (or factories) as constructor parameters and let callers supply their own instances. This also allows JVM tests to provide lightweight session stores without Android context needs.
- **Make clients configurable in `ONEAuthDelegate`.** The delegate directly creates `OneAuthClient` and `TwoAuthClient` and owns a hard-coded IO scope. Accept these dependencies (and a `CoroutineScope`) via the constructor so the delegate focuses on flow orchestration while callers control threading and client setup.

## Clarify session persistence boundaries
- **Separate session storage behind an interface.** `SessionManager` eagerly builds a concrete `Session` from the Android-backed provider. Introduce a `SessionStore` abstraction so production can use the encrypted DataStore while tests or other platforms can swap in in-memory or file-based stores without touching auth flows.
- **Surface explicit session lifecycle events.** Instead of toggling `sessionState` directly from the manager, emit events when saving, clearing, or expiring sessions. This enables observers to react consistently (e.g., revoke tokens, clear caches) without each caller wiring its own flags.

## Harden long-running coroutines
- **Use lifecycle-aware scopes.** The delegate launches coroutines on a standalone IO scope that never cancels, which can leak work after UI teardown. Accept a scope from the caller (e.g., ViewModel or application scope) to align coroutine lifetimes with the owner.
- **Model suspend entry points.** Converting `login`, `register`, and receipt submission to suspend functions would allow callers to decide how to launch them and simplify error propagation; the delegate can still expose a convenience wrapper for UI code if needed.

## Consider module boundaries
- **Split platform-neutral core from Android wiring.** Isolate models, session/domain interfaces, and network clients into a pure Kotlin module, keeping only intent-building and AppAuth bindings in the Android layer. This reduces test flakiness around keystore and AppAuth initialization while keeping runtime behavior the same.
