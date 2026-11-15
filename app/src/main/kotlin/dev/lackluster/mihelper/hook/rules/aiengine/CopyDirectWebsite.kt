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

package dev.lackluster.mihelper.hook.rules.aiengine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.condition.type.Modifiers
import dev.lackluster.mihelper.data.Scope
import dev.lackluster.mihelper.data.preference.Preferences
import dev.lackluster.mihelper.hook.base.BaseHooker
import dev.lackluster.mihelper.hook.utils.DexKit
import dev.lackluster.mihelper.hook.utils.RemotePreferences.observe
import dev.lackluster.mihelper.hook.utils.d
import dev.lackluster.mihelper.hook.utils.toTyped
import org.json.JSONArray
import org.json.JSONObject
import org.luckypray.dexkit.query.enums.StringMatchType

object CopyDirectWebsite : BaseHooker() {
    override val supportsHotReload: Boolean = true
    override val supportsRuntimeToggle: Boolean = true

    private val websiteIntent by lazy {
        DexKit.findMethodWithCache("aiengine_website_intent") {
            matcher {
                returnType = "android.content.Intent"
                paramCount = 1
                paramTypes("java.lang.String")
                addUsingString("clipboard_open", StringMatchType.Equals)
                addUsingString(Scope.BROWSER, StringMatchType.Equals)
            }
        }
    }
    private val metBubbleExecuteShow by lazy {
        DexKit.findMethodWithCache("bubble_execute_show") {
            matcher {
                addUsingString("BubbleWindowManager", StringMatchType.Equals)
                addUsingString("executeShow", StringMatchType.StartsWith)
                paramCount = 2
            }
        }
    }


    private val metGetIconUri by lazy {
        DexKit.findMethodWithCache("get_icon_uri") {
            matcher {
                addUsingString("/action_copy_direct_icon/", StringMatchType.Equals)
                paramTypes("android.content.Context", "int")
                returnType("java.lang.String")
            }
        }
    }

    override fun onInit() {
        registerDisposeCleanup(
            Preferences.AiEngine.OPEN_LINK_WITH_CUSTOM_BROWSER.observe {
                updateSelfState(it)
            }
        )
        websiteIntent
        metBubbleExecuteShow

        metGetIconUri
    }

    override fun onStart() {
        websiteIntent?.getMethodInstance(classLoader)?.hook {
            val url = getArg(0) as? String ?: return@hook result(proceed())
            val normalizedUrl =
                if (url.startsWith("http://") || url.startsWith("https://")) url
                else "https://$url"
            result(
                Intent(Intent.ACTION_VIEW, normalizedUrl.toUri()).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("open_source", "clipboard_open")
                }
            )
        }
        val clzCueData = "com.xiaomi.ai.bubble.core.model.CueData".toClassOrNull()
        val fldCueId = clzCueData?.resolve()?.firstFieldOrNull {
            name = "cueId"
            type(String::class)
        }?.toTyped<String>()
        val metToJson = clzCueData?.resolve()?.firstMethodOrNull {
            name = "toJson"
            returnType(String::class)
        }?.toTyped<String>()
        val metFromJson = clzCueData?.resolve()?.firstMethodOrNull {
            name = "fromJson"
            parameters(String::class)
            modifiers(Modifiers.STATIC)
        }?.toTyped<Any>()
        if (fldCueId != null && metToJson != null && metFromJson != null) {
            metBubbleExecuteShow?.getMethodInstance(classLoader)?.apply {
                val fldContext = this.declaringClass.resolve().firstFieldOrNull {
                    type(Context::class)
                }?.toTyped<Context>()
                hook {
                    val newArgs = args.toTypedArray()
                    val list = (newArgs[0] as? List<*>)
                    val context = fldContext?.get(thisObject)
                    if (list == null || context == null) {
                        return@hook result(proceed())
                    }
                    var defaultBrowserPkg: String? = null
                    var defaultBrowserName: String? = null
                    newArgs[0] = list.mapNotNull {
                        if (
                            it != null && clzCueData.isInstance(it) &&
                            fldCueId.get(it)?.startsWith("cue.copy_text_jump_app#hce.copy_text_jump_app.jump_url#") == true
                        ) {
                            val jsonString = metToJson.invoke(it)
                            val updated = jsonString?.let { source ->
                                val pkg = defaultBrowserPkg
                                    ?: listOfNotNull(
                                        defaultBrowserPackage(context),
                                        Scope.BROWSER
                                    ).distinct().first().also { it1 ->
                                        defaultBrowserPkg = it1
                                    }
                                val name = defaultBrowserName
                                    ?: context.packageManager.getApplicationInfo(pkg, 0).let { it1 ->
                                        context.packageManager.getApplicationLabel(it1).toString().also { it2 ->
                                            defaultBrowserName = it2
                                        }
                                    }
                                val jsonObject = JSONObject(source)
                                val display = jsonObject.getJSONObject("display")
                                val briefCard = display.getJSONObject("briefCard")
                                val startIcon = briefCard.getJSONObject("startIcon")
                                startIcon.put("type", "application")
                                startIcon.put("value", pkg)
                                val title = briefCard.getJSONObject("title")
                                title.put("text", "在${name}中打开")
                                title.put(
                                    "highlights",
                                    JSONArray().put(
                                        JSONObject()
                                            .put("start", 1)
                                            .put("end", 1 + name.length)
                                    )
                                )
                                display.put("targetPackage", pkg)
                                metFromJson.invoke(null, jsonObject.toString())
                            }
                            updated ?: it
                        } else {
                            it
                        }
                    }.also {
                        d { "CueData=${it.joinToString{ it1 -> metToJson.invoke(it1).toString() }}" }
                    }
                    result(proceed(newArgs))
                }
            }
        }
    }

    private fun defaultBrowserPackage(context: Context): String? {
        return context.packageManager.resolveActivity(
            Intent(Intent.ACTION_VIEW, "https://example.com".toUri()),
            PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.packageName?.takeUnless { it == "android" }
    }
}