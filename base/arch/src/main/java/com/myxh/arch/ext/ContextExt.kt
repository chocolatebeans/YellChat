package com.myxh.arch.ext

import android.content.Context
import com.blankj.utilcode.util.ToastUtils

/**
 * @description com.myxh.arch.ext
 * @author myxh
 * @date 2019-06-27
 */
fun Context.toast(message: String) = ToastUtils.showShort(message)

fun Context.toast(resId: Int) = ToastUtils.showShort(resId)

fun Context.toastLong(message: String) = ToastUtils.showLong(message)