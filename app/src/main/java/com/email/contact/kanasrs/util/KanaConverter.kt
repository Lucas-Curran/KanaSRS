package com.email.contact.kanasrs.util

import java.util.*
import java.util.regex.Pattern
import kotlin.math.min

/**
 * Copyright (c) 2013 Matthew Miller
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * From: https://github.com/MasterKale/WanaKanaJava
 *
 * Class has been converted to kotlin
 */
class KanaConverter(useObsoleteKana: Boolean) {
    var mOptions = HashMap<String, Boolean>()
    var mRtoJ = HashMap<String, String>()
    var mJtoR = HashMap<String, String>()

    private interface Command {
        fun run(str: String): Boolean
    }

    // Pass every character of a string through a function and return TRUE if every character passes the function's check
    private fun allTrue(checkStr: String, func: Command): Boolean {
        for (element in checkStr) {
            if (!func.run(element.toString())) {
                return false
            }
        }
        return true
    }

    // Check if a character is within a Unicode range
    private fun isCharInRange(chr: Char, start: Int, end: Int): Boolean {
        val code = chr.code
        return code in start..end
    }

    private fun isCharVowel(chr: Char, includeY: Boolean): Boolean {
        val regexp = if (includeY) Pattern.compile("[aeiouy]") else Pattern.compile("[aeiou]")
        val matcher = regexp.matcher(chr.toString())
        return matcher.find()
    }

    private fun isCharConsonant(chr: Char, includeY: Boolean): Boolean {
        val regexp =
            if (includeY) Pattern.compile("[bcdfghjklmnpqrstvwxyz]") else Pattern.compile("[bcdfghjklmnpqrstvwxz]")
        val matcher = regexp.matcher(chr.toString())
        return matcher.find()
    }

    private fun isCharKatakana(chr: Char): Boolean {
        return isCharInRange(chr, KATAKANA_START, KATAKANA_END)
    }

    private fun isCharHiragana(chr: Char): Boolean {
        return isCharInRange(chr, HIRAGANA_START, HIRAGANA_END)
    }

    private fun isCharKana(chr: Char): Boolean {
        return isCharHiragana(chr) || isCharKatakana(chr)
    }

    private fun katakanaToHiragana(kata: String): String {
        var code: Int
        var hira = ""
        for (element in kata) {
            if (isCharKatakana(element)) {
                code = element.code
                code += HIRAGANA_START - KATAKANA_START
                hira += String(Character.toChars(code))
            } else {
                hira += element
            }
        }
        return hira
    }

    private fun hiraganaToKatakana(hira: String?): String {
        var code: Int
        var kata = ""
        for (_i in 0 until hira!!.length) {
            val hiraChar = hira[_i]
            if (isCharHiragana(hiraChar)) {
                code = hiraChar.code
                code += KATAKANA_START - HIRAGANA_START
                kata += String(Character.toChars(code))
            } else {
                kata += hiraChar
            }
        }
        return kata
    }

    fun hiraganaToRomaji(hira: String): String? {
        if (isRomaji(hira)) {
            return hira
        }
        var chunk = ""
        var chunkSize: Int
        var cursor = 0
        val len = hira.length
        val maxChunk = 2
        var nextCharIsDoubleConsonant = false
        var roma: String? = ""
        var romaChar: String? = null
        while (cursor < len) {
            chunkSize = min(maxChunk, len - cursor)
            while (chunkSize > 0) {
                chunk = hira.substring(cursor, cursor + chunkSize)
                if (isKatakana(chunk)) {
                    chunk = katakanaToHiragana(chunk)
                }
                if (chunk[0].toString() == "???" && chunkSize == 1 && cursor < len - 1) {
                    nextCharIsDoubleConsonant = true
                    romaChar = ""
                    break
                }
                romaChar = mJtoR[chunk]
                if (romaChar != null && nextCharIsDoubleConsonant) {
                    romaChar = romaChar[0].toString() + romaChar
                    nextCharIsDoubleConsonant = false
                }
                if (romaChar != null) {
                    break
                }
                chunkSize--
            }
            if (romaChar == null) {
                romaChar = chunk
            }
            roma += romaChar
            cursor += if (chunkSize > 0) chunkSize else 1
        }
        return roma
    }

