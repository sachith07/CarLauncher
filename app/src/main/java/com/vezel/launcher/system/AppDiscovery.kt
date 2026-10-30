package com.vezel.launcher.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.widget.Toast
import com.vezel.launcher.R

/**
 * Discovers MAIN/LAUNCHER activities and opens packages safely.
 */
class AppDiscovery(private val context: Context) {

    data class LauncherApp(
        val label: String,
        val packageName: String,
        val activityName: String,
        val icon: Drawable?
    )

    fun discoverLauncherApps(): List<LauncherApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> = try {
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        } catch (_: Exception) {
            emptyList()
        }

        val self = context.packageName
        return resolved
            .asSequence()
            .filter { it.activityInfo?.packageName != self }
            .mapNotNull { info ->
                val activity = info.activityInfo ?: return@mapNotNull null
                LauncherApp(
                    label = info.loadLabel(pm)?.toString().orEmpty().ifBlank { activity.packageName },
                    packageName = activity.packageName,
                    activityName = activity.name,
                    icon = try {
                        info.loadIcon(pm)
                    } catch (_: Exception) {
                        null
                    }
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun openPackage(packageName: String): Boolean {
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

    /**
     * Tries known package candidates, then a category picker, then a toast.
     */
    fun launchKnownOrFallback(candidates: List<String>, pickerAction: String? = null) {
        for (pkg in candidates) {
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

    companion object {
        val RADIO_PACKAGES = listOf(
            "com.android.radio",
            "com.android.car.radio",
            "com.mediatek.radio",
            "com.mx.radio"
        )
        val MAPS_PACKAGES = listOf(
            "com.google.android.apps.maps",
            "com.android.maps",
            "com.baidu.BaiduMap"
        )
        val PHONE_PACKAGES = listOf(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.android.contacts"
        )
    }
}
