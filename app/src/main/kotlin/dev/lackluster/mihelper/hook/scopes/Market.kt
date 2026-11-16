package dev.lackluster.mihelper.hook.scopes

import dev.lackluster.mihelper.hook.base.BaseHooker
import dev.lackluster.mihelper.hook.rules.market.AdBlocker
import dev.lackluster.mihelper.hook.rules.market.BlockUpdateDialog
import dev.lackluster.mihelper.hook.rules.market.DisableCustomizeIcon
import dev.lackluster.mihelper.hook.rules.market.HideAppSecurity
import dev.lackluster.mihelper.hook.rules.market.HideTabItem
import dev.lackluster.mihelper.hook.rules.market.SkipSplash

object Market : BaseHooker() {
    override val supportsHotReload: Boolean = true
    override val supportsRuntimeToggle: Boolean = true

    override fun onInit() {
        attach(SkipSplash)
        attach(DisableCustomizeIcon)
        attach(HideAppSecurity)
        attach(AdBlocker)
        attach(BlockUpdateDialog)
        attach(HideTabItem)
    }
}