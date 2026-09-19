package com.avinash.homesense.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.avinash.homesense.data.model.Bucket
import com.avinash.homesense.data.model.BucketStat
import com.avinash.homesense.data.model.Reading
import com.avinash.homesense.ui.util.bucketReadings
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/** Validated categorical slots 1 (blue) and 2 (orange) — see dataviz palette reference. */
@Composable
fun temperatureSeriesColor(): Color = if (isSystemInDarkTheme()) Color(0xFF3987E5) else Color(0xFF2A78D6)

@Composable
fun humiditySeriesColor(): Color = if (isSystemInDarkTheme()) Color(0xFFD95926) else Color(0xFFEB6834)

/**
 * Temperature and humidity on one shared timeline. The two measures have
 * unrelated scales (°C vs %), so each is normalized to its own range within
 * the same canvas height rather than a single shared y-axis — the color-coded
 * legend and the tap popover's exact values are what keep the two readable
 * together. Raw readings are bucketed into 30-minute windows first so a noisy
 * sensor reads as a deliberate curve; each bucket's min/max spread renders as
 * a soft band around its mean. Tapping a point selects its bucket and shows
 * both values in a floating popover anchored near the tap.
 */
@Composable
fun DayTimelineCharts(
    readings: List<Reading>,
    date: LocalDate,
    isToday: Boolean,
    modifier: Modifier = Modifier,
) {
    val buckets = remember(readings, date) { bucketReadings(readings, date) }
    // Keyed to the day, not the bucket list — buckets get a new identity every
    // ~20s live poll, and re-keying on that would silently clear the user's
    // selection moments after they tap. Bucket index N always means the same
    // time-of-day slot across regenerations, so the selection stays valid.
    var selectedIndex by remember(date) { mutableStateOf<Int?>(null) }
    val tempColor = temperatureSeriesColor()
    val humidityColor = humiditySeriesColor()

    Column(modifier = modifier) {
        LegendRow(buckets, tempColor, humidityColor)
        Spacer(modifier = Modifier.height(8.dp))
        CombinedChart(
            buckets = buckets,
            tempColor = tempColor,
            humidityColor = humidityColor,
            isToday = isToday,
            selectedIndex = selectedIndex,
            onSelect = { selectedIndex = it },
            onDismiss = { selectedIndex = null },
        )
        HourAxisLabels()
    }
}

/**
 * Placeholder matching [DayTimelineCharts]'s layout exactly (same legend row,
 * chart height, and axis labels) so swapping it in while a day is loading
 * doesn't change the card's height once real data arrives.
 */
@Composable
fun DayTimelineChartsSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        LegendRow(buckets = emptyList(), tempColor = temperatureSeriesColor(), humidityColor = humiditySeriesColor())
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        HourAxisLabels()
    }
}

@Composable
private fun LegendRow(buckets: List<Bucket>, tempColor: Color, humidityColor: Color) {
    val lastTemp = buckets.lastOrNull { it.temperature != null }?.temperature
    val lastHumidity = buckets.lastOrNull { it.humidity != null }?.humidity
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        LegendItem(tempColor, "Temperature", lastTemp?.let { "%.1f°C".format(it.mean) })
        LegendItem(humidityColor, "Humidity", lastHumidity?.let { "%.0f%%".format(it.mean) })
    }
}

