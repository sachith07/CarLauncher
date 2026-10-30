package com.vezel.launcher.system

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.widget.Toast
import com.vezel.launcher.R
import com.vezel.launcher.data.LauncherPreferences

/**
 * Discovers MAIN/LAUNCHER activities and opens packages safely.
 * Prefers user-configured OEM packages from [LauncherPreferences].
 */
class AppDiscovery(private val context: Context) {

    data class LauncherApp(
        val label: String,
        val packageName: String,
        val activityName: String,
        val icon: Drawable?,
        val isSystem: Boolean = false
    )

    private val prefs = LauncherPreferences(context)

    @Volatile
    private var cachedApps: List<LauncherApp>? = null

    fun discoverLauncherApps(forceRefresh: Boolean = false): List<LauncherApp> {
        if (!forceRefresh) {
            cachedApps?.let { return it }
        }
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> = try {
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        } catch (_: Exception) {
            emptyList()
        }

        val self = context.packageName
        val list = resolved
            .asSequence()
            .filter { it.activityInfo?.packageName != self }
            .mapNotNull { info ->
                val activity = info.activityInfo ?: return@mapNotNull null
                val appInfo = try {
                    pm.getApplicationInfo(activity.packageName, 0)
                } catch (_: Exception) {
                    null
                }
                val isSystem = appInfo != null &&
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                LauncherApp(
                    label = info.loadLabel(pm)?.toString().orEmpty().ifBlank { activity.packageName },
                    packageName = activity.packageName,
                    activityName = activity.name,
                    icon = try {
                        info.loadIcon(pm)
                    } catch (_: Exception) {
                        null
                    },
                    isSystem = isSystem
                )
            }
            .sortedWith(compareBy({ it.isSystem }, { it.label.lowercase() }))
            .toList()
        cachedApps = list
        return list
    }

    fun invalidateCache() {
        cachedApps = null
    }

    fun openPackage(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        return try {
            val launch = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun openLauncherApp(app: LauncherApp): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(app.packageName, app.activityName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            openPackage(app.packageName)
        }
    }

    fun isPackageInstalled(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Tries preferred package first, then known candidates, then a category picker, then a toast.
     */
    fun launchKnownOrFallback(
        preferredPackage: String?,
        candidates: List<String>,
        pickerAction: String? = null
    ) {
        if (!preferredPackage.isNullOrBlank() && openPackage(preferredPackage)) return
        for (pkg in candidates) {
            if (pkg == preferredPackage) continue
            if (openPackage(pkg)) return
        }
        if (pickerAction != null) {
            try {
                val picker = Intent(pickerAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (picker.resolveActivity(context.packageManager) != null) {
                    context.startActivity(picker)
                    return
                }
            } catch (_: Exception) {
                // fall through to toast
            }
        }
        Toast.makeText(context, R.string.no_app_found, Toast.LENGTH_SHORT).show()
    }

    fun launchRadio() =
        launchKnownOrFallback(prefs.radioPackage.ifBlank { null }, RADIO_PACKAGES)

    fun launchMaps() =
        launchKnownOrFallback(
            prefs.mapsPackage.ifBlank { null },
            MAPS_PACKAGES,
            Intent.ACTION_VIEW
        )

    fun launchPhone() =
        launchKnownOrFallback(
            prefs.phonePackage.ifBlank { null },
            PHONE_PACKAGES,
            Intent.ACTION_DIAL
        )

    fun launchMusic() =
        launchKnownOrFallback(
            prefs.mediaPackage.ifBlank { null },
            MediaSessionController.MEDIA_FALLBACKS
        )

    fun launchVideo() =
        launchKnownOrFallback(null, VIDEO_PACKAGES)

    fun launchConnectivity() {
        val intents = listOf(
            Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS),
            Intent(android.provider.Settings.ACTION_WIFI_SETTINGS),
            Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS),
            Intent(android.provider.Settings.ACTION_SETTINGS)
        )
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return
                }
            } catch (_: Exception) {
            }
        }
        Toast.makeText(context, R.string.no_app_found, Toast.LENGTH_SHORT).show()
    }

    /** Opens Android's home-app picker / default apps settings when available. */
    fun openHomeAppSettings(): Boolean {
        val intents = listOf(
            Intent(android.provider.Settings.ACTION_HOME_SETTINGS),
            Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(android.provider.Settings.ACTION_SETTINGS)
        )
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (_: Exception) {
            }
        }
        return false
    }

    companion object {
        val RADIO_PACKAGES = listOf(
            "com.android.radio",
            "com.android.car.radio",
            "com.mediatek.radio",
            "com.mx.radio",
            "com.syu.radio"
        )
        val MAPS_PACKAGES = listOf(
            "com.google.android.apps.maps",
            "com.android.maps",
            "com.baidu.BaiduMap",
            "com.waze"
        )
        val PHONE_PACKAGES = listOf(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.android.contacts",
            "com.android.server.telecom"
        )
        val VIDEO_PACKAGES = listOf(
            "com.google.android.youtube",
            "com.android.gallery3d",
            "com.mxtech.videoplayer.ad",
            "com.mxtech.videoplayer.pro",
            "org.videolan.vlc",
            "com.android.chrome"
        )
    }
}
