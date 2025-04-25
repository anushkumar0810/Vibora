package com.anush.vibora.CustomClasses

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

class CustomEditText : AppCompatEditText {

    constructor(context: Context) : super(context) {
        CustomFontHelper.setCustomFont(this, context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        CustomFontHelper.setCustomFont(this, context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        CustomFontHelper.setCustomFont(this, context, attrs)
    }
}