@Composable
private fun LegendItem(color: Color, label: String, value: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (value != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

private data class ChartPoint(val bucketIndex: Int, val fraction: Double, val stat: BucketStat)

/** Reserved on the left of the chart for temperature axis labels; [HourAxisLabels] below is padded to match. */
private val AxisLabelWidth = 34.dp

@Composable
private fun CombinedChart(
    buckets: List<Bucket>,
    tempColor: Color,
    humidityColor: Color,
    isToday: Boolean,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val height = 220.dp

    val tempPoints = buckets.mapIndexedNotNull { i, b -> b.temperature?.let { ChartPoint(i, fractionOfDay(bucketMidpoint(b), zone), it) } }
    val humidityPoints = buckets.mapIndexedNotNull { i, b -> b.humidity?.let { ChartPoint(i, fractionOfDay(bucketMidpoint(b), zone), it) } }
    val allPoints = tempPoints.ifEmpty { humidityPoints }
    val nowFraction = if (isToday) fractionOfDay(Instant.now(), zone) else null

    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val nowLineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val axisTextStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val textMeasurer = rememberTextMeasurer()

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(buckets) {
                if (allPoints.isEmpty()) return@pointerInput
                val leftInset = AxisLabelWidth.toPx()
                val plotWidth = size.width - leftInset
                detectTapGestures { offset ->
                    val nearest = allPoints.minByOrNull { point -> abs((leftInset + point.fraction * plotWidth) - offset.x) }
                    nearest?.let { onSelect(it.bucketIndex) }
                }
            },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .onSizeChanged { canvasSize = it },
        ) {
            val topInset = 10.dp.toPx()
            val bottomInset = 10.dp.toPx()
            val plotHeight = size.height - topInset - bottomInset
            val leftInset = AxisLabelWidth.toPx()
            val plotWidth = size.width - leftInset

            // 10 steps (11 lines) so the middle line always lands exactly on
            // (min+max)/2 — 5 markers above the day's midpoint temperature,
            // 5 below — rather than a coarser split that skips round values.
            val gridSteps = 10
            val tempRange = valueRange(tempPoints)
            val tempStep = tempRange?.let { (min, max) -> (max - min) / gridSteps }
            val labelPattern = if (tempStep != null && abs(tempStep) < 1.0) "%.1f°" else "%.0f°"
            for (step in 0..gridSteps) {
                val y = topInset + plotHeight * step / gridSteps
                drawLine(color = gridColor, start = Offset(leftInset, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())

                if (tempRange != null) {
                    val (minValue, maxValue) = tempRange
                    val fractionFromTop = step.toFloat() / gridSteps
                    val value = maxValue - fractionFromTop * (maxValue - minValue)
                    val measured = textMeasurer.measure(labelPattern.format(value), style = axisTextStyle)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(leftInset - 6.dp.toPx() - measured.size.width, y - measured.size.height / 2f),
                    )
                }
            }

            drawSeries(tempPoints, buckets, topInset, plotHeight, tempColor, surfaceColor, leftInset, plotWidth)
            drawSeries(humidityPoints, buckets, topInset, plotHeight, humidityColor, surfaceColor, leftInset, plotWidth)

            if (nowFraction != null) {
                val nowX = leftInset + nowFraction.toFloat() * plotWidth
                drawLine(
                    color = nowLineColor.copy(alpha = 0.35f),
                    start = Offset(nowX, topInset),
                    end = Offset(nowX, size.height - bottomInset),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                )
            }

            if (selectedIndex != null) {
                val selectedPoint = allPoints.firstOrNull { it.bucketIndex == selectedIndex }
                if (selectedPoint != null) {
                    val selX = leftInset + selectedPoint.fraction.toFloat() * plotWidth
                    drawLine(
                        color = nowLineColor.copy(alpha = 0.4f),
                        start = Offset(selX, topInset),
                        end = Offset(selX, size.height - bottomInset),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                drawSelectionDot(tempPoints, selectedIndex, topInset, plotHeight, leftInset, plotWidth, tempColor, surfaceColor)
                drawSelectionDot(humidityPoints, selectedIndex, topInset, plotHeight, leftInset, plotWidth, humidityColor, surfaceColor)
            }
        }

        val selectedBucket = selectedIndex?.let { buckets.getOrNull(it) }
        if (selectedBucket != null && (selectedBucket.temperature != null || selectedBucket.humidity != null) && canvasSize.width > 0) {
            val fraction = fractionOfDay(bucketMidpoint(selectedBucket), zone)
            val popoverAlignment = when {
                fraction < 0.3 -> Alignment.TopStart
                fraction > 0.7 -> Alignment.TopEnd
                else -> Alignment.TopCenter
            }
            // A real Popup (not just an overlaid Box) so tapping, swiping, or
            // touching anywhere else on screen — not just this chart — dismisses it,
            // and back-press dismisses it too, both for free via PopupProperties.
            Popup(
                alignment = popoverAlignment,
                offset = IntOffset(0, with(LocalDensity.current) { 4.dp.roundToPx() }),
                onDismissRequest = onDismiss,
                properties = PopupProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
            ) {
                PopoverCard(
                    bucket = selectedBucket,
                    tempColor = tempColor,
                    humidityColor = humidityColor,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

/** Inclusive min/max across a series' min/max spread, or null if there's no data to range over. */
private fun valueRange(points: List<ChartPoint>): Pair<Double, Double>? {
    if (points.isEmpty()) return null
    val allValues = points.flatMap { listOf(it.stat.min, it.stat.max) }
    return allValues.min() to allValues.max()
}

private fun DrawScope.drawSeries(
    points: List<ChartPoint>,
    buckets: List<Bucket>,
    topInset: Float,
    plotHeight: Float,
    seriesColor: Color,
    surfaceColor: Color,
    leftInset: Float,
    plotWidth: Float,
) {
    if (points.isEmpty()) return
    val allValues = points.flatMap { listOf(it.stat.min, it.stat.max) }
    val minValue = allValues.min()
    val maxValue = allValues.max()
    val range = (maxValue - minValue).takeIf { it > 0.01 } ?: 1.0

    fun valueToY(value: Double): Float {
        val normalized = ((value - minValue) / range).toFloat()
        return topInset + plotHeight - (normalized * plotHeight)
    }
    fun xOf(point: ChartPoint): Float = leftInset + point.fraction.toFloat() * plotWidth

    for (segment in points.splitOnGaps()) {
        if (segment.size >= 2) {
            val meanPoints = segment.map { Offset(xOf(it), valueToY(it.stat.mean)) }
            val linePath = buildSmoothPath(meanPoints)

            val bandPath = buildBandPath(
                segment.map { Offset(xOf(it), valueToY(it.stat.max)) },
                segment.map { Offset(xOf(it), valueToY(it.stat.min)) },
            )
            drawPath(path = bandPath, color = seriesColor.copy(alpha = 0.08f), style = Fill)
            drawPath(path = linePath, color = seriesColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

            val last = segment.last()
            val end = Offset(xOf(last), valueToY(last.stat.mean))
            drawCircle(color = surfaceColor, radius = 6.dp.toPx(), center = end)
            if (last.bucketIndex == buckets.lastIndex && buckets.getOrNull(last.bucketIndex)?.isPartial == true) {
                drawCircle(color = seriesColor, radius = 4.dp.toPx(), center = end, style = Stroke(width = 1.5.dp.toPx()))
            } else {
                drawCircle(color = seriesColor, radius = 4.dp.toPx(), center = end)
            }
        } else {
            val p = segment[0]
            val point = Offset(xOf(p), valueToY(p.stat.mean))
            drawCircle(color = surfaceColor, radius = 6.dp.toPx(), center = point)
            drawCircle(color = seriesColor, radius = 4.dp.toPx(), center = point)
        }
    }
}

private fun DrawScope.drawSelectionDot(
    points: List<ChartPoint>,
    selectedIndex: Int?,
    topInset: Float,
    plotHeight: Float,
    leftInset: Float,
    plotWidth: Float,
    seriesColor: Color,
    surfaceColor: Color,
) {
    val selected = points.firstOrNull { it.bucketIndex == selectedIndex } ?: return
    val allValues = points.flatMap { listOf(it.stat.min, it.stat.max) }
    val minValue = allValues.min()
    val maxValue = allValues.max()
    val range = (maxValue - minValue).takeIf { it > 0.01 } ?: 1.0
    val normalized = ((selected.stat.mean - minValue) / range).toFloat()
    val y = topInset + plotHeight - (normalized * plotHeight)
    val x = leftInset + selected.fraction.toFloat() * plotWidth
    drawCircle(color = surfaceColor, radius = 7.dp.toPx(), center = Offset(x, y))
    drawCircle(color = seriesColor, radius = 5.dp.toPx(), center = Offset(x, y))
}

private fun List<ChartPoint>.splitOnGaps(): List<List<ChartPoint>> {
    if (isEmpty()) return emptyList()
    val segments = mutableListOf<MutableList<ChartPoint>>(mutableListOf(first()))
    for (i in 1 until size) {
        val prev = this[i - 1]
        val curr = this[i]
        if (curr.bucketIndex == prev.bucketIndex + 1) {
            segments.last().add(curr)
        } else {
            segments.add(mutableListOf(curr))
        }
    }
    return segments
}

@Composable
private fun HourAxisLabels() {
    val hours = listOf("12AM", "6AM", "12PM", "6PM")
    Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp, start = AxisLabelWidth)) {
        hours.forEachIndexed { index, hour ->
            Text(
                text = hour,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f),
                textAlign = when (index) {
                    0 -> TextAlign.Start
                    hours.lastIndex -> TextAlign.End
                    else -> TextAlign.Center
                },
            )
        }
    }
}

@Composable
private fun PopoverCard(
    bucket: Bucket,
    tempColor: Color,
    humidityColor: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val midpoint = bucketMidpoint(bucket)
    val nearestReading = bucket.rawReadings.minByOrNull { abs(Duration.between(midpoint, it.timestamp).seconds) }
    val displayTime = nearestReading?.timestamp ?: bucket.start
    val temp = nearestReading?.temperatureCelsius ?: bucket.temperature?.mean
    val humidity = nearestReading?.humidityPercent ?: bucket.humidity?.mean
    val count = bucket.temperature?.count ?: 0

    Card(
        modifier = modifier.widthIn(max = 220.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = DateTimeFormatter.ofPattern("h:mm a").format(displayTime.atZone(zone)),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (temp != null) DetailValue(color = tempColor, text = "%.1f°C".format(temp))
            if (humidity != null) DetailValue(color = humidityColor, text = "%.0f%% humidity".format(humidity))
            if (count > 1) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Avg of $count readings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailValue(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun bucketMidpoint(bucket: Bucket): Instant =
    bucket.start.plus(Duration.between(bucket.start, bucket.end).dividedBy(2))

private fun fractionOfDay(instant: Instant, zone: ZoneId): Double {
    val local = instant.atZone(zone).toLocalTime()
    return local.toSecondOfDay() / 86_400.0
}

private fun buildSmoothPath(points: List<Offset>): Path {
    val path = Path()
    path.moveTo(points[0].x, points[0].y)
    for (i in 0 until points.size - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val controlX1 = p0.x + (p1.x - p0.x) / 3f
        val controlX2 = p0.x + 2f * (p1.x - p0.x) / 3f
        path.cubicTo(controlX1, p0.y, controlX2, p1.y, p1.x, p1.y)
    }
    return path
}

/** A closed band between an upper (max) and lower (min) curve, both smoothed the same way as the mean line. */
private fun buildBandPath(upperPoints: List<Offset>, lowerPoints: List<Offset>): Path {
    val upper = buildSmoothPath(upperPoints)
    val path = Path()
    path.addPath(upper)
    val reversedLower = lowerPoints.reversed()
    path.lineTo(reversedLower.first().x, reversedLower.first().y)
    for (i in 0 until reversedLower.size - 1) {
        val p0 = reversedLower[i]
        val p1 = reversedLower[i + 1]
        val controlX1 = p0.x + (p1.x - p0.x) / 3f
        val controlX2 = p0.x + 2f * (p1.x - p0.x) / 3f
        path.cubicTo(controlX1, p0.y, controlX2, p1.y, p1.x, p1.y)
    }
    path.close()
    return path
}
