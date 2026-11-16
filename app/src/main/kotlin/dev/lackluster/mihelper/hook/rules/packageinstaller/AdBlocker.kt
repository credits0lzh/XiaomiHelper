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

package dev.lackluster.mihelper.hook.rules.packageinstaller

import android.annotation.SuppressLint
import com.highcapable.kavaref.KavaRef.Companion.resolve
import dev.lackluster.mihelper.data.preference.Preferences
import dev.lackluster.mihelper.hook.base.BaseHooker
import dev.lackluster.mihelper.hook.utils.DexKit
import dev.lackluster.mihelper.hook.utils.RemotePreferences.observe
import dev.lackluster.mihelper.hook.utils.toTyped
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Modifier

object AdBlocker : BaseHooker() {
    override val supportsHotReload: Boolean = true
    override val supportsRuntimeToggle: Boolean = true

    private val clzCloudParams by "com.miui.packageInstaller.model.CloudParams".lazyClassOrNull()
    private val ctorCloudParams by lazy {
        clzCloudParams?.resolve()?.firstConstructor()
    }
    private val cloudParamsMethod by lazy {
        DexKit.findMethodWithCache("cloud_params") {
            matcher {
                addUsingString("apkSignature3Sha256", StringMatchType.Equals)
            }
        }
    }
    private val ctorCloudResultSuccess by lazy {
        $$"com.miui.packageInstaller.model.CloudResult$Success".toClassOrNull()
            ?.resolve()?.firstConstructorOrNull {
                parameters("com.miui.packageInstaller.model.CloudParams")
            }?.toTyped()
    }
    private val adsEnableMethod by lazy {
        DexKit.findMethodWithCache("ads_enable") {
            matcher {
                addUsingString("ads_enable", StringMatchType.Equals)
                returnType = "boolean"
            }
        }
    }
    private val appStoreRecommendMethod by lazy {
        DexKit.findMethodWithCache("app_store_recommend") {
            matcher {
                addUsingString("app_store_recommend", StringMatchType.Equals)
                returnType = "boolean"
            }
        }
    }
    private val isPersonalizedAdEnabledMethod by lazy {
        DexKit.findMethodWithCache("personalized_ad_enable") {
            matcher {
                addUsingString($$"android.provider.MiuiSettings$Ad", StringMatchType.Equals)
                returnType = "boolean"
            }
        }
    }
    private val virusScanInstallMethod by lazy {
        DexKit.findMethodWithCache("virus_scan_install") {
            matcher {
                addUsingString("virus_scan_install", StringMatchType.Equals)
                returnType = "boolean"
            }
        }
    }
    private val miuiSafeMode by lazy {
        DexKit.findMethodWithCache("miui_safe_mode") {
            matcher {
                addUsingString("miui_safe_mode", StringMatchType.Equals)
                addUsingString("safe_mode_is_open_cloud_config", StringMatchType.Equals)
                returnType = "boolean"
            }
        }
    }
    // risk check
    private val verifyEnable by lazy {
        DexKit.findMethodWithCache("secure_verify_enable") {
            matcher {
                addUsingString("secure_verify_enable", StringMatchType.Equals)
                returnType = "boolean"
            }
        }
    }
    private val openSafeMode by lazy {
        DexKit.findMethodWithCache("open_safety_model") {
            matcher {
                addUsingString("installerOpenSafetyModel", StringMatchType.Equals)
                returnType = "boolean"
            }
        }
    }
    private val closeSafeMode by lazy {
        DexKit.findMethodWithCache("close_safety_model") {
            matcher {
                addUsingString("installerCloseSafetyModel", StringMatchType.Equals)
                returnType = "boolean"
            }
        }
    }
    private val singleAuth by lazy {
        DexKit.findMethodWithCache("single_auth") {
            matcher {
                addUsingString("installerSingleAuth", StringMatchType.Equals)
                returnType = "boolean"
            }
        }
    }
    // upload applications

    private val layoutMethod by lazy {
        DexKit.findMethodsWithCache("layout_method") {
            matcher {
                paramCount = 7
                paramTypes = listOf("java.lang.String", "java.lang.String", "java.lang.String", "java.lang.Integer", "java.lang.String", "java.lang.String", null)
            }
        }.filter { !it.isConstructor }
    }
    private val reportMethod by lazy {
        DexKit.findMethodsWithCache("report_method") {
            matcher {
                addUsingString("appSourcepackageName", StringMatchType.Equals)
                addUsingString($$"$apkInfo", StringMatchType.Equals)
                modifiers = Modifier.STATIC
            }
        }
    }
    private val metCloudFetchTime by lazy {
        DexKit.findMethodWithCache("cloud_data_fetch_time") {
            matcher {
                addUsingString("cloud_data_fetch_time", StringMatchType.Equals)
                paramCount = 0
                returnType = "long"
            }
        }
    }

