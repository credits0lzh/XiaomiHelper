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

import android.view.View
import android.widget.RelativeLayout
import com.highcapable.kavaref.KavaRef.Companion.resolve
import dev.lackluster.mihelper.data.preference.Preferences
import dev.lackluster.mihelper.hook.base.BaseHooker
import dev.lackluster.mihelper.hook.utils.RemotePreferences.observe
import dev.lackluster.mihelper.hook.utils.toTyped

object HideHomepageTopBar : BaseHooker() {
    override val supportsHotReload: Boolean = true
    override val supportsRuntimeToggle: Boolean = true

    override fun onInit() {
        registerDisposeCleanup(
            Preferences.Browser.HIDE_HOMEPAGE_TOP_BAR.observe {
                updateSelfState(it)
            }
        )
    }

    override fun onStart() {
        "com.android.browser.homepage.SimpleVersionHomeLayout".toClassOrNull()?.apply {
            val fldRlSimplePageTopView = resolve().firstFieldOrNull {
                name = "mRlSimplePageTopView"
                type(RelativeLayout::class)
            }?.toTyped<RelativeLayout>()
            resolve().firstConstructor().hook {
                val ori = proceed()
                fldRlSimplePageTopView?.get(thisObject)?.apply {
                    visibility = View.INVISIBLE
                }
                fldRlSimplePageTopView?.set(thisObject, null)
                result(ori)
            }
        }
    }
}