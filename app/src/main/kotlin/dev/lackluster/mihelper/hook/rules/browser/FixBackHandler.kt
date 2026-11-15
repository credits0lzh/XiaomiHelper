package dev.lackluster.mihelper.hook.rules.browser

import com.highcapable.kavaref.KavaRef.Companion.resolve
import dev.lackluster.mihelper.hook.base.BaseHooker
import dev.lackluster.mihelper.hook.utils.toTyped

object FixBackHandler : BaseHooker() {
    override val supportsHotReload: Boolean = true
    override val supportsRuntimeToggle: Boolean = true

    override fun onInit() {
        updateSelfState(true)
    }

    override fun onStart() {
        "com.android.browser.menu.PortraitMenuDialog".toClassOrNull()?.apply {
            val metOnHiddenChanged = resolve().firstMethodOrNull {
                name = "onHiddenChanged"
                parameters(Boolean::class)
                superclass()
            }?.toTyped<Unit>()
            val metIsHidden = resolve().firstMethodOrNull {
                name = "isHidden"
                parameterCount = 0
                returnType(Boolean::class)
                superclass()
            }?.toTyped<Boolean>()
            resolve().firstMethodOrNull {
                name = "onViewCreated"
            }?.hook {
                val ori = proceed()
                metOnHiddenChanged?.invoke(thisObject, metIsHidden?.invoke(thisObject) == true)
                result(ori)
            }
        }
    }
}