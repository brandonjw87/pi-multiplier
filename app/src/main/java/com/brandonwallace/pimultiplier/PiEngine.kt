package com.brandonwallace.pimultiplier

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

/**
 * Computes pi from scratch with the Chudnovsky series (binary splitting) and an
 * integer Newton square root. Results are cached; asking for more digits than the
 * cache holds triggers a bigger computation.
 */
object PiEngine {
    @Volatile private var cached: BigDecimal? = null
    @Volatile private var cachedDigits = 0

    private val C3_OVER_24: BigInteger =
        BigInteger.valueOf(640320L).pow(3).divide(BigInteger.valueOf(24L))

    /** Pi truncated to [decimals] decimal places. */
    @Synchronized
    fun pi(decimals: Int): BigDecimal {
        val c = cached
        if (c != null && cachedDigits >= decimals) return c.setScale(decimals, RoundingMode.DOWN)
        val target = maxOf(decimals, cachedDigits * 2, 1_100)
        val v = compute(target)
        cached = v
        cachedDigits = target
        return v.setScale(decimals, RoundingMode.DOWN)
    }

    /** The first [count] digits after "3.". */
    fun decimalDigits(count: Int): String =
        pi(count).unscaledValue().toString().substring(1, count + 1)

    private class PQT(val p: BigInteger, val q: BigInteger, val t: BigInteger)

    private fun compute(digits: Int): BigDecimal {
        val d = digits + 12 // guard digits
        val terms = (d / 14.181647462725477).toLong() + 2
        val s = split(0, terms)
        val one = BigInteger.TEN.pow(d)
        val sqrt10005 = isqrt(BigInteger.valueOf(10005L).multiply(one).multiply(one))
        val piScaled = BigInteger.valueOf(426880L).multiply(sqrt10005).multiply(s.q).divide(s.t)
        return BigDecimal(piScaled, d).setScale(digits, RoundingMode.DOWN)
    }

    private fun split(a: Long, b: Long): PQT {
        if (b - a == 1L) {
            val p: BigInteger
            val q: BigInteger
            if (a == 0L) {
                p = BigInteger.ONE; q = BigInteger.ONE
            } else {
                p = BigInteger.valueOf(6 * a - 5)
                    .multiply(BigInteger.valueOf(2 * a - 1))
                    .multiply(BigInteger.valueOf(6 * a - 1))
                q = BigInteger.valueOf(a).pow(3).multiply(C3_OVER_24)
            }
            var t = p.multiply(BigInteger.valueOf(13591409L + 545140134L * a))
            if (a % 2 == 1L) t = t.negate()
            return PQT(p, q, t)
        }
        val m = (a + b) / 2
        val l = split(a, m)
        val r = split(m, b)
        return PQT(l.p.multiply(r.p), l.q.multiply(r.q), l.t.multiply(r.q).add(l.p.multiply(r.t)))
    }

    private fun isqrt(n: BigInteger): BigInteger {
        if (n.signum() == 0) return n
        var x = BigInteger.ONE.shiftLeft((n.bitLength() + 1) / 2)
        while (true) {
            val y = x.add(n.divide(x)).shiftRight(1)
            if (y >= x) return x
            x = y
        }
    }
}

/** Exact BigDecimal helpers built on [PiEngine]. */
object PiMath {
    private const val LIMIT = 3_000

    fun parse(text: String): BigDecimal? {
        val t = text.trim().replace(",", "").replace(" ", "")
        if (t.isEmpty() || t == "-" || t == ".") return null
        return try {
            val bd = BigDecimal(t)
            if (bd.precision() - bd.scale() > LIMIT || bd.scale() > LIMIT) null else bd
        } catch (e: NumberFormatException) {
            null
        } catch (e: ArithmeticException) {
            null
        }
    }

    fun intDigits(n: BigDecimal): Int = maxOf(1, n.precision() - n.scale())

    fun timesPi(n: BigDecimal, decimals: Int): BigDecimal =
        n.multiply(PiEngine.pi(decimals + intDigits(n) + 8)).setScale(decimals, RoundingMode.HALF_EVEN)

    fun divPi(n: BigDecimal, decimals: Int): BigDecimal {
        val pi = PiEngine.pi(decimals + intDigits(n) + 12)
        val mc = MathContext(intDigits(n) + decimals + 10, RoundingMode.HALF_EVEN)
        return n.divide(pi, mc).setScale(decimals, RoundingMode.HALF_EVEN)
    }

    /** Short human label (about 7 significant digits). */
    fun short(n: BigDecimal, sig: Int = 7): String {
        if (n.signum() == 0) return "0"
        val r = n.round(MathContext(sig, RoundingMode.HALF_EVEN)).stripTrailingZeros()
        val mag = r.precision() - r.scale()
        return if (mag > 12 || mag < -5) r.toString() else r.toPlainString()
    }

    class Approx(val num: BigInteger, val den: BigInteger, val error: BigDecimal)

    /** Continued-fraction convergents h/k with N ~= (h/k)*pi. */
    fun piFractions(n: BigDecimal, maxDen: Long = 100_000L): List<Approx> {
        val mc = MathContext(80, RoundingMode.HALF_EVEN)
        val pi = PiEngine.pi(120)
        val x = n.divide(pi, mc)
        val out = ArrayList<Approx>()
        var h0 = BigInteger.ZERO; var h1 = BigInteger.ONE
        var k0 = BigInteger.ONE; var k1 = BigInteger.ZERO
        var rem = x
        val tiny = BigDecimal("1E-60")
        for (i in 0 until 14) {
            val a = rem.setScale(0, RoundingMode.FLOOR).toBigInteger()
            val h = a.multiply(h1).add(h0)
            val k = a.multiply(k1).add(k0)
            if (k > BigInteger.valueOf(maxDen)) break
            if (k.signum() > 0) {
                val approx = BigDecimal(h).multiply(pi).divide(BigDecimal(k), mc)
                out.add(Approx(h, k, n.subtract(approx)))
            }
            h0 = h1; h1 = h; k0 = k1; k1 = k
            val frac = rem.subtract(BigDecimal(a))
            if (frac.abs() < tiny) break
            rem = BigDecimal.ONE.divide(frac, mc)
        }
        return out
    }
}
