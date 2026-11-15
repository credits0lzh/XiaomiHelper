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
import dev.lackluster.mihelper.data.preference.Preferences
import dev.lackluster.mihelper.hook.base.BaseHooker
import dev.lackluster.mihelper.hook.utils.RemotePreferences.observe
import dev.lackluster.mihelper.hook.utils.d

object GMSBackgroundRunning : BaseHooker() {
    override val supportsHotReload: Boolean = true
    override val supportsRuntimeToggle: Boolean = true

    override fun onInit() {
        registerDisposeCleanup(
            Preferences.PowerKeeper.GMS_BG_RUNNING.observe {
                updateSelfState(it)
            }
        )
    }

    override fun onStart() {
        "com.miui.powerkeeper.utils.GmsObserver".toClassOrNull()?.apply {
            resolve().optional(true).firstMethodOrNull {
                name = "updateFrameworkGmsNetStatus"
            }?.hook {
                val newArgs = args.toTypedArray()
                d { "updateFrameworkGmsNetStatus(${newArgs[0]})" }
                newArgs[0] = false
                result(proceed(newArgs))
            }
        }
        $$"com.miui.powerkeeper.provider.SimpleSettings$Misc".toClassOrNull()?.apply {
            resolve().firstMethodOrNull {
                name = "getBoolean"
                parameterCount = 3
            }?.hook {
                val key = getArg(1) as? String
                if (key == "gms_control") {
                    result(false)
                } else {
                    result(proceed())
                }
            }
        }
    }
}