    private fun romajiToHiragana(roma: String): String? {
        return romajiToKana(roma, true)
    }

    private fun romajiToKana(roma: String, ignoreCase: Boolean): String? {
        var ignoreCase: Boolean? = ignoreCase
        var chunk = ""
        var chunkLC = ""
        var chunkSize: Int
        var position = 0
        val len = roma.length
        val maxChunk = 3
        var kana: String? = ""
        var kanaChar: String? = ""
        if (ignoreCase == null) {
            ignoreCase = false
        }
        while (position < len) {
            chunkSize = min(maxChunk, len - position)
            while (chunkSize > 0) {
                chunk = roma.substring(position, position + chunkSize)
                chunkLC = chunk.lowercase(Locale.getDefault())
                if ((chunkLC == "lts" || chunkLC == "xts") && len - position >= 4) {
                    chunkSize++
                    // The second parameter in substring() is an end point, not a length!
                    chunk = roma.substring(position, position + chunkSize)
                    chunkLC = chunk.lowercase(Locale.getDefault())
                }
                if (chunkLC[0].toString() == "n") {
                    // Convert n' to ???
                    if (mOptions[OPTION_IME_MODE]!! && chunk.length == 2 && chunkLC[1].toString() == "'") {
                        chunkSize = 2
                        chunk = "nn"
                        chunkLC = chunk.lowercase(Locale.getDefault())
                    }
                    // If the user types "nto", automatically convert "n" to "???" first
                    // "y" is excluded from the list of consonants so we can still get ??????, ??????, and ??????
                    if (chunk.length > 2 && isCharConsonant(chunkLC[1], false) && isCharVowel(
                            chunkLC[2], true
                        )
                    ) {
                        chunkSize = 1
                        // I removed the "n"->"???" mapping because the IME wouldn't let me type "na" for "???" without returning "??????",
                        // so the chunk needs to be manually set to a value that will map to "???"
                        chunk = "nn"
                        chunkLC = chunk.lowercase(Locale.getDefault())
                    }
                }

                // Prepare to return a small-??? because we're looking at double-consonants.
                if (chunk.length > 1 && chunkLC[0].toString() != "n" && isCharConsonant(
                        chunkLC[0],
                        true
                    ) && chunk[0] == chunk[1]
                ) {
                    chunkSize = 1
                    // Return a small katakana ??? when typing in uppercase
                    if (isCharInRange(chunk[0], UPPERCASE_START, UPPERCASE_END)) {
                        chunk = "???"
                        chunkLC = chunk
                    } else {
                        chunk = "???"
                        chunkLC = chunk
                    }
                }
                kanaChar = mRtoJ[chunkLC]
                if (kanaChar != null) {
                    break
                }
                chunkSize--
            }
            if (kanaChar == null) {
                chunk = convertPunctuation(chunk[0].toString())
                kanaChar = chunk
            }
            if (mOptions[OPTION_USE_OBSOLETE_KANA]!!) {
                if (chunkLC == "wi") {
                    kanaChar = "???"
                }
                if (chunkLC == "we") {
                    kanaChar = "???"
                }
            }
            if (roma.length > position + 1 && mOptions[OPTION_IME_MODE]!! && chunkLC[0].toString() == "n") {
                if (roma[position + 1].toString()
                        .lowercase(Locale.getDefault()) == "y" && position == len - 2 || position == len - 1
                ) {
                    kanaChar = chunk[0].toString()
                }
            }
            if (!ignoreCase) {
                if (isCharInRange(chunk[0], UPPERCASE_START, UPPERCASE_END)) {
                    kanaChar = hiraganaToKatakana(kanaChar)
                }
            }
            kana += kanaChar
            position += if (chunkSize > 0) chunkSize else 1
        }
        return kana
    }

