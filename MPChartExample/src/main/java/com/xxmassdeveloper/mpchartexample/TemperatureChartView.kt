package com.xxmassdeveloper.mpchartexample

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.Utils
import kotlinx.android.synthetic.main.view_temperature_chart.view.*
import org.joda.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Custom view that allows to show temperature values on the Chart view.
 */
class TemperatureChartView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ConstraintLayout(context, attrs, defStyleAttr) {

    var initDateTime: LocalDateTime = LocalDateTime.now()
    var offsetDateTime: LocalDateTime = LocalDateTime.now()

    private var dateTimeFormat: String = ""
    private var temperatureFormat: String = ""

    var startOffset: Float = 0f

    private var isShowCircles = false
    private var stepInMinutes: Int = 0
    private var minX: Float = 0f
    private var granularityX: Float = 0f
    private var minutesInPoint: Int = 0
    private var maxDifference: Long = 0L

    init {
        inflate(context, R.layout.view_temperature_chart, this)
    }

    /**
     * Inits the Temperature Chart View by params
     */
    fun init(chartType: ChartType, yMax: Float, yMin: Float, yHigh: Float, yModerate: Float, temperatureData: List<TemperatureData>, temperatureType: TemperatureType) {
        initDateTime = LocalDateTime.now()

        when (chartType) {
            ChartType.MINUTE -> {
                isShowCircles = true
                stepInMinutes = STEP_MINUTE
                minX = MIN_MINUTE_VALUE
                granularityX = GRANULARITY_X_MINUTE
                minutesInPoint = MINUTES_IN_POINT_MINUTE
                maxDifference = MAX_DIFFERENCE_MINUTE
                dateTimeFormat = context.getString(R.string.dashboard_chart_date_time_format)
            }
            ChartType.HOUR -> {
                isShowCircles = false
                stepInMinutes = STEP_HOUR
                minX = MIN_HOUR_VALUE
                granularityX = GRANULARITY_X_HOUR
                minutesInPoint = MINUTES_IN_POINT_HOUR
                maxDifference = MAX_DIFFERENCE_HOUR
                dateTimeFormat = context.getString(R.string.dashboard_chart_date_time_format)
            }
            ChartType.DAY -> {
                isShowCircles = true
                stepInMinutes = STEP_DAY
                minX = MIN_DAY_VALUE
                granularityX = GRANULARITY_X_DAY
                minutesInPoint = MINUTES_IN_POINT_DAY
                maxDifference = MAX_DIFFERENCE_DAY
                dateTimeFormat = context.getString(R.string.dashboard_chart_date_time_format_all)
            }
        }

        temperatureFormat = when (temperatureType) {
            TemperatureType.Fahrenheit -> context.getString(R.string.fever_threshold_temperature_fahrenheit)
            else -> context.getString(R.string.fever_threshold_temperature_celsius)
        }

        offsetDateTime = LocalDateTime(initDateTime.roundToStep(stepInMinutes))
        startOffset = convertMillis(initDateTime.roundToStep(minutesInPoint)) - convertMillis(offsetDateTime.toDateTime().millis)

        val data = ArrayList<ArrayList<ChartEntry>>()
        var dataIndex = 0
        data.add(ArrayList())
        lateinit var oldEntry: ChartEntry

        temperatureData.groupBy { it.dateTime.roundToStep(minutesInPoint) }
            .toList()
            .map { pair -> ChartEntry(millis = pair.first, temperature = pair.second.map { it.temperature }.average().toFloat()) }
            .forEachIndexed { i, chartEntry ->
                if (i == 0) {
                    data[dataIndex].add(chartEntry)
                    oldEntry = chartEntry
                } else {
                    if ((chartEntry.millis - oldEntry.millis) > maxDifference) {
                        dataIndex++
                        data.add(dataIndex, ArrayList())
                    }

                    data[dataIndex].add(chartEntry)
                    oldEntry = chartEntry
                }
            }

        lineChart.resetTracking()
        lineChart.clear()
        lineChart.legend.isEnabled = false
        lineChart.description.isEnabled = false
        lineChart.isScaleXEnabled = false
        lineChart.isScaleYEnabled = false
        lineChart.setScaleMinima(SCALE_MINIMA_X, SCALE_MINIMA_Y)
        lineChart.setMaxVisibleValueCount(temperatureData.size * VALUE_COUNT_MULTIPLIER)

        Utils.init(context)
        setupAxises(yMax = yMax, yMin = yMin)
        setChartData(yHigh, yModerate, data, isShowCircles)

        lineChart.moveViewToX(lineChart.xAxis.axisMaximum * COEF_MULTIPLIER)
        lineChart.invalidate()
    }

