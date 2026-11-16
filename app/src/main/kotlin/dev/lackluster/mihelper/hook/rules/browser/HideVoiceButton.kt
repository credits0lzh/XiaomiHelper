package dev.lackluster.mihelper.hook.rules.browser

import com.highcapable.kavaref.KavaRef.Companion.resolve
import dev.lackluster.mihelper.data.preference.Preferences
import dev.lackluster.mihelper.hook.base.BaseHooker
import dev.lackluster.mihelper.hook.utils.RemotePreferences.observe

object HideVoiceButton : BaseHooker() {
    override val supportsHotReload: Boolean = true
    override val supportsRuntimeToggle: Boolean = true

    override fun onInit() {
        registerDisposeCleanup(
            Preferences.Browser.HIDE_VOICE_BUTTON.observe {
                updateSelfState(it)
            }
        )
    }

    override fun onStart() {
        "com.android.browser.homepage.premium.view.PressVoiceButtonView".toClassOrNull()?.apply {
            resolve().firstMethodOrNull {
                name = "show"
            }?.hook {
                result(null)
            }
        }
    }
}