package com.myxh.arch.ext

import android.view.View
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

/**
 * @description View点击防抖
 * @author myxh
 * @date 2019-06-27
 */
fun View.throttleFirstClick(): Observable<Unit> = clicks().throttleFirst(500, TimeUnit.MILLISECONDS)