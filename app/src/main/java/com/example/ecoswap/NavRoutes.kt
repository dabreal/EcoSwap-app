package com.example.ecoswap

object NavRoutes {
    const val HOME = "home"
    const val HOME_ARGS = "home?tab={tab}&focusId={focusId}"
    const val DETAIL = "detail/{productId}"

    fun detail(productId: Long) = "detail/$productId"

    fun home(tab: Int = 0, focusId: Long? = null): String =
        "home?tab=$tab&focusId=${focusId ?: -1L}"
}