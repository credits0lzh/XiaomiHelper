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

import android.content.Context
import android.os.Binder
import android.os.Bundle
import android.os.UserHandle
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.condition.type.Modifiers
import dev.lackluster.mihelper.data.preference.Preferences
import dev.lackluster.mihelper.hook.base.BaseHooker
import dev.lackluster.mihelper.hook.utils.RemotePreferences.observe
import dev.lackluster.mihelper.hook.utils.d
import dev.lackluster.mihelper.hook.utils.toTyped

object BlockRevertBatteryPolicy: BaseHooker() {
    override val supportsHotReload: Boolean = true
    override val supportsRuntimeToggle: Boolean = true

    override fun onInit() {
        registerDisposeCleanup(
            Preferences.PowerKeeper.BLOCK_REVERT_BATTERY_POLICY.observe {
                updateSelfState(it)
            }
        )
    }

    override fun onStart() {
        "com.miui.powerkeeper.provider.PowerSaveConfigureManager".toClassOrNull()?.apply {
            val metGetUserId = UserHandle::class.resolve().firstMethodOrNull {
                name = "getUserId"
                parameters(Int::class)
                returnType(Int::class)
                modifiers(Modifiers.STATIC)
            }?.toTyped<Int>() ?: return@apply
            val clzUserConfigureHelper = "com.miui.powerkeeper.provider.UserConfigureHelper".toClassOrNull()
            val metGetUserConfigureHelperByPkg = clzUserConfigureHelper?.resolve()?.firstMethodOrNull {
                name = "getUserConfigureHelperByPkg"
                parameters(Context::class, Int::class, String::class)
                modifiers(Modifiers.STATIC)
            }?.toTyped<Any>() ?: return@apply
            val metGetBgControl = clzUserConfigureHelper.resolve().firstMethodOrNull {
                name = "getBgControl"
                parameterCount = 0
                returnType(String::class)
            }?.toTyped<String>() ?: return@apply
            val metGetId = clzUserConfigureHelper.resolve().firstMethodOrNull {
                name = "getId"
                parameterCount = 0
                returnType(Long::class)
            }?.toTyped<Long>() ?: return@apply

            val fldContext = resolve().firstFieldOrNull {
                name = "mContext"
            }?.toTyped<Context>() ?: return@apply
            val fldUserConfigureHelper = resolve().firstFieldOrNull {
                name = "mUserConfigureHelper"
            }?.toTyped<Any>() ?: return@apply
            val fldHasVisited = resolve().firstFieldOrNull {
                name = "sHasVisited"
                modifiers(Modifiers.STATIC)
            }?.toTyped<Set<String>>() ?: return@apply
            val metStoreList = resolve().firstMethodOrNull {
                name = "storeList"
                modifiers(Modifiers.STATIC)
            }?.toTyped<Unit>() ?: return@apply
            val metGetInterfaceConfigureValue = resolve().firstMethodOrNull {
                name = "getInterfaceConfigureValue"
                parameters(String::class)
            }?.toTyped<String>() ?: return@apply

            resolve().firstMethodOrNull {
                name = "getPowerSaveAppConfigure"
            }?.hook {
                val input = getArg(0) as? Bundle
                val output = getArg(1) as? Bundle
                if (input == null || output == null) {
                    return@hook result(-1)
                }
                val pkg = input.getString("App", null)
                val userId = if (input.containsKey("UserId")) {
                    input.getInt("UserId")
                } else {
                    metGetUserId.invoke(null, Binder.getCallingUid())
                }
                d { "userId: $userId pkg: $pkg" }
                val context = fldContext.get(thisObject)
                if (pkg == null || userId == null || context == null) {
                    return@hook result(-1)
                }
                val mUserConfigureHelper = metGetUserConfigureHelperByPkg.invoke(null, context, userId, pkg)
                fldUserConfigureHelper.set(thisObject, mUserConfigureHelper)
                val id = metGetId.invoke(mUserConfigureHelper)
                if (id == null || id < 0) {
                    return@hook result(-1)
                }
                val bgControl = metGetBgControl.invoke(mUserConfigureHelper) ?: return@hook result(-1)
                val sHasVisited = fldHasVisited.get(null)
                if (sHasVisited is MutableSet && sHasVisited.add("$userId$pkg")) {
                    metStoreList.invoke(null)
                }
                val result = metGetInterfaceConfigureValue.invoke(thisObject, bgControl)
                    ?: return@hook result(-1)
                d { "return configure : $result" }
                output.putString("AppConfigure", result)
                result(0)
            }
        }
    }
}