    /**
     * Setups X and Y axises by [yMax] and [yMin] values.
     */
    private fun setupAxises(yMax: Float, yMin: Float) {
        // Setup Y axis
        lineChart.axisLeft.isEnabled = false
        lineChart.axisLeft.apply {
            axisMaximum = yMax
            axisMinimum = yMin
        }
        lineChart.axisRight.isEnabled = true
        lineChart.axisRight.apply {
            valueFormatter = TemperatureFormatter(temperatureFormat)
            axisMaximum = yMax
            axisMinimum = yMin
            setLabelCount(LABEL_COUNT_Y, true)
        }

        // Setup X axis
        lineChart.xAxis.apply {
            valueFormatter = DateTimeValueFormatter(dateTimeFormat)
            position = XAxis.XAxisPosition.BOTTOM

            // startOffset - current time (right line)
            // 0 - first right line (exclude startOffset)
            axisMaximum = startOffset
            axisMinimum = minX
            granularity = granularityX

            addLimitLine(LimitLine(-0.001f).apply {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    icon = context.getDrawable(R.drawable.test_icon)
                }
                iconSize = 20
            })
        }
    }

    /**
     * Sets new values (and colors) for the Chart View by [tHigh], [tModerate], [data] and [isShowCircles] flag.
     */
    private fun setChartData(tHigh: Float, tModerate: Float, data: List<List<ChartEntry>>, isShowCircles: Boolean) {
        val values = ArrayList<ArrayList<Entry>>()
        val colors = ArrayList<ArrayList<Int>>()

        data.forEachIndexed { dataSetIndex, dataSetList ->
            lateinit var temperatureColorOld: TemperatureColor
            var temperatureOld = 0f
            var xOld = 0f
            values.add(dataSetIndex, ArrayList())
            colors.add(dataSetIndex, ArrayList())

            for (index in dataSetList.indices) {
                val dataCurrent = dataSetList[index]
                val temperatureCurrent = dataCurrent.temperature
                val temperatureColorCurrent = when {
                    temperatureCurrent < tModerate -> TemperatureColor.LOW
                    temperatureCurrent > tHigh -> TemperatureColor.HIGH
                    else -> TemperatureColor.MIDDLE
                }
                val xCurrent = convertMillis(dataCurrent.millis)
                val colorResIdCurrent = temperatureCurrent.countColorResId(tHigh, tModerate)
                val iconCurrent = if (isShowCircles) context.getTintDrawable(R.drawable.ic_chart_entry, colorResIdCurrent) else null

                // first element
                if (index == 0) {
                    values[dataSetIndex].add(Entry(xCurrent, temperatureCurrent, iconCurrent))
                    temperatureOld = temperatureCurrent
                    temperatureColorOld = temperatureColorCurrent
                    xOld = xCurrent
                    continue
                }

                if (temperatureColorOld == temperatureColorCurrent) {
                    // same area
                    values[dataSetIndex].add(Entry(xCurrent, temperatureCurrent, iconCurrent))
                    colors[dataSetIndex].add(context.color(colorResIdCurrent))
                } else {
                    val xModerate = countChartX(xOld, xCurrent, temperatureOld, temperatureCurrent, tModerate)
                    val xHigh = countChartX(xOld, xCurrent, temperatureOld, temperatureCurrent, tHigh)

                    // add lines between areas
                    when (temperatureColorOld) {
                        TemperatureColor.LOW -> {
                            values[dataSetIndex].add(Entry(xModerate, tModerate, null))
                            colors[dataSetIndex].add(context.color(R.color.temperatureLow))
                            if (temperatureColorCurrent == TemperatureColor.HIGH) {
                                values[dataSetIndex].add(Entry(xHigh, tHigh, null))
                                colors[dataSetIndex].add(context.color(R.color.temperatureMiddle))
                            }
                        }
                        TemperatureColor.MIDDLE -> {
                            if (temperatureColorCurrent == TemperatureColor.LOW)
                                values[dataSetIndex].add(Entry(xModerate, tModerate, null))
                            else
                                values[dataSetIndex].add(Entry(xHigh, tHigh, null))
                            colors[dataSetIndex].add(context.color(R.color.temperatureMiddle))
                        }
                        TemperatureColor.HIGH -> {
                            values[dataSetIndex].add(Entry(xHigh, tHigh, null))
                            colors[dataSetIndex].add(context.color(R.color.temperatureHigh))
                            if (temperatureColorCurrent == TemperatureColor.LOW) {
                                values[dataSetIndex].add(Entry(xModerate, tModerate, null))
                                colors[dataSetIndex].add(context.color(R.color.temperatureMiddle))
                            }
                        }
                    }

                    // next element
                    values[dataSetIndex].add(Entry(xCurrent, temperatureCurrent, iconCurrent))
                    colors[dataSetIndex].add(context.color(colorResIdCurrent))

                    temperatureColorOld = temperatureColorCurrent
                }
                temperatureOld = temperatureCurrent
                xOld = xCurrent
            }

            // Handle single entry
            if (values[dataSetIndex].size == NEXT_INDEX && colors[dataSetIndex].isEmpty()) {
                val value = values[dataSetIndex].first()
                values[dataSetIndex].add(value)
                colors[dataSetIndex].add(0) // It is not important what color between two same entries
            }
        }
        initDataSet(values, colors)
    }

    /**
     * Inits the data set by [values] and [colors] lists.
     */
    private fun initDataSet(values: List<List<Entry>>, colors: List<List<Int>>) {
        if (lineChart.data?.dataSetCount ?: 0 > 0) {
            lineChart.data.dataSets.forEachIndexed { index, it ->
                val dataSet = it as LineDataSet
                dataSet.values = values[index]
                dataSet.notifyDataSetChanged()
            }
            lineChart.data.notifyDataChanged()
            lineChart.notifyDataSetChanged()
        } else {
            val lineDataSets = LineData()
            values.forEachIndexed { index, it ->
                lineDataSets.addDataSet(
                    LineDataSet(it, "DataSet $index").apply {
                        lineWidth = LINE_WIDTH
                        setDrawIcons(true)
                        setDrawValues(false)
                        setDrawCircles(false)
                        setDrawCircleHole(false)
                        this.colors = colors[index]
                    }
                )
            }
            lineChart.data = lineDataSets
        }
    }

    /**
     * Count color resource id by [tHigh] and [tModerate].
     */
    private fun Float.countColorResId(tHigh: Float, tModerate: Float) = when {
        this < tModerate -> R.color.temperatureLow
        this > tHigh -> R.color.temperatureHigh
        else -> R.color.temperatureMiddle
    }

    /**
     * Counts X coordinate for [y] between two points ([x1], [y1] and [x2], [y2]).
     */
    private fun countChartX(x1: Float, x2: Float, y1: Float, y2: Float, y: Float): Float = when {
        y1 < y2 -> x1 + (y - y1) / (y2 - y1) * (x2 - x1)
        y1 > y2 -> x1 + (y1 - y) / (y1 - y2) * (x2 - x1)
        else -> x1 + (x2 - x1) / 2
    }

    /**
     * Formatter to format temperature values by [res] string id.
     */
    inner class TemperatureFormatter(val res: String) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String = res.format(value)
    }

    /**
     * Formatter to format date time values by [res] string id.
     */
    inner class DateTimeValueFormatter(val res: String) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val localDateTime = offsetDateTime.minusMinutes((value * minutesInPoint * COEF * -1).roundToInt())
            return localDateTime.toString(res, Locale.getDefault())
        }
    }

    /**
     * Converts [millis] to float value. Float value can be displayed on the Chart.
     */
    private fun convertMillis(millis: Long): Float {
        val minutes = (initDateTime.toDateTime().millis - millis) / (minutesInPoint * ONE_MINUTE) * -1f
        return floor(minutes) / COEF + startOffset
    }
}

