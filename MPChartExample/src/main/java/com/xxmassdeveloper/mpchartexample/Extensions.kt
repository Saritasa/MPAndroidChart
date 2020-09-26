package com.xxmassdeveloper.mpchartexample

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat

fun Context.color(colorRes: Int) = ContextCompat.getColor(this, colorRes)

fun Context.getTintDrawable(drawableRes: Int, colorRes: Int): Drawable {
    val source = ContextCompat.getDrawable(this, drawableRes)!!.mutate()
    val wrapped = DrawableCompat.wrap(source)
    DrawableCompat.setTint(wrapped, color(colorRes))
    return wrapped
}