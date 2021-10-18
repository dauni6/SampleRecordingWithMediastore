package com.example.samplerecordingwithmediastore

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import java.util.*
import kotlin.concurrent.timer

class RecordButton(
    context: Context,
    attrs: AttributeSet
): AppCompatImageButton(context, attrs) {

    init {
        setBackgroundResource(R.drawable.shape_oval_button)
    }

    private var timer: Timer? = null

    fun updateIconWithState(state: RecordState) {
        when (state) {
            RecordState.BEFORE_RECORDING -> {
                setImageResource(R.drawable.ic_record)
            }
            RecordState.ON_RECORDING -> {
                var flow = 0
                timer = timer(period = 1000L) {
                    if (flow % 2 == 0) {
                        setImageResource(R.drawable.ic_stop)
                    } else {
                        setImageResource(R.drawable.ic_stop_on)
                    }
                    flow++
                }
            }
            RecordState.AFTER_RECORDING -> {
                timer?.cancel()
                setImageResource(R.drawable.ic_play)
            }
            RecordState.ON_PLAYING -> {
                setImageResource(R.drawable.ic_stop)
            }
        }
    }

}