private enum class TemperatureColor {
    HIGH,
    MIDDLE,
    LOW
}

const val ONE_SECOND: Long = 1000
const val ONE_MINUTE = ONE_SECOND * 60
const val ONE_HOUR = ONE_MINUTE * 60

private const val COEF = 10_000f
private const val COEF_MULTIPLIER = 10

private const val LINE_WIDTH = 1f
private const val LABEL_COUNT_Y = 4
private const val VALUE_COUNT_MULTIPLIER = 2

private const val ALL_POINTS = 10080
private const val VISIBLE_POINTS = 30

private const val SCALE_MINIMA_X = ALL_POINTS / VISIBLE_POINTS.toFloat()
private const val SCALE_MINIMA_Y = 1f

private const val STEP_MINUTE = 15
private const val STEP_HOUR = 120
private const val STEP_DAY = 720

private const val MIN_MINUTE_VALUE = ALL_POINTS / COEF * -1
private const val MIN_HOUR_VALUE = ALL_POINTS / COEF * -1
private const val MIN_DAY_VALUE = ALL_POINTS / COEF * -1

private const val GRANULARITY_X_MINUTE = 15 / COEF
private const val GRANULARITY_X_HOUR = 10 / COEF
private const val GRANULARITY_X_DAY = 15 / COEF

private const val MINUTES_IN_POINT_MINUTE = 1
private const val MINUTES_IN_POINT_HOUR = 12
private const val MINUTES_IN_POINT_DAY = 48

private const val MAX_DIFFERENCE_MINUTE = 70 * ONE_SECOND
private const val MAX_DIFFERENCE_HOUR = (MINUTES_IN_POINT_HOUR + 1) * ONE_MINUTE
private const val MAX_DIFFERENCE_DAY = (MINUTES_IN_POINT_DAY + 12) * ONE_MINUTE

enum class ChartType {
    MINUTE,
    HOUR,
    DAY
}

data class ChartEntry(
    val temperature: Float,
    val millis: Long
)

fun LocalDateTime.roundToStep(stepInMinutes: Int) = this.toDateTime().millis - this.toDateTime().millis % (stepInMinutes * ONE_MINUTE)

/**
 * Temperature data model of data layer.
 */
data class TemperatureData(
    val sensorId: String,
    val userId: String = "",
    val dateTime: LocalDateTime = LocalDateTime.now(),
    var temperature: Float
)

private const val NEXT_INDEX = 1