    private fun convertPunctuation(input: String): String {
        if (input == '???'.toString()) {
            return ' '.toString()
        }
        return if (input == '-'.toString()) {
            '???'.toString()
        } else input
    }

    /**
     * Returns true if input is entirely hiragana.
     */
    fun isHiragana(input: String): Boolean {
        return allTrue(input, object : Command {
            override fun run(str: String): Boolean {
                return isCharHiragana(str[0])
            }
        })
    }

    fun isKatakana(input: String): Boolean {
        return allTrue(input, object : Command {
            override fun run(str: String): Boolean {
                return isCharKatakana(str[0])
            }
        })
    }

    fun isKana(input: String): Boolean {
        return allTrue(input, object : Command {
            override fun run(str: String): Boolean {
                return isHiragana(str) || isKatakana(str)
            }
        })
    }

    fun isRomaji(input: String): Boolean {
        return allTrue(input, object : Command {
            override fun run(str: String): Boolean {
                return !isHiragana(str) && !isKatakana(str)
            }
        })
    }

    fun toHiragana(input: String): String? {
        if (isRomaji(input)) {
            return romajiToHiragana(input)
        }
        return if (isKatakana(input)) {
            katakanaToHiragana(input)
        } else input
    }

    fun toKatakana(input: String): String {
        if (isHiragana(input)) {
            return hiraganaToKatakana(input)
        }
        return if (isRomaji(input)) {
            hiraganaToKatakana(romajiToHiragana(input))
        } else input
    }

    fun toKana(input: String): String? {
        return romajiToKana(input, false)
    }

    fun toRomaji(input: String): String? {
        return hiraganaToRomaji(input)
    }

