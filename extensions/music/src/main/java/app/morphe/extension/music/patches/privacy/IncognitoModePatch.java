/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.music.patches.privacy;

import static app.morphe.extension.shared.StringRef.str;

import android.content.Context;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;

import java.util.HashMap;
import java.util.Map;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.ResourceType;
import app.morphe.extension.shared.ResourceUtils;
import app.morphe.extension.shared.Utils;

/**
 * "Incognito mode": while enabled, the playback reports that add tracks to the account's watch
 * history are not sent to YouTube, and searches are sent signed out so they are not saved to the
 * account's search history. This only affects this app; it does not change the account's own
 * "Pause watch/search history" settings.
 */
@SuppressWarnings("unused")
public final class IncognitoModePatch {

    // Watch history is recorded from these playback reports (see CheckWatchHistoryDomainNameResolutionPatch).
    private static final String HISTORY_TRACKING_HOST = "s.youtube.com";

    // Blocked reports are sent here instead of failing, so the app treats them as delivered and
    // does not queue them for a retry that could land after Incognito mode is turned off.
    // Returns an empty "204 No Content" response.
    private static final String NO_OP_URL = "https://www.google.com/generate_204";

    // Search history is saved server-side from signed-in search requests.
    private static final String SEARCH_PATH = "/youtubei/v1/search";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String PAGE_ID_HEADER = "X-Goog-PageId";

    private IncognitoModePatch() {
    }

    /** Injection point: called whenever the user flips Incognito mode, from any UI entry point. */
    public static void toggle(Context context, boolean enabled) {
        Settings.INCOGNITO_MODE.save(enabled);
        Utils.showToastShort(enabled
                ? str("morphe_music_incognito_mode_enabled_toast")
                : str("morphe_music_incognito_mode_disabled_toast"));
    }

    public static boolean isEnabled() {
        return Settings.INCOGNITO_MODE.get();
    }

    /**
     * Injection point: called with every outgoing request URL before the request is built.
     */
    public static String redirectHistoryPing(String url) {
        try {
            if (url == null || !isHistoryPing(url)) {
                return url;
            }
            return isEnabled() ? NO_OP_URL : url;
        } catch (Exception ex) {
            Logger.printException(() -> "Incognito mode: redirectHistoryPing failure", ex);
            return url;
        }
    }

    /**
     * Injection point: called with every outgoing request's headers.
     * Redirected history reports are sent without the original YouTube headers, and while
     * Incognito mode is on, searches are sent signed out so they are not saved to search history.
     */
    public static Map<String, String> filterRequestHeaders(String url, Map<String, String> headers) {
        try {
            if (NO_OP_URL.equals(url)) {
                return new HashMap<>();
            }
            if (url != null && headers != null && isEnabled() && isSearchRequest(url)) {
                Map<String, String> signedOut = new HashMap<>(headers);
                signedOut.remove(AUTHORIZATION_HEADER);
                signedOut.remove(PAGE_ID_HEADER);
                return signedOut;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "Incognito mode: filterRequestHeaders failure", ex);
        }
        return headers;
    }

    private static boolean isSearchRequest(String url) {
        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        String path = uri.getPath();
        return host != null && path != null
                && (host.endsWith("youtube.com") || host.endsWith("googleapis.com"))
                && path.endsWith(SEARCH_PATH);
    }

    private static boolean isHistoryPing(String url) {
        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        if (host == null) {
            return false;
        }
        if (host.equals(HISTORY_TRACKING_HOST)) {
            return true;
        }
        String path = uri.getPath();
        return path != null
                && host.endsWith("youtube.com")
                && (path.startsWith("/api/stats/playback") || path.startsWith("/api/stats/watchtime"));
    }

    /**
     * Injection point: called from the Home tab's top bar menu preparation. Shows the Incognito
     * menu item and reflects the current state in its icon and title.
     */
    public static void prepareMenu(Menu menu) {
        try {
            int id = ResourceUtils.getIdentifier(ResourceType.ID, "morphe_incognito_menu_item");
            if (id == 0) {
                return;
            }
            MenuItem item = menu.findItem(id);
            if (item == null) {
                return;
            }
            item.setVisible(true);
            updateMenuItem(item);
        } catch (Exception ex) {
            Logger.printException(() -> "Incognito mode: failed to prepare menu item", ex);
        }
    }

    /**
     * Injection point: called from MusicActivity.onOptionsItemSelected() for every menu click
     * not already consumed by the active tab. This app's toolbar does not appear to consult
     * MenuItem.setOnMenuItemClickListener, so the click is handled here instead.
     */
    public static boolean handleMenuItemSelected(MenuItem item) {
        try {
            int id = ResourceUtils.getIdentifier(ResourceType.ID, "morphe_incognito_menu_item");
            if (id == 0 || item.getItemId() != id) {
                return false;
            }
            toggle(Utils.getContext(), !isEnabled());
            updateMenuItem(item);
            return true;
        } catch (Exception ex) {
            Logger.printException(() -> "Incognito mode: menu item click failed", ex);
            Utils.showToastShort(str("morphe_music_incognito_mode_failed_toast"));
            return true;
        }
    }

    private static void updateMenuItem(MenuItem item) {
        boolean enabled = isEnabled();
        item.setTitle(enabled
                ? str("morphe_music_incognito_menu_item_on")
                : str("morphe_music_incognito_menu_item_off"));

        int iconId = ResourceUtils.getIdentifier(
                ResourceType.DRAWABLE,
                enabled ? "morphe_incognito_menu_icon_on" : "morphe_incognito_menu_icon"
        );
        if (iconId != 0) {
            item.setIcon(iconId);
        }
    }
}
