package com.kasirinaja.store.ui.components

import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.compose.dimensions.dimensionsOf
import com.patrykandpatrick.vico.core.chart.dimensions.HorizontalDimensions
import com.patrykandpatrick.vico.core.chart.insets.Insets
import com.patrykandpatrick.vico.core.component.marker.MarkerComponent
import com.patrykandpatrick.vico.core.component.shape.DashedShape
import com.patrykandpatrick.vico.core.component.shape.ShapeComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.context.MeasureContext
import com.patrykandpatrick.vico.core.extension.copyColor
import com.patrykandpatrick.vico.core.marker.Marker

@Composable
fun rememberMarker(): Marker {
    val labelBackgroundColor = MaterialTheme.colorScheme.surface
    val labelBackground = remember(labelBackgroundColor) {
        ShapeComponent(shape = Shapes.pillShape, color = labelBackgroundColor.toArgb()).setShadow(
            radius = 4f,
            dy = 2f,
            applyElevationOverlay = true,
        )
    }
    val label = textComponent(
        background = labelBackground,
        lineCount = 1,
        padding = dimensionsOf(8.dp, 4.dp),
        typeface = Typeface.MONOSPACE,
        color = MaterialTheme.colorScheme.onSurface
    )
    val indicatorInnerComponent = shapeComponent(Shapes.pillShape, MaterialTheme.colorScheme.surface)
    val indicatorCenterComponent = shapeComponent(Shapes.pillShape, Color.White)
    val indicatorOuterComponent = shapeComponent(Shapes.pillShape, Color.White)
    val indicator = com.patrykandpatrick.vico.compose.component.overlayingComponent(
        outer = indicatorOuterComponent,
        inner = com.patrykandpatrick.vico.compose.component.overlayingComponent(
            outer = indicatorCenterComponent,
            inner = indicatorInnerComponent,
            innerPaddingAll = 5.dp,
        ),
        innerPaddingAll = 10.dp,
    )
    val guideline = com.patrykandpatrick.vico.compose.component.lineComponent(
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
        2.dp,
        DashedShape(Shapes.pillShape, 8f, 4f),
    )
    return remember(label, indicator, guideline) {
        object : MarkerComponent(label, indicator, guideline) {
            init {
                indicatorSizeDp = 36f
                onApplyEntryColor = { entryColor ->
                    indicatorOuterComponent.color = entryColor.copyColor(alpha = 32)
                    with(indicatorCenterComponent) {
                        color = entryColor
                        setShadow(radius = 12f, color = entryColor)
                    }
                }
            }
        }
    }
}
