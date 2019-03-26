package com.github.zieiony.app.calc

import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.zieiony.calc.Calc
import com.github.zieiony.calc.CalcException
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        text.setOnEditorActionListener { textView, _, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                prev.text = textView.text
                try {
                    val result = Calc().evaluate(textView.text.toString())
                    textView.text = if (result.toInt().toDouble() == result) result.toInt().toString() else result.toString()
                } catch (e: CalcException) {
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                }
            }
            true

        }
    }
}
