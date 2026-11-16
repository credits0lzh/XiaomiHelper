package dev.lackluster.mihelper.hook.scopes

import dev.lackluster.mihelper.hook.base.BaseHooker
import dev.lackluster.mihelper.hook.rules.browser.BlockDialog
import dev.lackluster.mihelper.hook.rules.browser.Configuration
import dev.lackluster.mihelper.hook.rules.browser.DebugMode
import dev.lackluster.mihelper.hook.rules.browser.DisableUpdateCheck
import dev.lackluster.mihelper.hook.rules.browser.HideHomepageTopBar
import dev.lackluster.mihelper.hook.rules.browser.HideVoiceButton
import dev.lackluster.mihelper.hook.rules.browser.SkipSplash
import dev.lackluster.mihelper.hook.rules.browser.SwitchEnv
import dev.lackluster.mihelper.hook.rules.browser.SyncUserAgentMetadata

object Browser : BaseHooker() {
    override val supportsHotReload: Boolean = true
    override val supportsRuntimeToggle: Boolean = true

    override val requireDexKit: Boolean = true

    override fun onInit() {
        attach(DebugMode)
        attach(SwitchEnv)
        attach(DisableUpdateCheck)
        attach(SkipSplash)
        attach(HideVoiceButton)
        attach(HideHomepageTopBar)
        attach(Configuration)
        attach(SyncUserAgentMetadata)
        attach(BlockDialog)
        return
//        attach(FixBackHandler)
    }
}