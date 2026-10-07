package com.brandonwallace.pimultiplier

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// ---------- platform helpers ----------

fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(ClipboardManager::class.java)
    cm?.setPrimaryClip(ClipData.newPlainText("Pi Multiplier", text))
    if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Share result"))
}

// ---------- animated background ----------

@Composable
fun PiDigitRain(digits: String, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp, color = Color.White)
    val glyphs = remember(measurer) { (0..9).map { measurer.measure(it.toString(), style) } }
    val t by rememberInfiniteTransition(label = "rain").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(120_000, easing = LinearEasing)),
        label = "t"
    )
    Canvas(modifier) {
        val cell = 26.dp.toPx()
        val cols = (size.width / cell).toInt() + 1
        val rows = (size.height / cell).toInt() + 2
        val totalH = rows * cell
        val twoPi = (2 * PI).toFloat()
        for (c in 0 until cols) {
            val laps = 1 + (c * 7) % 3
            val offset = ((t * laps) % 1f) * totalH
            val x = c * cell + (cell - glyphs[0].size.width) / 2f
            val tint = if (c % 3 == 0) Gold else Lavender
            for (r in 0 until rows) {
                var y = r * cell + offset
                if (y >= totalH) y -= totalH
                y -= cell
                val d = digits[(c * rows + r) % digits.length] - '0'
                val wave = 0.5f + 0.5f * sin(t * twoPi * 20f + c * 0.9f + r * 0.35f)
                val alpha = 0.035f + 0.075f * wave * wave
                drawText(glyphs[d.coerceIn(0, 9)], color = tint, topLeft = Offset(x, y), alpha = alpha)
            }
        }
    }
}

// ---------- building blocks ----------

@Composable
fun PiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val colors = CardDefaults.cardColors(containerColor = CardBg)
    val border = BorderStroke(
        1.dp,
        Brush.linearGradient(listOf(Gold.copy(alpha = 0.55f), Lavender.copy(alpha = 0.12f), Gold.copy(alpha = 0.30f)))
    )
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, border = border, content = content)
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, border = border, content = content)
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String) {
    Column(Modifier.padding(top = 8.dp, bottom = 2.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Gold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun NumberField(value: String, onValueChange: (String) -> Unit, label: String, error: Boolean) {
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = { new -> onValueChange(new.filter { it.isDigit() || it in ".-,eE+" }.take(120)) },
        label = { Text(label) },
        singleLine = true,
        isError = error,
        supportingText = if (error) ({ Text("Enter a number like 42, -3.5 or 1e6") }) else null,
        textStyle = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        trailingIcon = {
            TextButton(onClick = {
                onValueChange(if (value.startsWith("-")) value.drop(1) else "-$value")
            }) { Text("±", fontSize = 22.sp, color = Gold) }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold,
            focusedLabelColor = Gold,
            cursorColor = Gold,
            unfocusedContainerColor = CardBg,
            focusedContainerColor = CardBg,
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun QuickChips(options: List<Pair<String, String>>, onPick: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (label, v) ->
            FilterChip(selected = false, onClick = { onPick(v) }, label = { Text(label) })
        }
    }
}

@Composable
fun PrecisionControl(
    value: Int,
    onChange: (Int) -> Unit,
    min: Int,
    max: Int,
    quick: List<Int>,
    label: String = "Decimal places",
) {
    fun toPos(v: Int) = sqrt((v - min).toFloat() / (max - min))
    fun fromPos(p: Float) = (min + (max - min) * p * p).roundToInt().coerceIn(min, max)
    PiCard {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                FilledTonalIconButton(onClick = { onChange((value - 1).coerceAtLeast(min)) }) { Text("−", fontSize = 20.sp) }
                Text(
                    "$value", style = MaterialTheme.typography.titleLarge, color = Gold,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 64.dp)
                )
                FilledTonalIconButton(onClick = { onChange((value + 1).coerceAtMost(max)) }) { Text("+", fontSize = 20.sp) }
            }
            Slider(
                value = toPos(value),
                onValueChange = { onChange(fromPos(it)) },
                colors = SliderDefaults.colors(
                    thumbColor = Gold, activeTrackColor = Gold,
                    inactiveTrackColor = Gold.copy(alpha = 0.18f)
                )
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quick.forEach { q ->
                    FilterChip(
                        selected = value == q, onClick = { onChange(q) }, label = { Text("$q") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold, selectedLabelColor = Color(0xFF2A1A00)
                        )
                    )
                }
            }
        }
    }
}

/**
 * Big result with a digit-reveal animation: when the value changes, only the digits
 * that differ from the previous value "type in", with a glowing leading edge.
 */
