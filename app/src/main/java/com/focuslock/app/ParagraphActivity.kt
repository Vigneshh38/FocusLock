package com.focuslock.app

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/** Type the paragraph exactly. Any mistake, paste, voice or swipe input restarts it. */
class ParagraphActivity : AppCompatActivity() {

    private lateinit var target: String
    private lateinit var pkg: String
    private lateinit var targetView: TextView
    private lateinit var progress: TextView
    private lateinit var error: TextView
    private lateinit var input: EditText
    private var ignore = false
    private var bulkInsert = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paragraph)
        pkg = intent.getStringExtra(MainActivity.EXTRA_PKG) ?: run { finish(); return }
        val rule = Store.rule(this, pkg) ?: run { finish(); return }
        title = "Unblock ${Apps.info(this, pkg).label}"

        target = Paragraphs.make(rule.amount)
        targetView = findViewById(R.id.target)
        progress = findViewById(R.id.progress)
        error = findViewById(R.id.error)
        input = findViewById(R.id.input)

        // no suggestions / autocorrect, no copy-paste menu
        input.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        input.setHorizontallyScrolling(false)
        input.maxLines = 6
        input.isLongClickable = false
        val noMenu = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?) = false
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false
            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?) = false
            override fun onDestroyActionMode(mode: ActionMode?) {}
        }
        input.customSelectionActionModeCallback = noMenu
        input.customInsertionActionModeCallback = noMenu

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!ignore && count - before > 2) bulkInsert = true
            }
            override fun afterTextChanged(s: Editable) {
                if (ignore) return
                if (bulkInsert) {
                    bulkInsert = false
                    restart(s, "Paste, voice or swipe typing isn't allowed. Type letter by letter — start again.")
                    return
                }
                val typed = s.toString()
                if (!target.startsWith(typed)) {
                    restart(s, "Mistake at character ${typed.length}. Start again from the beginning.")
                    return
                }
                error.text = ""
                render(typed.length)
                if (typed.length == target.length) success()
            }
        })
        render(0)
    }

    private fun restart(s: Editable, msg: String) {
        ignore = true
        s.clear()
        ignore = false
        error.text = msg
        render(0)
    }

    private fun render(done: Int) {
        val span = SpannableString(target)
        if (done > 0) span.setSpan(ForegroundColorSpan(Color.parseColor("#43A047")), 0, done, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (done < target.length) span.setSpan(BackgroundColorSpan(Color.parseColor("#66FFB300")), done, done + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        targetView.text = span
        progress.text = "$done / ${target.length} characters"
    }

    private fun success() {
        Store.removeRule(this, pkg)
        Toast.makeText(this, "Done! App unblocked.", Toast.LENGTH_LONG).show()
        finish()
    }
}
