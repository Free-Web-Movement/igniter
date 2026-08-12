package io.github.freewebmovement.igniter.ui.component.textview.listener

import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import io.github.freewebmovement.igniter.IgniterApplication

/**
 * Text view listener which splits the update text event in four parts:
 *  - The text placed **before** the updated part.
 *  - The **old** text in the updated part.
 *  - The **new** text in the updated part.
 *  - The text placed **after** the updated part.
 */
abstract class TextViewListener(
    protected val tv: TextView,
    protected val app: IgniterApplication
) : TextWatcher {
    init {
        tv.addTextChangedListener(this)
    }

    /**
     * Unchanged sequence which is placed before the updated sequence.
     */
    private var _before: String? = null

    /**
     * Updated sequence before the update.
     */
    private var _old: String? = null

    /**
     * Updated sequence after the update.
     */
    private var _new: String? = null

    /**
     * Unchanged sequence which is placed after the updated sequence.
     */
    private var _after: String? = null

    /**
     * Indicates when changes are made from within the listener, should be omitted.
     */
    private var _ignore = false

    override fun beforeTextChanged(sequence: CharSequence, start: Int, count: Int, after: Int) {
        _before = sequence.subSequence(0, start).toString()
        _old = sequence.subSequence(start, start + count).toString()
        _after = sequence.subSequence(start + count, sequence.length).toString()
    }

    override fun onTextChanged(sequence: CharSequence, start: Int, before: Int, count: Int) {
        _new = sequence.subSequence(start, start + count).toString()
    }

    override fun afterTextChanged(sequence: Editable) {
        if (_ignore) {
            return
        }
        startUpdates() // to prevent infinite loop.
        onTextChanged(_before!!, _old!!, _new!!, _after!!)
        endUpdates()
    }

    /**
     * Triggered method when the text in the text view has changed.
     */
    protected abstract fun onTextChanged(before: String, old: String, aNew: String, after: String)

    /**
     * Call this method when you start to update the text view, so it stops listening to it and then prevent an infinite loop.
     */
    protected fun startUpdates() {
        _ignore = true
    }

    /**
     * Call this method when you finished to update the text view in order to restart to listen to it.
     */
    protected fun endUpdates() {
        _ignore = false
    }
}
