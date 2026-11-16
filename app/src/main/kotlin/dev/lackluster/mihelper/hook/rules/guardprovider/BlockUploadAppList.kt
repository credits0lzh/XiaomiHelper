/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * This file is part of XiaomiHelper project
 * Copyright (C) 2023 HowieHChen, howie.dev@outlook.com

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
import dev.lackluster.mihelper.hook.utils.RemotePreferences.observe
import dev.lackluster.mihelper.hook.utils.d
import dev.lackluster.mihelper.hook.utils.toTyped
import org.luckypray.dexkit.query.enums.StringMatchType

object BlockUploadAppList : BaseHooker() {
    override val supportsHotReload: Boolean = true
    override val supportsRuntimeToggle: Boolean = true

    private val metReportAllUnSystemAppsStatus by lazy {
        DexKit.findMethodWithCache("get_all_unsystem_apps") {
            matcher {
                returnType = "java.lang.String"
                addUsingString("AntiDefraudAppManager", StringMatchType.Equals)
                addUsingString("getAllUnSystemAppsStatus", StringMatchType.StartsWith)
            }
        }
    }
    private val metCloudScanRequest by lazy {
        DexKit.findMethodWithCache("cloud_scan_request") {
            matcher {
                addUsingString("CloudScanner", StringMatchType.Equals)
                addUsingString("androidVersion", StringMatchType.Equals)
            }
        }
    }
    private val metReportStore by lazy {
        DexKit.findMethodWithCache("report_store") {
            matcher {
                addUsingString("ReportStore", StringMatchType.Equals)
                addUsingString("apk file gone, drop", StringMatchType.Contains)
            }
        }
    }
    private val clzActionResult by lazy {
        DexKit.findClassWithCache("action_result") {
            matcher {
                addUsingString("ActionResult", StringMatchType.StartsWith)
                addUsingString("message", StringMatchType.Contains)
            }
        }
    }
    private val clzEnumResult by lazy {
        DexKit.findClassWithCache("enum_permanent") {
            matcher {
                addUsingString("Success", StringMatchType.Equals)
                addUsingString("Permanent", StringMatchType.Equals)
                addUsingString("Transient", StringMatchType.Equals)
            }
        }
    }
    private val enumPermanent by lazy {
        clzEnumResult?.getInstance(classLoader)?.resolve()?.firstMethodOrNull {
            name = "valueOf"
            modifiers(Modifiers.STATIC)
        }?.invoke("Permanent")
    }

    override fun onInit() {
        registerDisposeCleanup(
            Preferences.GuardProvider.BLOCK_UPLOAD_APP.observe {
                updateSelfState(it)
            }
        )
        metReportAllUnSystemAppsStatus
        metCloudScanRequest
        metReportStore
        clzEnumResult
        clzActionResult
    }

    override fun onStart() {
        metReportAllUnSystemAppsStatus?.getMethodInstance(classLoader)?.hook {
            result(null)
        }
        metCloudScanRequest?.getMethodInstance(classLoader)?.hook {
            result(null)
        }
        metReportStore?.getMethodInstance(classLoader)?.let { method ->
            if (enumPermanent != null) {
                method.hook {
                    result(enumPermanent)
                }
            }
        }
        d { "enumPermanent $enumPermanent" }
        "com.miui.guardprovider.action.impl.CollectPackagesAction".toClassOrNull()?.apply {
            val clz = clzActionResult?.getInstance(this@BlockUploadAppList.classLoader)
            val actionResult = clz?.resolve()?.firstMethodOrNull {
                modifiers(Modifiers.STATIC)
                parameters(String::class)
            }?.invoke("Skipped")
            if (actionResult != null) {
                clz.resolve().firstFieldOrNull {
                    type(Int::class)
                }?.toTyped<Int>()?.set(actionResult, 0)
                resolve().firstMethodOrNull {
                    parameters("android.content.Context", "java.lang.Object")
                }?.hook {
                    result(actionResult)
                }
            }
            d { "actionResult $actionResult" }
        }
    }
}