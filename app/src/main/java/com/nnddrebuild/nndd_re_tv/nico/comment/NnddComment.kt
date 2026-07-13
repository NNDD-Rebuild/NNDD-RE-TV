package com.nnddrebuild.nndd_re_tv.nico.comment

/** 再生時のコメント表現。移植元: NNDD-RE(PC版) src/shared/types/comment.ts NNDDREComment */
data class NnddComment(
    val no: Int,
    val vposMs: Long,
    val text: String,
    /** 0=BIG 1=MEDIUM 2=SMALL */
    val size: Int,
    /** "naka" | "ue" | "shita" */
    val pos: String,
    val color: Long,
    val strokeColor: Long?
)