@Composable
fun ResultDisplay(title: String, value: String, shareMessage: String) {
    val context = LocalContext.current
    var prev by remember { mutableStateOf("") }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(value) {
        val start = value.commonPrefixWith(prev).length
        prev = value
        reveal.snapTo(start.toFloat())
        val remaining = value.length - start
        reveal.animateTo(
            value.length.toFloat(),
            tween(durationMillis = (300 + remaining * 2).coerceAtMost(1500), easing = LinearOutSlowInEasing)
        )
    }
    val shown = reveal.value.toInt().coerceIn(0, value.length)
    val dot = value.indexOf('.')
    val headLen = if (dot < 0) value.length else minOf(value.length, dot + 7)
    val glowFrom = if (shown < value.length) shown - 8 else Int.MAX_VALUE

    fun styleFor(i: Int, base: Color): SpanStyle = when {
        i >= shown -> SpanStyle(color = Color.Transparent)
        i >= glowFrom -> SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)
        else -> SpanStyle(color = base)
    }

    val head: AnnotatedString = buildAnnotatedString {
        for (i in 0 until headLen) withStyle(styleFor(i, Gold)) { append(value[i]) }
    }
    val tail: AnnotatedString = buildAnnotatedString {
        for (i in headLen until value.length) {
            val di = i - dot - 1
            if (di > 0 && di % 10 == 0) append(' ')
            withStyle(styleFor(i, Lavender)) { append(value[i]) }
        }
    }
    val decimals = if (dot < 0) 0 else value.length - dot - 1
    val headStyle = when {
        headLen <= 12 -> MaterialTheme.typography.displaySmall
        headLen <= 20 -> MaterialTheme.typography.headlineMedium
        else -> MaterialTheme.typography.titleLarge
    }

    PiCard(onClick = { copyToClipboard(context, value) }) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(head, style = headStyle.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold))
            if (tail.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(tail, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp, lineHeight = 22.sp))
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$decimals decimal places · tap to copy",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = { copyToClipboard(context, value) }) { Text("Copy") }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = { shareText(context, shareMessage) }) { Text("Share") }
            }
        }
    }
}

@Composable
fun ValueRow(label: String, formula: String, value: String) {
    val context = LocalContext.current
    PiCard(onClick = { copyToClipboard(context, value) }) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.titleMedium, color = Gold, modifier = Modifier.weight(1f))
                Text(formula, style = MaterialTheme.typography.labelLarge, color = Lavender)
            }
            Spacer(Modifier.height(4.dp))
            Text(value, fontFamily = FontFamily.Monospace, fontSize = 16.sp, lineHeight = 22.sp)
        }
    }
}

// ---------- fun facts ----------

private val FACTS = listOf(
    "π is irrational: its decimals never end and never fall into a repeating pattern (Lambert, 1761).",
    "π is transcendental (Lindemann, 1882) — which is why 'squaring the circle' with compass and straightedge is impossible.",
    "Starting at decimal place 762 there are six 9s in a row: the 'Feynman point'. Try 999999 in Digit Hunt!",
    "NASA JPL uses about 15 decimals of π for interplanetary navigation. ~40 decimals would measure the observable universe to within a hydrogen atom.",
    "Archimedes trapped π between 3 10/71 and 3 1/7 using 96-sided polygons.",
    "355/113, found by Zu Chongzhi in the 5th century, matches π to 6 decimal places.",
    "Pi Day is March 14 (3/14) — also Albert Einstein's birthday.",
    "This app computes its own π at launch with the Chudnovsky series: each term adds about 14 correct digits.",
    "In 1897 the Indiana 'Pi Bill' nearly put a wrong value of π into law — a visiting mathematician talked the senators out of it.",
    "e^(iπ) + 1 = 0 links five of the most important numbers in mathematics.",
    "Drop needles on lined paper and π shows up in how often they cross a line (Buffon's needle).",
    "Pick two random whole numbers: the chance they share no common factor is 6/π² ≈ 61%.",
    "7 × π = 21.991… is the closest whole-number near-miss in the 1–100 times table. Spot the ★ in the Table tab.",
    "A pizza with radius z and thickness a has volume pi·z·z·a.",
    "Record computations of π now exceed 300 trillion digits.",
)

@Composable
fun FunFactsCard() {
    var i by rememberSaveable { mutableIntStateOf(Random.nextInt(FACTS.size)) }
    LaunchedEffect(i) {
        delay(9_000)
        i = (i + 1) % FACTS.size
    }
    PiCard(onClick = { i = (i + 1) % FACTS.size }) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✦ π fun fact", style = MaterialTheme.typography.titleSmall, color = Gold, modifier = Modifier.weight(1f))
                Text(piDayCountdown(), style = MaterialTheme.typography.labelMedium, color = Lavender)
            }
            Spacer(Modifier.height(8.dp))
            AnimatedContent(
                targetState = i,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) },
                label = "fact"
            ) { idx ->
                Text(FACTS[idx], style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(6.dp))
            Text("tap for another", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun piDayCountdown(): String {
    val today = LocalDate.now()
    var next = LocalDate.of(today.year, 3, 14)
    if (today.isAfter(next)) next = next.plusYears(1)
    val days = ChronoUnit.DAYS.between(today, next)
    return if (days == 0L) "Happy Pi Day! 🥧" else "Pi Day in $days days"
}
