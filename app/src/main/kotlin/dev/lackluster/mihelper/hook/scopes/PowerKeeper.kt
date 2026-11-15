package dev.lackluster.mihelper.hook.scopes

import dev.lackluster.mihelper.hook.base.BaseHooker
import dev.lackluster.mihelper.hook.rules.powerkeeper.BlockBatteryWhitelist
import dev.lackluster.mihelper.hook.rules.powerkeeper.BlockRevertBatteryPolicy
import dev.lackluster.mihelper.hook.rules.powerkeeper.CustomRefreshRate
import dev.lackluster.mihelper.hook.rules.powerkeeper.GMSBackgroundRunning

object PowerKeeper : BaseHooker() {
    override val supportsHotReload: Boolean = true
    override val supportsRuntimeToggle: Boolean = true

    override val requireDexKit: Boolean = true

    override fun onInit() {
        attach(CustomRefreshRate)
        attach(BlockBatteryWhitelist)
        attach(BlockRevertBatteryPolicy)
        attach(GMSBackgroundRunning)
    }
}