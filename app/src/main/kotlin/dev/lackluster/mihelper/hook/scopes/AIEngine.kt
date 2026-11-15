package dev.lackluster.mihelper.hook.scopes

import dev.lackluster.mihelper.hook.base.BaseHooker
import dev.lackluster.mihelper.hook.rules.aiengine.CopyDirectWebsite

object AIEngine : BaseHooker() {
    override val supportsHotReload: Boolean = true
    override val supportsRuntimeToggle: Boolean = true

    override val requireDexKit: Boolean = true

    override fun onInit() {
        attach(CopyDirectWebsite)
    }
}