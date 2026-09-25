/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.music.privacy.incognito

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// MusicActivity.onOptionsItemSelected(MenuItem) - every menu item click that isn't consumed by
// the currently active tab fragment's own handler ends up here, regardless of which tab is
// showing. Targeted by class name (a required, unobfuscated Activity name) rather than by
// resource literals, since the toolbar's MenuItem.setOnMenuItemClickListener does not appear to
// be consulted by this app's own click dispatch.
// MusicActivity.onPrepareOptionsMenu(Menu) - runs after every (re)creation of the top bar menu,
// including page changes, so the Incognito icon's on/off state can be re-applied each time.
internal object MusicActivityOnPrepareOptionsMenuFingerprint : Fingerprint(
    definingClass = "/MusicActivity;",
    name = "onPrepareOptionsMenu",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Landroid/view/Menu;")
)

internal object MusicActivityOnOptionsItemSelectedFingerprint : Fingerprint(
    definingClass = "/MusicActivity;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Landroid/view/MenuItem;")
)
