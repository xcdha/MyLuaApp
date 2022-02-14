package com.dingyi.myluaapp.plugin.api.editor

import android.view.View
import com.dingyi.myluaapp.plugin.api.editor.language.Language
import java.io.File

interface Editor<T>  {

    fun getText():CharSequence

    fun getCurrentLine():Int

    fun getCurrentColumn():Int

    fun setText(charSequence: CharSequence)

    fun appendText(charSequence: CharSequence)

    fun getId():Int

    fun saveState(): T

    fun restoreState(editorState: T)

    fun getFile():File

    fun isModify():Boolean

    fun getLanguage():Language

    fun setLanguage(language: Language)


    fun getCurrentView():View

}