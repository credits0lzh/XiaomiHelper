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

package dev.lackluster.mihelper.hook.rules.guardprovider

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.condition.type.Modifiers
import dev.lackluster.mihelper.data.preference.Preferences
import dev.lackluster.mihelper.hook.base.BaseHooker
import dev.lackluster.mihelper.hook.utils.DexKit
import dev.lackluster.mihelper.hook.utils.RemotePreferences.get
import dev.lackluster.mihelper.hook.utils.toTyped
import org.luckypray.dexkit.query.enums.StringMatchType
import java.util.Arrays

object BlockEnvCheck : BaseHooker() {
    override val supportsHotReload: Boolean = true
    override val supportsRuntimeToggle: Boolean = false

    private val clzDeviceStateReporter by lazy {
        DexKit.findClassWithCache("device_state_reporter") {
            matcher {
                addUsingString("device_state.xml", StringMatchType.Equals)
                addUsingString("DeviceStateReporter", StringMatchType.Equals)
            }
        }
    }
    private val checkRootClass by lazy {
        DexKit.findClassesWithCache("su_check") {
            matcher {
                addUsingString("/data/local/su", StringMatchType.Equals)
                addUsingString("/system/bin/su", StringMatchType.Equals)
                addUsingString("/system/xbin/su", StringMatchType.Equals)
            }
        }
    }

    override fun onInit() {
        Preferences.GuardProvider.BLOCK_ENV_CHECK.get().also {
            updateSelfState(it)
        }
        clzDeviceStateReporter
        checkRootClass
    }

    override fun onStart() {
        clzDeviceStateReporter?.getInstance(classLoader)?.apply {
            val fldType = resolve().firstFieldOrNull {
                type(Int::class)
                modifiers(Modifiers.FINAL)
            }?.toTyped<Int>()
            resolve().firstMethodOrNull {
                name = "run"
            }?.hook {
                if (fldType?.get(thisObject) == 6) {
                    result(null)
                } else {
                    result(proceed())
                }
            }
        }
        checkRootClass.forEach { clz ->
            clz.getInstance(classLoader).apply {
                resolve().firstFieldOrNull {
                    type(Array<String>::class)
                    modifiers(Modifiers.STATIC)
                }?.toTyped<Array<String>>()?.get(null)?.let { suPaths ->
                    Arrays.fill(suPaths, "/invalid/path/to/su")
                }
            }
        }
    }
}