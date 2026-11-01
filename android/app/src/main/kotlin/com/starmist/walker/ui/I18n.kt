package com.starmist.walker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/** The languages the app's own writing (menus, buttons, notes) comes in. The places, cards and facts are written in Chinese only, for now. */
enum class Lang { ZH, EN }

/** Which language to show, by the setting: 0 follows the phone (Chinese phones get Chinese, everything else English), 1 Chinese, 2 English. */
fun langOf(setting: Int, phoneLanguage: String): Lang = when (setting) {
    1 -> Lang.ZH
    2 -> Lang.EN
    else -> if (phoneLanguage == "zh") Lang.ZH else Lang.EN
}

internal val LocalLang = staticCompositionLocalOf { Lang.ZH }

/**
 * The writing of the app is Chinese in the code, and the English is looked up by the Chinese text: a text with numbers in it is looked up with each number
 * written as "#" ("今日 4,321 步" → "今日 # 步"), and one with names or words that change is written as a template with {0}, {1} ([trf]). A text with no
 * entry is shown as it is, in Chinese.
 */
object I18n {
    /** The language for the places that cannot ask the screen (messages made in the view model, notifications). */
    @Volatile var lang: Lang = Lang.ZH

    private val number = Regex("""\d[\d,]*(?:\.\d+)?""")

    fun tr(text: String, lang: Lang = this.lang): String {
        if (lang == Lang.ZH || text.isEmpty()) return text
        EN[text]?.let { return it }
        val numbers = number.findAll(text).map { it.value }.toList()
        if (numbers.isEmpty()) return text
        val key = number.replace(text, "#")
        val en = EN[key] ?: return text
        var i = 0
        return Regex("#").replace(en) { if (i < numbers.size) numbers[i++] else "#" }
    }

    /** The short form of a text for places with little room (the writing on a dial), where it has one; otherwise the same as [tr]. */
    fun trShort(text: String, lang: Lang = this.lang): String = if (lang == Lang.EN) SHORT[text] ?: tr(text, lang) else text

    private val SHORT = mapOf("今日步數" to "Steps")

    /** [template] with {0}, {1}… in it, in the language asked for, with [args] put in. */
    fun trf(template: String, vararg args: Any?, lang: Lang = this.lang): String {
        var s = if (lang == Lang.ZH) template else (EN[template] ?: template)
        args.forEachIndexed { i, a -> s = s.replace("{$i}", a.toString()) }
        return s
    }
}

@Composable
internal fun tr(text: String): String = I18n.tr(text, LocalLang.current)

@Composable
internal fun trf(template: String, vararg args: Any?): String = I18n.trf(template, *args, lang = LocalLang.current)

/** The app's writing in the language the user picked (or the phone's). */
@Composable
internal fun AppLanguage(lang: Lang, content: @Composable () -> Unit) {
    I18n.lang = lang
    CompositionLocalProvider(LocalLang provides lang) { content() }
}
