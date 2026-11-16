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

import android.os.Bundle
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.condition.type.Modifiers
import com.highcapable.kavaref.extension.makeAccessible
import dev.lackluster.mihelper.data.preference.Preferences
import dev.lackluster.mihelper.hook.base.BaseHooker
import dev.lackluster.mihelper.hook.utils.RemotePreferences.observe
import dev.lackluster.mihelper.hook.utils.d
import dev.lackluster.mihelper.hook.utils.toTyped
import io.github.libxposed.api.XposedInterface

object BlockBatteryWhitelist : BaseHooker() {
    override val supportsHotReload: Boolean = true
    override val supportsRuntimeToggle: Boolean = false

    override fun onInit() {
        registerDisposeCleanup(
            Preferences.PowerKeeper.BLOCK_BATTERY_WHITELIST.observe {
                updateSelfState(it)
            }
        )
    }

    @Volatile
    private var alwaysWhiteAppsInit = false

    override fun onStart() {
        "com.miui.powerkeeper.controller.DeviceIdleController".toClassOrNull()?.apply {
            val fldAlwaysWhiteApps = resolve().firstFieldOrNull {
                name = "sAlwaysWhiteApps"
                type(Set::class)
                modifiers(Modifiers.STATIC)
            }?.toTyped<Set<String>>()
            val fldPowerSaveModeAlwaysWhiteApps = resolve().firstFieldOrNull {
                name = "sPowerSaveModeAlwaysWhiteApps"
                type(Set::class)
                modifiers(Modifiers.STATIC)
            }?.toTyped<Set<String>>()
            resolve().firstMethodOrNull {
                name = "initAlwaysWhiteApps"
            }?.hook {
                synchronized(this@apply) {
                    if (alwaysWhiteAppsInit) {
                        return@hook result(null)
                    }
                    val sAlwaysWhiteApps = fldAlwaysWhiteApps?.get(null)
                    if (sAlwaysWhiteApps is MutableSet) {
                        sAlwaysWhiteApps.clear()
                        sAlwaysWhiteApps.add("com.android.deskclock")
                    }
                    val sPowerSaveModeAlwaysWhiteApps = fldPowerSaveModeAlwaysWhiteApps?.get(null)
                    if (sPowerSaveModeAlwaysWhiteApps is MutableSet) {
                        sPowerSaveModeAlwaysWhiteApps.clear()
                    }
                    d { "initAlwaysWhiteApps sAlwaysWhiteApps=${sAlwaysWhiteApps?.joinToString()}\nsPowerSaveModeAlwaysWhiteApps=${sPowerSaveModeAlwaysWhiteApps?.joinToString()}" }
                    alwaysWhiteAppsInit = true
                }
                result(null)
            }
        }
        "com.miui.powerkeeper.provider.GlobalFeatureConfigureHelper".toClassOrNull()?.apply {
            resolve().firstMethodOrNull {
                name = "getDozeWhiteListApps"
                parameters(Bundle::class)
                modifiers(Modifiers.STATIC)
            }?.hook {
                result(arrayListOf<String>())
            }
        }
        "com.miui.powerkeeper.utils.CommonAdapter".toClassOrNull()?.apply {
            val metRemovePowerSaveWhitelistApp = "android.os.IDeviceIdleController".toClassOrNull()?.resolve()?.firstMethodOrNull {
                name = "removePowerSaveWhitelistApp"
                parameters(String::class)
            }?.self?.apply {
                makeAccessible()
            } ?: return@apply
            resolve().firstMethodOrNull {
                name = "removePowerSaveWhitelistApps"
            }?.hook(exceptionMode = XposedInterface.ExceptionMode.PASSTHROUGH) {
                val iDeviceIdleController = getArg(0)
                val packages = (getArg(1) as? Array<*>)?.filterIsInstance<String>()
                if (iDeviceIdleController == null || packages == null) {
                    return@hook result(proceed())
                }
                var failure: Exception? = null
                for (pkg in packages) {
                    try {
                        metRemovePowerSaveWhitelistApp.invoke(iDeviceIdleController, pkg)
                    } catch (e: Exception) {
                        if (failure == null) {
                            failure = e
                        } else {
                            failure.addSuppressed(e)
                        }
                    }
                }
                failure?.let { throw it }
                result(null)
            }
        }
    }
}