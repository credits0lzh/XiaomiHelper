/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * This file is part of XiaomiHelper project
 * Copyright (C) 2025 HowieHChen, howie.dev@outlook.com

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

package dev.lackluster.mihelper.hook.rules.market

import com.highcapable.kavaref.KavaRef.Companion.resolve
import dev.lackluster.mihelper.data.preference.Preferences
import dev.lackluster.mihelper.hook.base.BaseHooker
import dev.lackluster.mihelper.hook.utils.RemotePreferences.observe
import dev.lackluster.mihelper.hook.utils.toTyped

object SkipSplash : BaseHooker() {
    override val supportsHotReload: Boolean = true
    override val supportsRuntimeToggle: Boolean = true

    override fun onInit() {
        registerDisposeCleanup(
            Preferences.Market.SKIP_SPLASH.observe {
                updateSelfState(it)
            }
        )
    }

    override fun onStart() {
        "com.xiaomi.market.ui.splash.SplashManager".toClassOrNull()?.apply {
            val metNotifySplashDismiss = resolve().firstMethodOrNull {
                name = "notifySplashDismiss"
            }?.toTyped<Unit>()
            resolve().firstMethodOrNull {
                name = "tryAdSplash"
            }?.hook {
                result(null)
            }
            resolve().firstMethodOrNull {
                name = "trySplashWhenApplicationForeground"
            }?.hook {
                metNotifySplashDismiss?.invoke(thisObject)
                result(null)
            }
        }
        "com.xiaomi.market.business_ui.main.MarketTabActivity".toClassOrNull()?.apply {
            resolve().firstMethodOrNull {
                name = "trySplash"
            }?.hook {
                result(null)
            }
        }
    }
}