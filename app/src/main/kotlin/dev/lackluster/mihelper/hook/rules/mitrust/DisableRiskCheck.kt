/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This file is part of XiaomiHelper project

 * This file references HyperCeiler <https://github.com/ReChronoRain/HyperCeiler/blob/main/app/src/main/java/com/sevtinge/hyperceiler/module/hook/trustservice/DisableMrm.java>
 * Copyright (C) 2023-2024 HyperCeiler Contributions

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.lackluster.mihelper.hook.rules.mitrust

import dev.lackluster.mihelper.data.preference.Preferences
import dev.lackluster.mihelper.hook.base.BaseHooker
import dev.lackluster.mihelper.hook.utils.DexKit
import dev.lackluster.mihelper.hook.utils.RemotePreferences.observe
import org.luckypray.dexkit.query.enums.StringMatchType

object DisableRiskCheck : BaseHooker() {
    override val supportsHotReload: Boolean = true
    override val supportsRuntimeToggle: Boolean = true

    private val metInitMrmService by lazy {
        DexKit.findMethodWithCache("init_mrm") {
            matcher {
                returnType = "boolean"
                addUsingString("MiTrustService/statusEventHandle", StringMatchType.Equals)
                addUsingString("vendor.xiaomi.hardware.mrm.IMrm/default", StringMatchType.Equals)
            }
        }
    }

    override fun onInit() {
        registerDisposeCleanup(
            Preferences.MiTrust.BLOCK_RISK_CHECK.observe {
                updateSelfState(it)
            }
        )
        metInitMrmService
    }

    override fun onStart() {
        metInitMrmService?.getMethodInstance(classLoader)?.hook {
            result(false)
        }
    }
}