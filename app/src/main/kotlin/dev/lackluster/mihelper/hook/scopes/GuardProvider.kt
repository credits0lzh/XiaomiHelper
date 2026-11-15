package dev.lackluster.mihelper.hook.scopes

import dev.lackluster.mihelper.hook.base.BaseHooker
import dev.lackluster.mihelper.hook.rules.guardprovider.BlockEnvCheck
import dev.lackluster.mihelper.hook.rules.guardprovider.BlockUploadAppList

object GuardProvider: BaseHooker() {
    override val supportsHotReload: Boolean = true
    override val supportsRuntimeToggle: Boolean = true

    override val requireDexKit: Boolean = true

    override fun onInit() {
        attach(BlockUploadAppList)
        attach(BlockEnvCheck)
    }
}