    private fun prepareRtoJ() {
        mRtoJ["a"] = "???"
        mRtoJ["i"] = "???"
        mRtoJ["u"] = "???"
        mRtoJ["e"] = "???"
        mRtoJ["o"] = "???"
        mRtoJ["yi"] = "???"
        mRtoJ["wu"] = "???"
        mRtoJ["whu"] = "???"
        mRtoJ["xa"] = "???"
        mRtoJ["xi"] = "???"
        mRtoJ["xu"] = "???"
        mRtoJ["xe"] = "???"
        mRtoJ["xo"] = "???"
        mRtoJ["xyi"] = "???"
        mRtoJ["xye"] = "???"
        mRtoJ["ye"] = "??????"
        mRtoJ["wha"] = "??????"
        mRtoJ["whi"] = "??????"
        mRtoJ["whe"] = "??????"
        mRtoJ["who"] = "??????"
        mRtoJ["wi"] = "??????"
        mRtoJ["we"] = "??????"
        mRtoJ["va"] = "??????"
        mRtoJ["vi"] = "??????"
        mRtoJ["vu"] = "???"
        mRtoJ["ve"] = "??????"
        mRtoJ["vo"] = "??????"
        mRtoJ["vya"] = "??????"
        mRtoJ["vyi"] = "??????"
        mRtoJ["vyu"] = "??????"
        mRtoJ["vye"] = "??????"
        mRtoJ["vyo"] = "??????"
        mRtoJ["ka"] = "???"
        mRtoJ["ki"] = "???"
        mRtoJ["ku"] = "???"
        mRtoJ["ke"] = "???"
        mRtoJ["ko"] = "???"
        mRtoJ["lka"] = "???"
        mRtoJ["lke"] = "???"
        mRtoJ["xka"] = "???"
        mRtoJ["xke"] = "???"
        mRtoJ["kya"] = "??????"
        mRtoJ["kyi"] = "??????"
        mRtoJ["kyu"] = "??????"
        mRtoJ["kye"] = "??????"
        mRtoJ["kyo"] = "??????"
        mRtoJ["qya"] = "??????"
        mRtoJ["qyu"] = "??????"
        mRtoJ["qyo"] = "??????"
        mRtoJ["qwa"] = "??????"
        mRtoJ["qwi"] = "??????"
        mRtoJ["qwu"] = "??????"
        mRtoJ["qwe"] = "??????"
        mRtoJ["qwo"] = "??????"
        mRtoJ["qa"] = "??????"
        mRtoJ["qi"] = "??????"
        mRtoJ["qe"] = "??????"
        mRtoJ["qo"] = "??????"
        mRtoJ["kwa"] = "??????"
        mRtoJ["qyi"] = "??????"
        mRtoJ["qye"] = "??????"
        mRtoJ["ga"] = "???"
        mRtoJ["gi"] = "???"
        mRtoJ["gu"] = "???"
        mRtoJ["ge"] = "???"
        mRtoJ["go"] = "???"
        mRtoJ["gya"] = "??????"
        mRtoJ["gyi"] = "??????"
        mRtoJ["gyu"] = "??????"
        mRtoJ["gye"] = "??????"
        mRtoJ["gyo"] = "??????"
        mRtoJ["gwa"] = "??????"
        mRtoJ["gwi"] = "??????"
        mRtoJ["gwu"] = "??????"
        mRtoJ["gwe"] = "??????"
        mRtoJ["gwo"] = "??????"
        mRtoJ["sa"] = "???"
        mRtoJ["si"] = "???"
        mRtoJ["shi"] = "???"
        mRtoJ["su"] = "???"
        mRtoJ["se"] = "???"
        mRtoJ["so"] = "???"
        mRtoJ["za"] = "???"
        mRtoJ["zi"] = "???"
        mRtoJ["zu"] = "???"
        mRtoJ["ze"] = "???"
        mRtoJ["zo"] = "???"
        mRtoJ["ji"] = "???"
        mRtoJ["sya"] = "??????"
        mRtoJ["syi"] = "??????"
        mRtoJ["syu"] = "??????"
        mRtoJ["sye"] = "??????"
        mRtoJ["syo"] = "??????"
        mRtoJ["sha"] = "??????"
        mRtoJ["shu"] = "??????"
        mRtoJ["she"] = "??????"
        mRtoJ["sho"] = "??????"
        mRtoJ["swa"] = "??????"
        mRtoJ["swi"] = "??????"
        mRtoJ["swu"] = "??????"
        mRtoJ["swe"] = "??????"
        mRtoJ["swo"] = "??????"
        mRtoJ["zya"] = "??????"
        mRtoJ["zyi"] = "??????"
        mRtoJ["zyu"] = "??????"
        mRtoJ["zye"] = "??????"
        mRtoJ["zyo"] = "??????"
        mRtoJ["ja"] = "??????"
        mRtoJ["ju"] = "??????"
        mRtoJ["je"] = "??????"
        mRtoJ["jo"] = "??????"
        mRtoJ["jya"] = "??????"
        mRtoJ["jyi"] = "??????"
        mRtoJ["jyu"] = "??????"
        mRtoJ["jye"] = "??????"
        mRtoJ["jyo"] = "??????"
        mRtoJ["ta"] = "???"
        mRtoJ["ti"] = "???"
        mRtoJ["tu"] = "???"
        mRtoJ["te"] = "???"
        mRtoJ["to"] = "???"
        mRtoJ["chi"] = "???"
        mRtoJ["tsu"] = "???"
        mRtoJ["ltu"] = "???"
        mRtoJ["xtu"] = "???"
        mRtoJ["tya"] = "??????"
        mRtoJ["tyi"] = "??????"
        mRtoJ["tyu"] = "??????"
        mRtoJ["tye"] = "??????"
        mRtoJ["tyo"] = "??????"
        mRtoJ["cha"] = "??????"
        mRtoJ["chu"] = "??????"
        mRtoJ["che"] = "??????"
        mRtoJ["cho"] = "??????"
        mRtoJ["cya"] = "??????"
        mRtoJ["cyi"] = "??????"
        mRtoJ["cyu"] = "??????"
        mRtoJ["cye"] = "??????"
        mRtoJ["cyo"] = "??????"
        mRtoJ["tsa"] = "??????"
        mRtoJ["tsi"] = "??????"
        mRtoJ["tse"] = "??????"
        mRtoJ["tso"] = "??????"
        mRtoJ["tha"] = "??????"
        mRtoJ["thi"] = "??????"
        mRtoJ["thu"] = "??????"
        mRtoJ["the"] = "??????"
        mRtoJ["tho"] = "??????"
        mRtoJ["twa"] = "??????"
        mRtoJ["twi"] = "??????"
        mRtoJ["twu"] = "??????"
        mRtoJ["twe"] = "??????"
        mRtoJ["two"] = "??????"
        mRtoJ["da"] = "???"
        mRtoJ["di"] = "???"
        mRtoJ["du"] = "???"
        mRtoJ["de"] = "???"
        mRtoJ["do"] = "???"
        mRtoJ["dya"] = "??????"
        mRtoJ["dyi"] = "??????"
        mRtoJ["dyu"] = "??????"
        mRtoJ["dye"] = "??????"
        mRtoJ["dyo"] = "??????"
        mRtoJ["dha"] = "??????"
        mRtoJ["dhi"] = "??????"
        mRtoJ["dhu"] = "??????"
        mRtoJ["dhe"] = "??????"
        mRtoJ["dho"] = "??????"
        mRtoJ["dwa"] = "??????"
        mRtoJ["dwi"] = "??????"
        mRtoJ["dwu"] = "??????"
        mRtoJ["dwe"] = "??????"
        mRtoJ["dwo"] = "??????"
        mRtoJ["na"] = "???"
        mRtoJ["ni"] = "???"
        mRtoJ["nu"] = "???"
        mRtoJ["ne"] = "???"
        mRtoJ["no"] = "???"
        mRtoJ["nya"] = "??????"
        mRtoJ["nyi"] = "??????"
        mRtoJ["nyu"] = "??????"
        mRtoJ["nye"] = "??????"
        mRtoJ["nyo"] = "??????"
        mRtoJ["ha"] = "???"
        mRtoJ["hi"] = "???"
        mRtoJ["hu"] = "???"
        mRtoJ["he"] = "???"
        mRtoJ["ho"] = "???"
        mRtoJ["fu"] = "???"
        mRtoJ["hya"] = "??????"
        mRtoJ["hyi"] = "??????"
        mRtoJ["hyu"] = "??????"
        mRtoJ["hye"] = "??????"
        mRtoJ["hyo"] = "??????"
        mRtoJ["fya"] = "??????"
        mRtoJ["fyu"] = "??????"
        mRtoJ["fyo"] = "??????"
        mRtoJ["fwa"] = "??????"
        mRtoJ["fwi"] = "??????"
        mRtoJ["fwu"] = "??????"
        mRtoJ["fwe"] = "??????"
        mRtoJ["fwo"] = "??????"
        mRtoJ["fa"] = "??????"
        mRtoJ["fi"] = "??????"
        mRtoJ["fe"] = "??????"
        mRtoJ["fo"] = "??????"
        mRtoJ["fyi"] = "??????"
        mRtoJ["fye"] = "??????"
        mRtoJ["ba"] = "???"
        mRtoJ["bi"] = "???"
        mRtoJ["bu"] = "???"
        mRtoJ["be"] = "???"
        mRtoJ["bo"] = "???"
        mRtoJ["bya"] = "??????"
        mRtoJ["byi"] = "??????"
        mRtoJ["byu"] = "??????"
        mRtoJ["bye"] = "??????"
        mRtoJ["byo"] = "??????"
        mRtoJ["pa"] = "???"
        mRtoJ["pi"] = "???"
        mRtoJ["pu"] = "???"
        mRtoJ["pe"] = "???"
        mRtoJ["po"] = "???"
        mRtoJ["pya"] = "??????"
        mRtoJ["pyi"] = "??????"
        mRtoJ["pyu"] = "??????"
        mRtoJ["pye"] = "??????"
        mRtoJ["pyo"] = "??????"
        mRtoJ["ma"] = "???"
        mRtoJ["mi"] = "???"
        mRtoJ["mu"] = "???"
        mRtoJ["me"] = "???"
        mRtoJ["mo"] = "???"
        mRtoJ["mya"] = "??????"
        mRtoJ["myi"] = "??????"
        mRtoJ["myu"] = "??????"
        mRtoJ["mye"] = "??????"
        mRtoJ["myo"] = "??????"
        mRtoJ["ya"] = "???"
        mRtoJ["yu"] = "???"
        mRtoJ["yo"] = "???"
        mRtoJ["xya"] = "???"
        mRtoJ["xyu"] = "???"
        mRtoJ["xyo"] = "???"
        mRtoJ["ra"] = "???"
        mRtoJ["ri"] = "???"
        mRtoJ["ru"] = "???"
        mRtoJ["re"] = "???"
        mRtoJ["ro"] = "???"
        mRtoJ["rya"] = "??????"
        mRtoJ["ryi"] = "??????"
        mRtoJ["ryu"] = "??????"
        mRtoJ["rye"] = "??????"
        mRtoJ["ryo"] = "??????"
        mRtoJ["la"] = "???"
        mRtoJ["li"] = "???"
        mRtoJ["lu"] = "???"
        mRtoJ["le"] = "???"
        mRtoJ["lo"] = "???"
        mRtoJ["lya"] = "??????"
        mRtoJ["lyi"] = "??????"
        mRtoJ["lyu"] = "??????"
        mRtoJ["lye"] = "??????"
        mRtoJ["lyo"] = "??????"
        mRtoJ["wa"] = "???"
        mRtoJ["wo"] = "???"
        mRtoJ["lwe"] = "???"
        mRtoJ["xwa"] = "???"
        mRtoJ["nn"] = "???"
        mRtoJ["'n '"] = "???"
        mRtoJ["xn"] = "???"
        mRtoJ["ltsu"] = "???"
        mRtoJ["xtsu"] = "???"
    }

