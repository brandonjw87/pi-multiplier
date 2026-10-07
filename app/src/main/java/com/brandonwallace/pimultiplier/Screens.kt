package com.brandonwallace.pimultiplier

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val screenPadding = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)

@Composable
private fun ScrollScreen(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .then(screenPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        content()
        Spacer(Modifier.height(12.dp))
    }
}

private val TRY_NUMBERS = listOf("1" to "1", "2 (τ)" to "2", "½" to "0.5", "113" to "113", "−1" to "-1", "10⁶" to "1000000", "e" to "2.718281828459045")

// ==================== 1. Multiply ====================

@Composable
fun MultiplyScreen(input: String, onInput: (String) -> Unit) {
    var decimals by rememberSaveable { mutableIntStateOf(50) }
    val n = PiMath.parse(input)
    val result = remember(input, decimals) { n?.let { PiMath.timesPi(it, decimals).toPlainString() } }
    ScrollScreen {
        ScreenHeader("π Multiplier", "Any number × π, exact to 1,000 decimal places")
        NumberField(input, onInput, "Your number  N", error = input.isNotBlank() && n == null)
        QuickChips(TRY_NUMBERS, onInput)
        PrecisionControl(decimals, { decimals = it }, 2, 1000, listOf(10, 50, 100, 250, 500, 1000))
        if (n != null && result != null) {
            val label = n.stripTrailingZeros().toPlainString()
            ResultDisplay("$label × π =", result, "$label × π = $result\n— via Pi Multiplier")
        }
        FunFactsCard()
    }
}

// ==================== 2. Circle ====================

