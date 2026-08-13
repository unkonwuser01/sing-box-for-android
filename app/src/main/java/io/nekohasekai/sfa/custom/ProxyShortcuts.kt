package io.nekohasekai.sfa.custom

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import io.nekohasekai.sfa.R

/** Publishes the three app actions without creating launcher icons. */
object ProxyShortcuts {
    private const val START = "proxy_start"
    private const val STOP = "proxy_stop"
    private const val TOGGLE = "proxy_toggle"

    fun publish(context: Context) {
        val appContext = context.applicationContext
        val packageName = appContext.packageName
        ShortcutManagerCompat.removeDynamicShortcuts(appContext, listOf(START, STOP, TOGGLE))
        ShortcutManagerCompat.addDynamicShortcuts(
            appContext,
            listOf(
                shortcut(appContext, START, R.string.shortcut_proxy_start, R.drawable.ic_play_arrow_24, "$packageName.action.START"),
                shortcut(appContext, STOP, R.string.shortcut_proxy_stop, R.drawable.ic_stop_24, "$packageName.action.STOP"),
                shortcut(appContext, TOGGLE, R.string.shortcut_proxy_toggle, R.drawable.ic_electric_bolt_24, "$packageName.action.TOGGLE"),
            ),
        )
    }

    private fun shortcut(
        context: Context,
        id: String,
        label: Int,
        icon: Int,
        action: String,
    ): ShortcutInfoCompat =
        ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(context.getString(label))
            .setLongLabel(context.getString(label))
            .setIcon(IconCompat.createWithResource(context, icon))
            .setIntent(Intent(context, ProxyShortcutActivity::class.java).setAction(action))
            .build()
}