    override fun onInit() {
        registerDisposeCleanup(
            Preferences.PackageInstaller.REMOVE_ELEMENT.observe {
                updateSelfState(it)
            }
        )
        adsEnableMethod
        appStoreRecommendMethod
        isPersonalizedAdEnabledMethod
        virusScanInstallMethod
        miuiSafeMode
        cloudParamsMethod

        verifyEnable
        openSafeMode
        closeSafeMode
        singleAuth

        layoutMethod
        reportMethod
        metCloudFetchTime
    }

    @SuppressLint("DiscouragedApi")
    override fun onStart() {
        adsEnableMethod?.getMethodInstance(classLoader)?.hook {
            result(false)
        }
        appStoreRecommendMethod?.getMethodInstance(classLoader)?.hook {
            result(false)
        }
        isPersonalizedAdEnabledMethod?.getMethodInstance(classLoader)?.hook {
            result(false)
        }
        virusScanInstallMethod?.getMethodInstance(classLoader)?.hook {
            result(false)
        }
        miuiSafeMode?.getMethodInstance(classLoader)?.hook {
            result(false)
        }
        cloudParamsMethod?.getMethodInstance(classLoader)?.hook {
            val cloudParams = ctorCloudParams?.copy()?.create()
            val cloudResult = cloudParams?.let { ctorCloudResultSuccess?.newInstance(it) }
            result(cloudResult)
        }
        clzCloudParams?.apply {
            val installNotAllow = resolve().firstFieldOrNull {
                name = "installNotAllow"
            }?.toTyped<Boolean>()
            val showSafeModeTip = resolve().firstFieldOrNull {
                name = "showSafeModeTip"
            }?.toTyped<Boolean>()
            val showAdsBefore = resolve().firstFieldOrNull {
                name = "showAdsBefore"
            }?.toTyped<Boolean>()
            val showAdsAfter = resolve().firstFieldOrNull {
                name = "showAdsAfter"
            }?.toTyped<Boolean>()
            val useSystemAppRules = resolve().firstFieldOrNull {
                name = "useSystemAppRules"
            }?.toTyped<Boolean>()
            val registrationStatus = resolve().firstFieldOrNull {
                name = "registrationStatus"
            }?.toTyped<Int>()
            ctorCloudParams?.self?.hook {
                val ori = proceed()
                installNotAllow?.set(thisObject, false)
                showSafeModeTip?.set(thisObject, false)
                showAdsBefore?.set(thisObject, false)
                showAdsAfter?.set(thisObject, false)
                useSystemAppRules?.set(thisObject, true)
                registrationStatus?.set(thisObject, 2)
                result(ori)
            }
            resolve().firstMethodOrNull {
                name = "getAppRegisterScene"
            }?.hook {
                result("registered")
            }
            resolve().firstMethodOrNull {
                name = "isMarketApp"
            }?.hook {
                result(false)
            }
            resolve().firstMethodOrNull {
                name = "isMarketApp64NotInstallAllow"
            }?.hook {
                result(false)
            }
            resolve().firstMethodOrNull {
                name = "isProhibitInstalling"
            }?.hook {
                result(false)
            }
            resolve().firstMethodOrNull {
                name = "isAreaForbidden"
            }?.hook {
                result(false)
            }
            resolve().firstMethodOrNull {
                name = "isNewUnregistered"
            }?.hook {
                result(false)
            }
            resolve().firstMethodOrNull {
                name = "isUnrecorded"
            }?.hook {
                result(false)
            }
        }
        // count check
        "com.miui.packageInstaller.model.RiskControlRules".toClassOrNull()?.resolve()?.firstMethodOrNull {
            name = "getCurrentLevel"
        }?.hook {
            result(0)
        }
        // risk check
        verifyEnable?.getMethodInstance(classLoader)?.hook {
            result(false)
        }
        openSafeMode?.getMethodInstance(classLoader)?.hook {
            result(false)
        }
        closeSafeMode?.getMethodInstance(classLoader)?.hook {
            result(false)
        }
        singleAuth?.getMethodInstance(classLoader)?.hook {
            result(false)
        }
        // upload applications
        layoutMethod.map {
            it.getMethodInstance(classLoader)
        }.hookAll {
            result(null)
        }
        reportMethod.map {
            it.getMethodInstance(classLoader)
        }.hookAll {
            result(null)
        }
        metCloudFetchTime?.getMethodInstance(classLoader)?.hook {
            result(Long.MAX_VALUE)
        }
    }
}