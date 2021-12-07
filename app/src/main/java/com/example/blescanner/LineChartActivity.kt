package com.example.blescanner

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.android.synthetic.main.activity_line_chart.*

class LineChartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_line_chart)

        val lineDataSet = LineDataSet(lineChartDataSet(), getString(R.string.hrt))
        val iLineDataSet = ArrayList<ILineDataSet>()
        iLineDataSet.add(lineDataSet)
        vLineCharts.also {
            it.data = LineData(iLineDataSet)
            it.invalidate()
        }

    }

    private fun lineChartDataSet() : ArrayList<Entry> {
        val dataSet = ArrayList<Entry>()
        for (i in 70 .. 109) {
            dataSet.add(Entry((i-70).toFloat(), i.toFloat() ))
        }
        return dataSet
    }
}