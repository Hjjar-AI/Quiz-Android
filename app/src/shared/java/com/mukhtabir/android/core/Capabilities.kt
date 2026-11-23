package com.mukhtabir.android.core

import com.mukhtabir.android.data.remote.UserDto

/**
 * Capability helpers.
 *
 * The web app resolves capabilities on the server and ships the final
 * flat list on every UserDto. The Android client never re-derives the
 * set — it just asks the list. These helpers exist so every screen
 * calls the same three predicates and no screen invents its own
 * `capabilities.contains(...)` string comparison.
 *
 * Capability strings mirror `apps/users/capabilities.py` in the
 * backend. Add to this file only when a screen needs to gate a UI
 * surface — the canonical list lives on the server.
 */

/** Single capability check. */
fun UserDto.has(capability: String): Boolean =
    capability.isNotBlank() && capabilities.contains(capability)

/** Any-of check. Empty argument list returns false. */
fun UserDto.hasAny(vararg capabilities: String): Boolean =
    capabilities.any(::has)

/**
 * All-of check. An empty argument list returns true, matching
 * [kotlin.collections.all] semantics — a caller that passes nothing
 * is not gating anything.
 */
fun UserDto.hasAll(vararg capabilities: String): Boolean =
    capabilities.all(::has)

/** Convenience: null-safe check when the user may not have loaded yet. */
fun UserDto?.hasOrFalse(capability: String): Boolean =
    this?.has(capability) == true