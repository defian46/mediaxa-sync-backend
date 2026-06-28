package com.mediaxa.business.suite.presentation.component

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Composable
fun SalesAreaChart(
    dataPoints: List<Pair<String, Double>>, // Label to value
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF1E88E5),
    fillColor: Color = Color(0xFF1E88E5).copy(alpha = 0.2f),
    gridColor: Color = Color.Gray.copy(alpha = 0.2f)
) {
    if (dataPoints.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Tidak ada data", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val maxVal = dataPoints.maxOf { it.second }.coerceAtLeast(1.0)
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingLeft = 50.dp.toPx()
        val paddingRight = 20.dp.toPx()
        val paddingTop = 20.dp.toPx()
        val paddingBottom = 40.dp.toPx()
        
        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom
        
        // Draw Grid Lines (Y-Axis splits: 4 lines)
        val gridCount = 4
        for (i in 0..gridCount) {
            val y = paddingTop + chartHeight * (i.toFloat() / gridCount)
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(paddingLeft + chartWidth, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw Line & Area Path
        val path = Path()
        val fillPath = Path()
        
        val stepX = if (dataPoints.size > 1) chartWidth / (dataPoints.size - 1) else chartWidth
        
        dataPoints.forEachIndexed { index, pair ->
            val x = paddingLeft + index * stepX
            val y = paddingTop + chartHeight * (1f - (pair.second.toFloat() / maxVal.toFloat()))
            
            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, chartHeight + paddingTop)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            
            if (index == dataPoints.size - 1) {
                fillPath.lineTo(x, chartHeight + paddingTop)
                fillPath.close()
            }
        }
        
        // Draw Area Fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.4f), lineColor.copy(alpha = 0.0f)),
                startY = paddingTop,
                endY = paddingTop + chartHeight
            )
        )
        
        // Draw Trend Line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), join = StrokeJoin.Round)
        )
        
        // Draw Labels (X-Axis: Max 5 labels to keep tidy)
        val paint = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 10.dp.toPx()
            textAlign = Paint.Align.CENTER
        }
        
        val stepLabel = (dataPoints.size / 5).coerceAtLeast(1)
        dataPoints.forEachIndexed { index, pair ->
            if (index % stepLabel == 0 || index == dataPoints.size - 1) {
                val x = paddingLeft + index * stepX
                drawContext.canvas.nativeCanvas.drawText(
                    pair.first,
                    x,
                    height - 10.dp.toPx(),
                    paint
                )
            }
        }
    }
}

@Composable
fun HourlyPeakBarChart(
    hourlyData: List<Pair<Int, Int>>, // Hour (0-23) to transaction count
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF4CAF50),
    gridColor: Color = Color.Gray.copy(alpha = 0.2f)
) {
    if (hourlyData.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Tidak ada data", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val maxVal = hourlyData.maxOf { it.second }.coerceAtLeast(1)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingLeft = 40.dp.toPx()
        val paddingRight = 20.dp.toPx()
        val paddingTop = 20.dp.toPx()
        val paddingBottom = 40.dp.toPx()

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        // Draw horizontal grid lines
        val gridCount = 3
        for (i in 0..gridCount) {
            val y = paddingTop + chartHeight * (i.toFloat() / gridCount)
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(paddingLeft + chartWidth, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw Bars
        val barCount = hourlyData.size
        val totalSpacing = chartWidth * 0.2f
        val barWidth = (chartWidth - totalSpacing) / barCount
        val barSpacing = totalSpacing / (barCount + 1)

        val paint = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 9.dp.toPx()
            textAlign = Paint.Align.CENTER
        }

        hourlyData.forEachIndexed { index, pair ->
            val x = paddingLeft + barSpacing + index * (barWidth + barSpacing)
            val barHeight = chartHeight * (pair.second.toFloat() / maxVal.toFloat())
            val y = paddingTop + chartHeight - barHeight

            drawRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )

            // Draw hourly labels at X-Axis
            if (index % 4 == 0 || index == barCount - 1) {
                drawContext.canvas.nativeCanvas.drawText(
                    "${pair.first}:00",
                    x + barWidth / 2,
                    height - 10.dp.toPx(),
                    paint
                )
            }
        }
    }
}

@Composable
fun PaymentsDonutChart(
    paymentSegments: List<Pair<String, Double>>, // Payment method to amount
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        Color(0xFFE91E63), // Pink
        Color(0xFF9C27B0), // Purple
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFFFC107)  // Yellow
    )
) {
    if (paymentSegments.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Tidak ada data", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val totalAmount = paymentSegments.sumOf { it.second }.coerceAtLeast(1.0)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val minDim = min(width, height)
        val radius = minDim / 2 * 0.8f
        val strokeWidth = 30.dp.toPx()

        var currentAngle = -90f

        paymentSegments.forEachIndexed { index, pair ->
            val sweepAngle = (pair.second / totalAmount * 360f).toFloat()
            val color = colors[index % colors.size]

            drawArc(
                color = color,
                startAngle = currentAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset((width - radius * 2) / 2, (height - radius * 2) / 2),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth)
            )

            currentAngle += sweepAngle
        }
    }
}

@Composable
fun CircularProgressIndicatorWidget(
    progressPercent: Double,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF4CAF50),
    trackColor: Color = Color.LightGray.copy(alpha = 0.3f),
    strokeWidth: Float = 20f
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val minDim = min(width, height)
        val radius = minDim / 2 * 0.9f

        // Draw background track arc
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset((width - radius * 2) / 2, (height - radius * 2) / 2),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth)
        )

        // Draw animated progress arc
        val sweepAngle = (progressPercent.toFloat() / 100f * 360f).coerceIn(0f, 360f)
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset((width - radius * 2) / 2, (height - radius * 2) / 2),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}
