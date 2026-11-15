package dev.lackluster.mihelper.hook.scopes

import dev.lackluster.mihelper.hook.base.BaseHooker
import dev.lackluster.mihelper.hook.rules.packageinstaller.AdBlocker
import dev.lackluster.mihelper.hook.rules.packageinstaller.RemoveElement
import dev.lackluster.mihelper.hook.rules.packageinstaller.ResourcesUtils
import dev.lackluster.mihelper.hook.rules.packageinstaller.CustomInstallSource
import dev.lackluster.mihelper.hook.rules.packageinstaller.DisguiseNoNet

object PackageInstaller : BaseHooker() {
    override val supportsHotReload: Boolean = true
    override val supportsRuntimeToggle: Boolean = true

    override val requireDexKit: Boolean = true

    override fun onInit() {
        attach(ResourcesUtils)
        attach(AdBlocker)
        attach(RemoveElement)
        attach(CustomInstallSource)
        attach(DisguiseNoNet)
    }
}