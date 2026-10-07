package com.brandonwallace.pimultiplier

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent { PiTheme { PiApp() } }
    }
}

enum class Mode(val label: String) { Multiply("Multiply"), Circle("Circle"), Table("Table"), Reverse("Reverse"), Hunt("Hunt") }

@Composable
fun PiApp() {
    var digits by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        digits = withContext(Dispatchers.Default) {
            PiEngine.pi(10_100)
            PiEngine.decimalDigits(10_000)
        }
    }
    var mode by rememberSaveable { mutableStateOf(Mode.Multiply) }
    var input by rememberSaveable { mutableStateOf("2") }
    val holder = rememberSaveableStateHolder()

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Night, NightMid, Night)))
    ) {
        PiDigitRain(
            digits = digits.ifEmpty { "14159265358979323846264338327950288419716939937510" },
            modifier = Modifier.fillMaxSize()
        )
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = Color(0xF0170D30)) {
                    Mode.entries.forEach { m ->
                        NavigationBarItem(
                            selected = mode == m,
                            onClick = { mode = m },
                            icon = { NavGlyph(m) },
                            label = { Text(m.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF2A1A00),
                                selectedTextColor = Gold,
                                indicatorColor = Gold,
                                unselectedIconColor = Lavender,
                                unselectedTextColor = Lavender.copy(alpha = 0.75f),
                            )
                        )
                    }
                }
            }
        ) { pad ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .consumeWindowInsets(pad)
                    .imePadding()
            ) {
                if (digits.isEmpty()) {
                    Loading()
                } else {
                    AnimatedContent(
                        targetState = mode,
                        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                        label = "mode"
                    ) { m ->
                        holder.SaveableStateProvider(m.name) {
                            when (m) {
                                Mode.Multiply -> MultiplyScreen(input) { input = it }
                                Mode.Circle -> CircleScreen(input) { input = it }
                                Mode.Table -> TableScreen()
                                Mode.Reverse -> ReverseScreen(input) { input = it }
                                Mode.Hunt -> HuntScreen(digits)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Loading() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("π", fontSize = 72.sp, color = Gold, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        CircularProgressIndicator(color = Gold)
        Spacer(Modifier.height(16.dp))
        Text("Computing 10,000 digits of π from scratch…", color = Lavender, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun NavGlyph(mode: Mode) {
    val c = LocalContentColor.current
    when (mode) {
        Mode.Multiply -> Text("×π", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Mode.Reverse -> Text("÷π", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Mode.Circle -> Canvas(Modifier.size(24.dp)) {
            val w = 2.dp.toPx()
            val r = size.minDimension / 2 - w
            drawCircle(c, r, style = Stroke(w))
            drawLine(c, center, Offset(center.x + r, center.y), w, cap = StrokeCap.Round)
            drawCircle(c, w * 1.2f, center)
        }
        Mode.Table -> Canvas(Modifier.size(24.dp)) {
            val w = 1.8.dp.toPx()
            val inset = 3.dp.toPx()
            val s = size.width - inset * 2
            drawRoundRect(c, Offset(inset, inset), Size(s, s), CornerRadius(3.dp.toPx()), style = Stroke(w))
            for (i in 1..2) {
                val p = inset + s * i / 3f
                drawLine(c, Offset(p, inset), Offset(p, inset + s), w)
                drawLine(c, Offset(inset, p), Offset(inset + s, p), w)
            }
        }
        Mode.Hunt -> Canvas(Modifier.size(24.dp)) {
            val w = 2.2.dp.toPx()
            val cc = Offset(size.width * 0.42f, size.height * 0.42f)
            val r = size.width * 0.27f
            drawCircle(c, r, cc, style = Stroke(w))
            drawLine(
                c, Offset(cc.x + r * 0.72f, cc.y + r * 0.72f),
                Offset(size.width * 0.88f, size.height * 0.88f), w * 1.3f, cap = StrokeCap.Round
            )
        }
    }
}
