/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.music.privacy.incognito

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.music.misc.extension.sharedExtensionPatch
import app.morphe.patches.music.misc.settings.PreferenceScreen
import app.morphe.patches.music.misc.settings.settingsPatch
import app.morphe.patches.music.shared.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.shared.misc.request.buildRequestPatch
import app.morphe.patches.shared.misc.request.hookBuildRequest
import app.morphe.patches.shared.misc.request.hookBuildRequestUrl
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.ResourceGroup
import app.morphe.util.cloneParameters
import app.morphe.util.copyResources

private const val EXTENSION_CLASS = "Lapp/morphe/extension/music/patches/privacy/IncognitoModePatch;"

private const val INCOGNITO_MODE_PREFERENCE_TAG =
    "app.morphe.extension.music.settings.preference.IncognitoModeSwitchPreference"

private val incognitoModeResourcePatch = resourcePatch {
    dependsOn(settingsPatch)

    execute {
        PreferenceScreen.GENERAL.addPreferences(
            SwitchPreference(
                "morphe_music_incognito_mode",
                summary = true,
                tag = INCOGNITO_MODE_PREFERENCE_TAG
            )
        )

        copyResources(
            "incognito",
            ResourceGroup(
                "drawable",
                "morphe_incognito_menu_icon.xml",
                "morphe_incognito_menu_icon_on.xml"
            )
        )

        // Add the Home tab top-bar menu item, next to the existing search/account icons.
        val menu = get("res/menu/main_menu.xml")
        var menuXml = menu.readText()

        if (!menuXml.contains("morphe_incognito_menu_item")) {
            // orderInCategory lower than the existing items' "2" so this sorts before them.
            val newItem = "<item android:icon=\"@drawable/morphe_incognito_menu_icon\" " +
                "android:id=\"@+id/morphe_incognito_menu_item\" " +
                "android:visible=\"true\" " +
                "android:orderInCategory=\"1\" " +
                "android:title=\"@string/morphe_music_incognito_menu_item_off\" " +
                "yt:showAsAction=\"always\"/>\n"

            // Anchor on </menu> rather than </group> - some app versions (e.g. 9.15.51) don't
            // wrap the top-bar items in a <group> at all, only later versions do.
            menuXml = menuXml.replace("</menu>", "$newItem</menu>")
        }

        menu.writeText(menuXml)
    }
}

@Suppress("unused")
val incognitoModePatch = bytecodePatch(
    name = "Incognito mode",
    description = "Adds an Incognito mode toggle to the Home tab top bar. While on, tracks you " +
        "play and searches you make in this app are not saved to your YouTube history."
) {
    dependsOn(
        sharedExtensionPatch,
        incognitoModeResourcePatch,
        buildRequestPatch,
    )

    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

    execute {
        // Runs after every top bar menu (re)creation, so the icon's on/off state survives page changes.
        MusicActivityOnPrepareOptionsMenuFingerprint.method.addInstructions(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS->prepareMenu(Landroid/view/Menu;)V"
        )

        // The toolbar's MenuItem.setOnMenuItemClickListener does not appear to be consulted by
        // this app's own click dispatch, so intercept clicks the same way the app's real menu
        // items are handled: at the Activity level, which every tab's unhandled clicks reach.
        MusicActivityOnOptionsItemSelectedFingerprint.method.cloneParameters().apply {
            addInstructionsWithLabels(
                0,
                """
                    invoke-static { p1 }, $EXTENSION_CLASS->handleMenuItemSelected(Landroid/view/MenuItem;)Z
                    move-result v0
                    if-eqz v0, :not_ours
                    return v0
                    :not_ours
                    nop
                """
            )
        }

        // URL hook first, so the header hook below sees the rewritten URL.
        hookBuildRequestUrl("$EXTENSION_CLASS->redirectHistoryPing")
        hookBuildRequest("$EXTENSION_CLASS->filterRequestHeaders", hookHeader = true)
    }
}
