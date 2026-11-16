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

package dev.lackluster.mihelper.hook.rules.browser

import com.highcapable.kavaref.KavaRef.Companion.resolve
import dev.lackluster.mihelper.data.preference.Preferences
import dev.lackluster.mihelper.hook.base.BaseHooker
import dev.lackluster.mihelper.hook.utils.DexKit
import dev.lackluster.mihelper.hook.utils.RemotePreferences.observe
import org.luckypray.dexkit.query.enums.StringMatchType

object Configuration : BaseHooker() {
    override val supportsHotReload: Boolean = true
    override val supportsRuntimeToggle: Boolean = true

    @Volatile
    private var showSugSwitch = false
    @Volatile
    private var blockDialog = false
    @Volatile
    private var adBlocker = false

    private val clzConfigCenter by lazy {
        DexKit.withBridge {
            getClassData("miui.browser.util.prefs.KvPrefs")
        }?.let {
            listOf(it)
        } ?: emptyList()
    }
    private val metPrefShowSugSwitchView by lazy {
        DexKit.findMethodWithCache("pref_show_sug_switch_view") {
            matcher {
                addUsingString("pref_show_sug_switch_view", StringMatchType.Equals)
                paramCount = 0
            }
            searchInClass(clzConfigCenter)
        }
    }
    private val metRecPopupCardSwitch by lazy {
        DexKit.findMethodWithCache("rec_popup_card_switch") {
            matcher {
                addUsingString("rec_popup_card_switch", StringMatchType.Equals)
                paramCount = 0
            }
            searchInClass(clzConfigCenter)
        }
    }
    private val metPrefPushPopDialog by lazy {
        DexKit.findMethodWithCache("pref_push_pop_dialog") {
            matcher {
                addUsingString("pref_push_pop_dialog", StringMatchType.Equals)
                addUsingString("activity", StringMatchType.Equals)
            }
        }
    }
    private val metDefaultPageRealRimeHotSpotSwitch by lazy {
        DexKit.findMethodWithCache("default_page_real_time_hot_spot_switch") {
            matcher {
                addUsingString("default_page_real_time_hot_spot_switch", StringMatchType.Equals)
                paramCount = 0
            }
            searchInClass(clzConfigCenter)
        }
    }
    private val metDefaultPageGuessYouWantSwitch by lazy {
        DexKit.findMethodWithCache("default_page_guess_you_want_switch") {
            matcher {
                addUsingString("default_page_guess_you_want_switch", StringMatchType.Equals)
                paramCount = 0
            }
            searchInClass(clzConfigCenter)
        }
    }
    private val metAdAppDownloadExitSwitch by lazy {
        DexKit.findMethodWithCache("ad_app_download_exit_switch") {
            matcher {
                addUsingString("ad_app_download_exit_switch", StringMatchType.Equals)
                paramCount = 0
            }
            searchInClass(clzConfigCenter)
        }
    }
    private val metAdAppDownloadPushSwitch by lazy {
        DexKit.findMethodWithCache("ad_app_download_push_switch") {
            matcher {
                addUsingString("ad_app_download_push_switch", StringMatchType.Equals)
                paramCount = 0
            }
            searchInClass(clzConfigCenter)
        }
    }
    private val metAdAppDownloadHomeSwitch by lazy {
        DexKit.findMethodWithCache("ad_app_download_home_switch") {
            matcher {
                addUsingString("ad_app_download_home_switch", StringMatchType.Equals)
                paramCount = 0
            }
            searchInClass(clzConfigCenter)
        }
    }

    override fun onInit() {
        updateSelfState(showSugSwitch || blockDialog || adBlocker)
        registerDisposeCleanup(
            Preferences.Browser.SHOW_SUG_SWITCH_ENTRY.observe {
                showSugSwitch = it
                updateSelfState(showSugSwitch || blockDialog || adBlocker)
            },
        )
        registerDisposeCleanup(
            Preferences.Browser.BLOCK_DIALOG.observe {
                blockDialog = it
                updateSelfState(showSugSwitch || blockDialog || adBlocker)
            }
        )
        registerDisposeCleanup(
            Preferences.Browser.AD_BLOCKER.observe {
                adBlocker = it
                updateSelfState(showSugSwitch || blockDialog || adBlocker)
            }
        )
        // showSugSwitch
        metPrefShowSugSwitchView
        // blockDialog
        metRecPopupCardSwitch
        metPrefPushPopDialog
        // adBlocker
        metDefaultPageRealRimeHotSpotSwitch
        metDefaultPageGuessYouWantSwitch
        metAdAppDownloadExitSwitch
        metAdAppDownloadPushSwitch
        metAdAppDownloadHomeSwitch

    }

    override fun onStart() {
        // showSugSwitch
        metPrefShowSugSwitchView?.getMethodInstance(classLoader)?.hook {
            if (showSugSwitch) {
                result(true)
            } else {
                result(proceed())
            }
        }
        // blockDialog
        metRecPopupCardSwitch?.getMethodInstance(classLoader)?.hook {
            if (blockDialog) {
                result(false)
            } else {
                result(proceed())
            }
        }
        metPrefPushPopDialog?.getMethodInstance(classLoader)?.hook {
            if (blockDialog) {
                result(null)
            } else {
                result(proceed())
            }
        }
        // adBlocker
        metDefaultPageRealRimeHotSpotSwitch?.getMethodInstance(classLoader)?.hook {
            if (adBlocker) {
                result(false)
            } else {
                result(proceed())
            }
        }
        metDefaultPageGuessYouWantSwitch?.getMethodInstance(classLoader)?.hook {
            if (adBlocker) {
                result(false)
            } else {
                result(proceed())
            }
        }
        metAdAppDownloadExitSwitch?.getMethodInstance(classLoader)?.hook {
            if (adBlocker) {
                result(false)
            } else {
                result(proceed())
            }
        }
        metAdAppDownloadPushSwitch?.getMethodInstance(classLoader)?.hook {
            if (adBlocker) {
                result(false)
            } else {
                result(proceed())
            }
        }
        metAdAppDownloadHomeSwitch?.getMethodInstance(classLoader)?.hook {
            if (adBlocker) {
                result(false)
            } else {
                result(proceed())
            }
        }
        "com.android.browser.ad.AdAppDownloadManager".toClassOrNull()?.apply {
            resolve().firstMethodOrNull {
                name = "check2Show"
            }?.hook {
                if (adBlocker) {
                    result(false)
                } else {
                    result(proceed())
                }
            }
        }
    }
}