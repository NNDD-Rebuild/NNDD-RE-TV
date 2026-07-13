package com.nnddrebuild.nndd_re_tv.nico.comment

data class ParsedCommentCommand(
    val color: Long,
    val strokeColor: Long?,
    /** 0=BIG 1=MEDIUM 2=SMALL */
    val size: Int,
    /** "naka" | "ue" | "shita" */
    val position: String
)

private const val DEFAULT_COLOR = 0xffffffL

private val STANDARD_COLORS: Map<String, Long> = mapOf(
    "white" to 0xffffffL, "red" to 0xff0000L, "pink" to 0xff8080L,
    "orange" to 0xffc000L, "yellow" to 0xffff00L, "green" to 0x00ff00L,
    "cyan" to 0x00ffffL, "blue" to 0x0000ffL, "purple" to 0xc000ffL, "black" to 0x000000L
)

private val PREMIUM_COLORS: Map<String, Long> = mapOf(
    "white2" to 0xcccc99L, "niconicowhite" to 0xcccc99L,
    "red2" to 0xcc0033L, "truered" to 0xcc0033L,
    "pink2" to 0xff33ccL,
    "orange2" to 0xff6600L, "passionorange" to 0xff6600L,
    "yellow2" to 0x999900L, "madyellow" to 0x999900L,
    "green2" to 0x00cc66L, "elementalgreen" to 0x00cc66L,
    "cyan2" to 0x00ccccL,
    "blue2" to 0x3399ffL, "marineblue" to 0x3399ffL,
    "purple2" to 0x6633ccL, "nobleviolet" to 0x6633ccL,
    "black2" to 0x000000L
)

private val HEX_COLOR_RE = Regex("^#[0-9a-f]{6}$", RegexOption.IGNORE_CASE)

/**
 * コメントのcommands配列(またはmail文字列)から色・サイズ・位置を抽出する。
 * 移植元: NNDD-RE(PC版) src/main/nicovideo/comment/CommentCommandParser.ts
 */
object CommentCommandParser {

    fun parse(commands: List<String>?, isPremium: Boolean): ParsedCommentCommand {
        var fillColor = DEFAULT_COLOR
        var strokeColor: Long? = null
        var hasNamedColor = false
        var hasFillHex = false
        var size = 1 // MEDIUM
        var position = "naka"

        for (raw in commands ?: emptyList()) {
            val t = raw.lowercase()
            when {
                t == "naka" -> position = "naka"
                t == "ue" -> position = "ue"
                t == "shita" -> position = "shita"
                t == "big" -> size = 0
                t == "medium" -> size = 1
                t == "small" -> size = 2
                STANDARD_COLORS.containsKey(t) -> {
                    fillColor = STANDARD_COLORS.getValue(t)
                    hasNamedColor = true
                }
                isPremium && PREMIUM_COLORS.containsKey(t) -> {
                    fillColor = PREMIUM_COLORS.getValue(t)
                    hasNamedColor = true
                }
                HEX_COLOR_RE.matches(t) -> {
                    val hex = t.substring(1).toLong(16)
                    when {
                        hasNamedColor -> strokeColor = hex
                        !hasFillHex -> {
                            fillColor = hex
                            hasFillHex = true
                        }
                        else -> strokeColor = hex
                    }
                }
            }
        }
        return ParsedCommentCommand(fillColor, strokeColor, size, position)
    }
}
