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

import android.app.Activity
import android.view.View
import com.highcapable.kavaref.KavaRef.Companion.resolve
import dev.lackluster.mihelper.data.Scope
import dev.lackluster.mihelper.data.preference.Preferences
import dev.lackluster.mihelper.hook.base.BaseHooker
import dev.lackluster.mihelper.hook.utils.RemotePreferences.observe
import dev.lackluster.mihelper.hook.utils.toTyped
import dev.lackluster.mihelper.utils.factory.getResId
import io.github.libxposed.api.XposedInterface

object HideTabItem : BaseHooker() {
    override val supportsHotReload: Boolean = true
    override val supportsRuntimeToggle: Boolean = true

    @Volatile
    private var ignoreRestrict = false
    @Volatile
    private var showTabHome = true
    @Volatile
    private var showTabGame = true
    @Volatile
    private var showTabRank = true
    @Volatile
    private var showTabAgent = true
    @Volatile
    private var showTabAppAssemble = true
    @Volatile
    private var showTabMiniGame = true
    @Volatile
    private var showTabMine = true
    @Volatile
    private var showTabOthers = true

    private val clzTabInfo by "com.xiaomi.market.model.TabInfo".lazyClassOrNull()

    private var hideTabContainer = false
    private var tabContainerId = 0
    private var hideTabHooker: XposedInterface.HookHandle? = null
    private val fldActivity by lazy {
        "com.xiaomi.market.ui.DoubleTabProxyActivityWrapper".toClassOrNull()?.resolve()?.firstFieldOrNull {
            name = "mActivity"
            superclass()
        }?.toTyped<Activity>()
    }
    private val hideTabMethod by lazy {
        "com.xiaomi.market.ui.DoubleTabProxyActivityWrapper".toClassOrNull()?.resolve()?.firstMethodOrNull {
            name = "setTabContainer"
        }
    }

    private fun checkConfig() {
        var needHideTabContainer: Boolean
        if (ignoreRestrict) {
            var visibleTab = 0
            if (showTabHome) visibleTab++
            if (showTabGame) visibleTab++
            if (showTabRank) visibleTab++
            if (showTabAgent) visibleTab++
            if (showTabAppAssemble) visibleTab++
            if (showTabMiniGame) visibleTab++
            if (showTabMine) visibleTab++
            needHideTabContainer = (visibleTab == 1)
        } else {
            needHideTabContainer = false
        }
        if (needHideTabContainer != hideTabContainer) {
            hideTabHooker?.unhook()
            if (needHideTabContainer) {
                hideTabHooker = hideTabMethod?.hook {
                    val ori = proceed()
                    fldActivity?.get(thisObject)?.let {
                        if (tabContainerId == 0) {
                            tabContainerId = it.getResId("tab_container_layout", "id", Scope.MARKET)
                        }
                        it.findViewById<View>(tabContainerId)?.visibility = View.GONE
                    }
                    result(ori)
                }
            }
            hideTabContainer = needHideTabContainer
        }
    }

    override fun onInit() {
        registerDisposeCleanup(
            Preferences.Market.ENABLE_FILTER_TAB.observe {
                updateSelfState(it)
            }
        )
    }

    override fun onStart() {
        registerStopCleanup(
            Preferences.Market.FILTER_TAB_IGNORE_RESTRICT.observe {
                ignoreRestrict = it
                checkConfig()
            }
        )
        registerStopCleanup(
            Preferences.Market.HIDE_TAB_HOME.observe {
                showTabHome = !it
                checkConfig()
            }
        )
        registerStopCleanup(
            Preferences.Market.HIDE_TAB_GAME.observe {
                showTabGame = !it
                checkConfig()
            }
        )
        registerStopCleanup(
            Preferences.Market.HIDE_TAB_RANK.observe {
                showTabRank = !it
                checkConfig()
            }
        )
        registerStopCleanup(
            Preferences.Market.HIDE_TAB_AGENT.observe {
                showTabAgent = !it
                checkConfig()
            }
        )
        registerStopCleanup(
            Preferences.Market.HIDE_TAB_APP_ASSEMBLE.observe {
                showTabAppAssemble = !it
                checkConfig()
            }
        )
        registerStopCleanup(
            Preferences.Market.HIDE_TAB_MINI_GAME.observe {
                showTabMiniGame = !it
                checkConfig()
            }
        )
        registerStopCleanup(
            Preferences.Market.HIDE_TAB_MINE.observe {
                showTabMine = !it
                checkConfig()
            }
        )
        registerStopCleanup(
            Preferences.Market.HIDE_TAB_OTHERS.observe {
                showTabOthers = !it
                checkConfig()
            }
        )
        clzTabInfo?.apply {
            val tabFldTag = resolve().firstFieldOrNull {
                name = "tag"
                type = String::class
            }?.toTyped<String>()
            resolve().firstMethodOrNull {
                name = "fromJSON"
                parameterCount = 1
            }?.hook {
                val ori = proceed()
                val list = (ori as? List<*>)
                if (list != null) {
                    val filtered = list.filter {
                        val tag = tabFldTag?.get(it) ?: return@filter true
                        if (tag.startsWith("native_market_home")) !ignoreRestrict || showTabHome
                        else if (tag.startsWith("native_market_game")) showTabGame
                        else if (tag.startsWith("native_market_rank")) showTabRank
                        else if (tag.startsWith("native_market_agent")) showTabAgent
                        else if (
                            tag.startsWith("native_app_assemble") || tag.startsWith("native_market_video")
                        ) showTabAppAssemble
                        else if (tag.startsWith("native_market_quick_game")) showTabMiniGame
                        else if (tag.startsWith("native_market_mine")) !ignoreRestrict || showTabMine
                        else showTabOthers
                    }.toList()
                    result(filtered)
                } else {
                    result(ori)
                }
            }
        }
    }

    override fun onStop() {
        hideTabHooker?.unhook()
    }
}