@Composable
fun CircleScreen(input: String, onInput: (String) -> Unit) {
    var isDiameter by rememberSaveable { mutableStateOf(false) }
    var decimals by rememberSaveable { mutableIntStateOf(12) }
    val n = PiMath.parse(input)
    ScrollScreen {
        ScreenHeader("Circle mode", "Treat N as a radius or a diameter")
        NumberField(input, onInput, if (isDiameter) "Diameter  d" else "Radius  r", error = input.isNotBlank() && n == null)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(false to "Radius", true to "Diameter").forEach { (d, label) ->
                FilterChip(
                    selected = isDiameter == d, onClick = { isDiameter = d }, label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Gold, selectedLabelColor = Color(0xFF2A1A00))
                )
            }
        }
        if (n != null) {
            val size = n.abs()
            val r: BigDecimal = if (isDiameter) size.divide(BigDecimal(2)) else size
            val d: BigDecimal = r.multiply(BigDecimal(2))
            val vals = remember(input, isDiameter, decimals) {
                val pi = PiEngine.pi(decimals + 3 * PiMath.intDigits(r) + 12)
                val r2 = r.multiply(r)
                val r3 = r2.multiply(r)
                val hu = RoundingMode.HALF_EVEN
                listOf(
                    d.multiply(pi).setScale(decimals, hu),
                    r2.multiply(pi).setScale(decimals, hu),
                    r2.multiply(BigDecimal(4)).multiply(pi).setScale(decimals, hu),
                    r3.multiply(BigDecimal(4)).multiply(pi).divide(BigDecimal(3), decimals, hu),
                )
            }
            PiCard {
                Column(Modifier.padding(14.dp)) {
                    RollingCircle(
                        restartKey = "$input|$isDiameter",
                        dLabel = PiMath.short(d),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Roll it once: the circumference unrolls to π diameters ≈ ${PiMath.short(vals[0])}",
                        style = MaterialTheme.typography.bodyMedium, color = Lavender
                    )
                }
            }
            if (n.signum() < 0) {
                Text("Negative size? Geometry uses |N| = ${PiMath.short(size)}.", color = Lavender, style = MaterialTheme.typography.bodySmall)
            }
            PrecisionControl(decimals, { decimals = it }, 2, 200, listOf(4, 12, 25, 50, 100, 200))
            ValueRow("Circumference", "2πr = πd", vals[0].toPlainString())
            ValueRow("Area", "πr²", vals[1].toPlainString())
            ValueRow("Sphere surface", "4πr²", vals[2].toPlainString())
            ValueRow("Sphere volume", "⁴⁄₃πr³", vals[3].toPlainString())
            Text("Tap any value to copy it.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RollingCircle(restartKey: Any, dLabel: String, modifier: Modifier = Modifier) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(restartKey) {
        while (true) {
            p.snapTo(0f)
            delay(400)
            p.animateTo(1f, tween(2800, easing = FastOutSlowInEasing))
            delay(2000)
        }
    }
    val measurer = rememberTextMeasurer()
    val small = TextStyle(color = Lavender, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    val goldStyle = small.copy(color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    Canvas(modifier.aspectRatio(2.1f)) {
        val margin = 6.dp.toPx()
        val labelBand = 22.dp.toPx()
        val baseY = size.height - 20.dp.toPx()
        val twoPi = (2 * PI).toFloat()
        val radius = minOf((size.width - 2 * margin) / (twoPi + 2f), (baseY - labelBand) / 2f)
        val x0 = margin + radius
        val prog = p.value
        val theta = prog * twoPi
        val cx = x0 + theta * radius
        val cy = baseY - radius
        val stroke = 3.dp.toPx()

        // ground + diameter ticks
        drawLine(Lavender.copy(alpha = 0.25f), Offset(margin, baseY), Offset(size.width - margin, baseY), 1.dp.toPx())
        for (k in 0..3) {
            val x = x0 + k * 2 * radius
            drawLine(Lavender.copy(alpha = 0.6f), Offset(x, baseY - 5.dp.toPx()), Offset(x, baseY + 5.dp.toPx()), 1.5.dp.toPx())
            if (k > 0) drawText(measurer, "${k}d", topLeft = Offset(x - 7.dp.toPx(), baseY + 5.dp.toPx()), style = small)
        }
        // unrolled circumference
        drawLine(Gold, Offset(x0, baseY), Offset(cx, baseY), stroke, cap = StrokeCap.Round)
        // cycloid trail of the marker
        val path = Path()
        for (i in 0..72) {
            val th = theta * i / 72f
            val px = x0 + th * radius - radius * sin(th)
            val py = baseY - radius + radius * cos(th)
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(path, Gold.copy(alpha = 0.4f), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
        // glowing disc
        drawCircle(
            Brush.radialGradient(listOf(DeepPurple.copy(alpha = 0.75f), Color.Transparent), center = Offset(cx, cy), radius = radius * 1.5f),
            radius * 1.5f, Offset(cx, cy)
        )
        drawCircle(Lavender.copy(alpha = 0.85f), radius, Offset(cx, cy), style = Stroke(stroke))
        drawArc(
            Gold, startAngle = 90f, sweepAngle = prog * 360f, useCenter = false,
            topLeft = Offset(cx - radius, cy - radius), size = Size(2 * radius, 2 * radius),
            style = Stroke(stroke + 1.dp.toPx(), cap = StrokeCap.Round)
        )
        // rotating diameter + marker
        val mx = cx - radius * sin(theta)
        val my = cy + radius * cos(theta)
        drawLine(Lavender.copy(alpha = 0.7f), Offset(2 * cx - mx, 2 * cy - my), Offset(mx, my), 1.5.dp.toPx())
        drawCircle(Gold, 4.5.dp.toPx(), Offset(mx, my))
        // live labels
        drawText(measurer, "d = $dLabel", topLeft = Offset(margin, 0f), style = small)
        val live = if (prog >= 0.999f) "C = π·d" else String.format("%.3f × d", prog * PI)
        val layout = measurer.measure(live, goldStyle)
        drawText(layout, topLeft = Offset(size.width - margin - layout.size.width, 0f))
    }
}

// ==================== 3. Times table ====================

@Composable
fun TableScreen() {
    var decimals by rememberSaveable { mutableIntStateOf(10) }
    val context = LocalContext.current
    val rows = remember(decimals) {
        val pi = PiEngine.pi(decimals + 10)
        (1..100).map { k ->
            val v = BigDecimal(k).multiply(pi)
            val frac = v.subtract(v.setScale(0, RoundingMode.FLOOR))
            val dist = minOf(frac, BigDecimal.ONE.subtract(frac))
            Triple(k, v.setScale(decimals, RoundingMode.HALF_EVEN).toPlainString(), dist < BigDecimal("0.02"))
        }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ScreenHeader("π Times Table", "1 to 100 × π · ★ = near-miss whole number · tap a row to copy")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(4, 10, 20, 40).forEach { q ->
                        FilterChip(
                            selected = decimals == q, onClick = { decimals = q }, label = { Text("$q decimals") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Gold, selectedLabelColor = Color(0xFF2A1A00))
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
        itemsIndexed(rows, key = { _, r -> r.first }) { idx, (k, v, near) ->
            val bg = when {
                near -> Gold.copy(alpha = 0.16f)
                k % 10 == 0 -> DeepPurple.copy(alpha = 0.55f)
                idx % 2 == 0 -> CardBg
                else -> Color(0xB3180E33)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .clickable { copyToClipboard(context, v) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$k × π", color = if (near) Gold else Lavender, fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace, modifier = Modifier.width(84.dp)
                )
                Text(v, fontFamily = FontFamily.Monospace, fontSize = 15.sp, modifier = Modifier.weight(1f))
                val tag = when {
                    near -> "★"
                    k == 1 -> "π"
                    k == 2 -> "τ"
                    else -> ""
                }
                Text(tag, color = Gold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ==================== 4. Reverse ====================

@Composable
fun ReverseScreen(input: String, onInput: (String) -> Unit) {
    var decimals by rememberSaveable { mutableIntStateOf(30) }
    val n = PiMath.parse(input)
    val result = remember(input, decimals) { n?.let { PiMath.divPi(it, decimals).toPlainString() } }
    ScrollScreen {
        ScreenHeader("Reverse π", "How many π's fit in your number?  N ÷ π")
        NumberField(input, onInput, "Your number  N", error = input.isNotBlank() && n == null)
        QuickChips(listOf("22" to "22", "355" to "355", "10" to "10", "360" to "360", "1000" to "1000", "9.8696" to "9.8696044"), onInput)
        PrecisionControl(decimals, { decimals = it }, 2, 1000, listOf(10, 30, 100, 500, 1000))
        if (n != null && result != null) {
            val label = n.stripTrailingZeros().toPlainString()
            ResultDisplay("$label ÷ π =", result, "$label ÷ π = $result\n— via Pi Multiplier")
            val info = remember(input) {
                val k = PiMath.divPi(n, 30)
                Triple(
                    PiMath.piFractions(n),
                    PiMath.divPi(n.multiply(BigDecimal(180)), 8),
                    k
                )
            }
            PiCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("What that means", style = MaterialTheme.typography.titleMedium, color = Gold)
                    Text("• $label is about ${PiMath.short(info.third)} π's.")
                    Text("• A circle with circumference $label has diameter ${PiMath.short(info.third)} and radius ${PiMath.short(info.third.divide(BigDecimal(2)))}.")
                    Text("• As an angle, $label radians = ${PiMath.short(info.second, 10)}° (${PiMath.short(info.third.divide(BigDecimal(2)))} full turns).")
                }
            }
            if (info.first.isNotEmpty() && n.signum() != 0) {
                PiCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Best π-fractions", style = MaterialTheme.typography.titleMedium, color = Gold)
                        Text("Continued-fraction approximations  N ≈ (a/b)·π", style = MaterialTheme.typography.labelMedium, color = Lavender)
                        info.first.takeLast(6).forEach { a ->
                            val frac = if (a.den == java.math.BigInteger.ONE) "${a.num}π" else "(${a.num}/${a.den})·π"
                            val err = if (a.error.signum() == 0 || a.error.abs() < BigDecimal("1E-50")) "exact" else "off by ${PiMath.short(a.error.abs(), 3)}"
                            Row {
                                Text(frac, fontFamily = FontFamily.Monospace, color = GoldSoft, modifier = Modifier.weight(1f))
                                Text(err, style = MaterialTheme.typography.labelMedium, color = Lavender)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== 5. Digit hunt ====================

@Composable
fun HuntScreen(digits: String) {
    var query by rememberSaveable { mutableStateOf("0314") }
    val focus = LocalFocusManager.current
    val q = query.filter { it.isDigit() }
    val positions = remember(q) {
        if (q.isEmpty()) emptyList() else {
            val out = ArrayList<Int>()
            var i = digits.indexOf(q)
            while (i >= 0) { out.add(i); i = digits.indexOf(q, i + 1) }
            out
        }
    }
    val first = positions.firstOrNull()
    val marks = remember(q) {
        val arr = BooleanArray(digits.length)
        positions.forEach { s -> for (j in s until minOf(s + q.length, digits.length)) arr[j] = true }
        arr
    }
    val streamState = rememberLazyListState()
    LaunchedEffect(first) { if (first != null) streamState.animateScrollToItem(maxOf(0, first / 50 - 1)) }

    ScrollScreen {
        ScreenHeader("Digit Hunt", "Find your digits in the first 10,000 decimals of π")
        OutlinedTextField(
            value = query,
            onValueChange = { v -> query = v.filter { it.isDigit() }.take(12) },
            label = { Text("Digits to find (try a birthday, MMDD)") },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold, focusedLabelColor = Gold, cursorColor = Gold,
                unfocusedContainerColor = CardBg, focusedContainerColor = CardBg,
            ),
            modifier = Modifier.fillMaxWidth()
        )
        QuickChips(
            listOf("0314 (Pi Day)" to "0314", "0329" to "0329", "1225" to "1225", "999999" to "999999", "1031" to "1031", "007" to "007", "42" to "42"),
        ) { query = it }

        PiCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    q.isEmpty() -> Text("Type some digits to start hunting.", color = Lavender)
                    first == null -> {
                        Text("Not in the first 10,000 decimals", style = MaterialTheme.typography.titleLarge, color = Gold)
                        Text(
                            "A random ${q.length}-digit string needs about 10^${q.length} digits of π on average. Try a shorter one!",
                            color = Lavender
                        )
                    }
                    else -> {
                        Text("Decimal place ${"%,d".format(first + 1)}", style = MaterialTheme.typography.headlineMedium, color = Gold, fontWeight = FontWeight.Bold)
                        Text(
                            if (positions.size == 1) "\"$q\" appears once in the first 10,000 decimals."
                            else "\"$q\" appears ${positions.size} times; first at place ${first + 1}.",
                            color = Lavender
                        )
                        val from = maxOf(0, first - 24)
                        val to = minOf(digits.length, first + q.length + 24)
                        val ctx = buildAnnotatedString {
                            append(if (from == 0) "3." else "…")
                            withStyle(SpanStyle(color = Lavender)) { append(digits.substring(from, first)) }
                            withStyle(SpanStyle(background = Gold, color = Color(0xFF2A1A00), fontWeight = FontWeight.Bold)) {
                                append(digits.substring(first, first + q.length))
                            }
                            withStyle(SpanStyle(color = Lavender)) { append(digits.substring(first + q.length, to)) }
                            append("…")
                        }
                        Text(ctx, fontFamily = FontFamily.Monospace, fontSize = 16.sp, lineHeight = 24.sp)
                        // occurrence strip
                        Canvas(Modifier.fillMaxWidth().height(26.dp)) {
                            drawRoundRect(Lavender.copy(alpha = 0.15f), size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f))
                            positions.forEach { pos ->
                                val x = size.width * pos / digits.length
                                val isFirst = pos == first
                                drawLine(
                                    if (isFirst) Gold else Lavender.copy(alpha = 0.7f),
                                    Offset(x, if (isFirst) 0f else size.height * 0.25f),
                                    Offset(x, if (isFirst) size.height else size.height * 0.75f),
                                    if (isFirst) 3.dp.toPx() else 1.5.dp.toPx()
                                )
                            }
                        }
                        Row { Text("1", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f)); Text("10,000", style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }

        // the digit stream itself, scrolled to the match
        PiCard {
            Column(Modifier.padding(12.dp)) {
                Text("π digit stream", style = MaterialTheme.typography.titleSmall, color = Gold)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(300.dp)) {
                    LazyColumn(state = streamState, modifier = Modifier.fillMaxHeight()) {
                        items(digits.length / 50) { row ->
                            val line = buildAnnotatedString {
                                withStyle(SpanStyle(color = Lavender.copy(alpha = 0.55f))) { append("%5d ".format(row * 50 + 1)) }
                                for (j in 0 until 50) {
                                    val g = row * 50 + j
                                    if (j > 0 && j % 10 == 0) append(' ')
                                    val style = when {
                                        first != null && g >= first && g < first + q.length ->
                                            SpanStyle(background = Gold, color = Color(0xFF2A1A00), fontWeight = FontWeight.Bold)
                                        marks[g] -> SpanStyle(color = Gold, fontWeight = FontWeight.Bold)
                                        else -> SpanStyle(color = Color(0xFFE6DDFF))
                                    }
                                    withStyle(style) { append(digits[g]) }
                                }
                            }
                            Text(line, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp, lineHeight = 16.sp, softWrap = false)
                        }
                    }
                }
            }
        }

        DigitFrequencyCard(digits)
    }
}

@Composable
private fun DigitFrequencyCard(digits: String) {
    val counts = remember(digits) { IntArray(10).also { c -> digits.forEach { c[it - '0']++ } } }
    val max = counts.max().toFloat()
    PiCard {
        Column(Modifier.padding(16.dp)) {
            Text("Digit census (first 10,000)", style = MaterialTheme.typography.titleSmall, color = Gold)
            Text("Remarkably even — π is believed to be a 'normal' number.", style = MaterialTheme.typography.labelMedium, color = Lavender)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                counts.forEachIndexed { d, c ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("$c", fontSize = 9.sp, color = Lavender)
                        Box(
                            Modifier
                                .padding(horizontal = 4.dp)
                                .fillMaxWidth()
                                .height((80 * c / max).dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(Brush.verticalGradient(listOf(Gold, DeepPurple)))
                        )
                        Text("$d", fontWeight = FontWeight.Bold, color = Gold)
                    }
                }
            }
        }
    }
}
