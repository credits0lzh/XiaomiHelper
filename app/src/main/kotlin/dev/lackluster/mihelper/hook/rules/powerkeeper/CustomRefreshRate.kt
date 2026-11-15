/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * This file is part of XiaomiHelper project
 * Copyright (C) 2026 HowieHChen, howie.dev@outlook.com

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.lackluster.mihelper.hook.rules.powerkeeper

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.condition.type.Modifiers
import dev.lackluster.mihelper.data.preference.Preferences
import dev.lackluster.mihelper.hook.base.BaseHooker
import dev.lackluster.mihelper.hook.utils.RemotePreferences.observe
import dev.lackluster.mihelper.hook.utils.d
import dev.lackluster.mihelper.hook.utils.toTyped

object CustomRefreshRate : BaseHooker() {
    override val supportsHotReload: Boolean = true
    override val supportsRuntimeToggle: Boolean = true

    @Volatile
    private var oriIsCustomFpsSwitch: String? = null

    private val clzDisplayFrameSetting by "com.miui.powerkeeper.statemachine.DisplayFrameSetting".lazyClassOrNull()
    private val fldIsCustomFpsSwitch by lazy {
        clzDisplayFrameSetting?.resolve()?.firstFieldOrNull {
            name = "mIsCustomFpsSwitch"
            modifiers(Modifiers.STATIC)
        }?.toTyped<String>()
    }

    override fun onInit() {
        registerDisposeCleanup(
            Preferences.PowerKeeper.UNLOCK_CUSTOM_REFRESH.observe {
                updateSelfState(it)
            }
        )
    }

    override fun onStart() {
        clzDisplayFrameSetting?.apply {
            resolve().firstMethodOrNull {
                name = "parseCustomModeSwitchFromDb"
            }?.hook {
                val ori = proceed()
                oriIsCustomFpsSwitch = fldIsCustomFpsSwitch?.get(null)
                d { "onStart mIsCustomFpsSwitch = $oriIsCustomFpsSwitch" }
                fldIsCustomFpsSwitch?.set(null, "true")
                result(ori)
            }
        }
    }

    override fun onStop() {
        oriIsCustomFpsSwitch?.let {
            d { "onStop mIsCustomFpsSwitch = $it" }
            fldIsCustomFpsSwitch?.set(null, it)
        }
    }
}