    private fun prepareJtoR() {
        mJtoR["???"] = "a"
        mJtoR["???"] = "i"
        mJtoR["???"] = "u"
        mJtoR["???"] = "e"
        mJtoR["???"] = "o"
        mJtoR["??????"] = "va"
        mJtoR["??????"] = "vi"
        mJtoR["???"] = "vu"
        mJtoR["??????"] = "ve"
        mJtoR["??????"] = "vo"
        mJtoR["???"] = "ka"
        mJtoR["???"] = "ki"
        mJtoR["??????"] = "kya"
        mJtoR["??????"] = "kyi"
        mJtoR["??????"] = "kyu"
        mJtoR["???"] = "ku"
        mJtoR["???"] = "ke"
        mJtoR["???"] = "ko"
        mJtoR["???"] = "ga"
        mJtoR["???"] = "gi"
        mJtoR["???"] = "gu"
        mJtoR["???"] = "ge"
        mJtoR["???"] = "go"
        mJtoR["??????"] = "gya"
        mJtoR["??????"] = "gyi"
        mJtoR["??????"] = "gyu"
        mJtoR["??????"] = "gye"
        mJtoR["??????"] = "gyo"
        mJtoR["???"] = "sa"
        mJtoR["???"] = "su"
        mJtoR["???"] = "se"
        mJtoR["???"] = "so"
        mJtoR["???"] = "za"
        mJtoR["???"] = "zu"
        mJtoR["???"] = "ze"
        mJtoR["???"] = "zo"
        mJtoR["???"] = "shi"
        mJtoR["??????"] = "sha"
        mJtoR["??????"] = "shu"
        mJtoR["??????"] = "sho"
        mJtoR["???"] = "ji"
        mJtoR["??????"] = "ja"
        mJtoR["??????"] = "ju"
        mJtoR["??????"] = "jo"
        mJtoR["???"] = "ta"
        mJtoR["???"] = "chi"
        mJtoR["??????"] = "cha"
        mJtoR["??????"] = "chu"
        mJtoR["??????"] = "cho"
        mJtoR["???"] = "tsu"
        mJtoR["???"] = "te"
        mJtoR["???"] = "to"
        mJtoR["???"] = "da"
        mJtoR["???"] = "di"
        mJtoR["???"] = "du"
        mJtoR["???"] = "de"
        mJtoR["???"] = "do"
        mJtoR["???"] = "na"
        mJtoR["???"] = "ni"
        mJtoR["??????"] = "nya"
        mJtoR["??????"] = "nyu"
        mJtoR["??????"] = "nyo"
        mJtoR["???"] = "nu"
        mJtoR["???"] = "ne"
        mJtoR["???"] = "no"
        mJtoR["???"] = "ha"
        mJtoR["???"] = "hi"
        mJtoR["???"] = "fu"
        mJtoR["???"] = "he"
        mJtoR["???"] = "ho"
        mJtoR["??????"] = "hya"
        mJtoR["??????"] = "hyu"
        mJtoR["??????"] = "hyo"
        mJtoR["??????"] = "fa"
        mJtoR["??????"] = "fi"
        mJtoR["??????"] = "fe"
        mJtoR["??????"] = "fo"
        mJtoR["???"] = "ba"
        mJtoR["???"] = "bi"
        mJtoR["???"] = "bu"
        mJtoR["???"] = "be"
        mJtoR["???"] = "bo"
        mJtoR["??????"] = "bya"
        mJtoR["??????"] = "byu"
        mJtoR["??????"] = "byo"
        mJtoR["???"] = "pa"
        mJtoR["???"] = "pi"
        mJtoR["???"] = "pu"
        mJtoR["???"] = "pe"
        mJtoR["???"] = "po"
        mJtoR["??????"] = "pya"
        mJtoR["??????"] = "pyu"
        mJtoR["??????"] = "pyo"
        mJtoR["???"] = "ma"
        mJtoR["???"] = "mi"
        mJtoR["???"] = "mu"
        mJtoR["???"] = "me"
        mJtoR["???"] = "mo"
        mJtoR["??????"] = "mya"
        mJtoR["??????"] = "myu"
        mJtoR["??????"] = "myo"
        mJtoR["???"] = "ya"
        mJtoR["???"] = "yu"
        mJtoR["???"] = "yo"
        mJtoR["???"] = "ra"
        mJtoR["???"] = "ri"
        mJtoR["???"] = "ru"
        mJtoR["???"] = "re"
        mJtoR["???"] = "ro"
        mJtoR["??????"] = "rya"
        mJtoR["??????"] = "ryu"
        mJtoR["??????"] = "ryo"
        mJtoR["???"] = "wa"
        mJtoR["???"] = "wo"
        mJtoR["???"] = "n"
        mJtoR["???"] = "wi"
        mJtoR["???"] = "we"
        mJtoR["??????"] = "kye"
        mJtoR["??????"] = "kyo"
        mJtoR["??????"] = "jyi"
        mJtoR["??????"] = "jye"
        mJtoR["??????"] = "cyi"
        mJtoR["??????"] = "che"
        mJtoR["??????"] = "hyi"
        mJtoR["??????"] = "hye"
        mJtoR["??????"] = "byi"
        mJtoR["??????"] = "bye"
        mJtoR["??????"] = "pyi"
        mJtoR["??????"] = "pye"
        mJtoR["??????"] = "mye"
        mJtoR["??????"] = "myi"
        mJtoR["??????"] = "ryi"
        mJtoR["??????"] = "rye"
        mJtoR["??????"] = "nyi"
        mJtoR["??????"] = "nye"
        mJtoR["??????"] = "syi"
        mJtoR["??????"] = "she"
        mJtoR["??????"] = "ye"
        mJtoR["??????"] = "wha"
        mJtoR["??????"] = "who"
        mJtoR["??????"] = "wi"
        mJtoR["??????"] = "we"
        mJtoR["??????"] = "vya"
        mJtoR["??????"] = "vyu"
        mJtoR["??????"] = "vyo"
        mJtoR["??????"] = "swa"
        mJtoR["??????"] = "swi"
        mJtoR["??????"] = "swu"
        mJtoR["??????"] = "swe"
        mJtoR["??????"] = "swo"
        mJtoR["??????"] = "qya"
        mJtoR["??????"] = "qyu"
        mJtoR["??????"] = "qyo"
        mJtoR["??????"] = "qwa"
        mJtoR["??????"] = "qwi"
        mJtoR["??????"] = "qwu"
        mJtoR["??????"] = "qwe"
        mJtoR["??????"] = "qwo"
        mJtoR["??????"] = "gwa"
        mJtoR["??????"] = "gwi"
        mJtoR["??????"] = "gwu"
        mJtoR["??????"] = "gwe"
        mJtoR["??????"] = "gwo"
        mJtoR["??????"] = "tsa"
        mJtoR["??????"] = "tsi"
        mJtoR["??????"] = "tse"
        mJtoR["??????"] = "tso"
        mJtoR["??????"] = "tha"
        mJtoR["??????"] = "thi"
        mJtoR["??????"] = "thu"
        mJtoR["??????"] = "the"
        mJtoR["??????"] = "tho"
        mJtoR["??????"] = "twa"
        mJtoR["??????"] = "twi"
        mJtoR["??????"] = "twu"
        mJtoR["??????"] = "twe"
        mJtoR["??????"] = "two"
        mJtoR["??????"] = "dya"
        mJtoR["??????"] = "dyi"
        mJtoR["??????"] = "dyu"
        mJtoR["??????"] = "dye"
        mJtoR["??????"] = "dyo"
        mJtoR["??????"] = "dha"
        mJtoR["??????"] = "dhi"
        mJtoR["??????"] = "dhu"
        mJtoR["??????"] = "dhe"
        mJtoR["??????"] = "dho"
        mJtoR["??????"] = "dwa"
        mJtoR["??????"] = "dwi"
        mJtoR["??????"] = "dwu"
        mJtoR["??????"] = "dwe"
        mJtoR["??????"] = "dwo"
        mJtoR["??????"] = "fwu"
        mJtoR["??????"] = "fya"
        mJtoR["??????"] = "fyu"
        mJtoR["??????"] = "fyo"
        mJtoR["???"] = "a"
        mJtoR["???"] = "i"
        mJtoR["???"] = "e"
        mJtoR["???"] = "u"
        mJtoR["???"] = "o"
        mJtoR["???"] = "ya"
        mJtoR["???"] = "yu"
        mJtoR["???"] = "yo"
        mJtoR["???"] = ""
        mJtoR["???"] = "ka"
        mJtoR["???"] = "ka"
        mJtoR["???"] = "wa"
        mJtoR["'???'"] = " "
        mJtoR["??????"] = "n'a"
        mJtoR["??????"] = "n'i"
        mJtoR["??????"] = "n'u"
        mJtoR["??????"] = "n'e"
        mJtoR["??????"] = "n'o"
        mJtoR["??????"] = "n'ya"
        mJtoR["??????"] = "n'yu"
        mJtoR["??????"] = "n'yo"
    }

    companion object {
        //static final int LOWERCASE_START = 0x61;
        //static final int LOWERCASE_END = 0x7A;
        const val UPPERCASE_START = 0x41
        const val UPPERCASE_END = 0x5A
        const val HIRAGANA_START = 0x3041
        const val HIRAGANA_END = 0x3096
        const val KATAKANA_START = 0x30A1
        const val KATAKANA_END = 0x30FA
        const val OPTION_USE_OBSOLETE_KANA = "useObsoleteKana"
        const val OPTION_IME_MODE = "IMEMode"
    }

    init {
        mOptions[OPTION_USE_OBSOLETE_KANA] = useObsoleteKana
        mOptions[OPTION_IME_MODE] = false
        prepareRtoJ()
        prepareJtoR()
    }
}