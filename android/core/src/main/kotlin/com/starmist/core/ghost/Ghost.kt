package com.starmist.core.ghost

/** Steps counted between two readings. The steps are taken to be spread evenly over the span. */
data class StepSpan(val fromMillis: Long, val toMillis: Long, val steps: Long) {
    init {
        require(toMillis >= fromMillis) { "span ends before it starts" }
    }
}

/**
 * "Yesterday's you": a ghost that walks the same way you walked yesterday. At any moment of today
 * it has covered what you had covered by the same time of day yesterday.
 */
object Ghost {
    /** The ghost stays hidden until it has walked this much, so a nearly empty day makes no ghost. */
    const val MIN_GHOST_STEPS = 500L

    /** Steps counted up to [atMillis]; a span that is still running counts in proportion. */
    fun cumulativeAt(spans: List<StepSpan>, atMillis: Long): Long {
        var total = 0.0
        for (span in spans) {
            when {
                span.toMillis <= atMillis -> total += span.steps
                span.fromMillis < atMillis -> {
                    val length = (span.toMillis - span.fromMillis).coerceAtLeast(1)
                    total += span.steps * (atMillis - span.fromMillis).toDouble() / length
                }
            }
        }
        return total.toLong()
    }

    /**
     * Where the ghost is now: yesterday's [spans], read [offsetIntoDayMillis] after yesterday's
     * midnight [yesterdayStartMillis].
     */
    fun stepsNow(spans: List<StepSpan>, yesterdayStartMillis: Long, offsetIntoDayMillis: Long): Long =
        cumulativeAt(spans, yesterdayStartMillis + offsetIntoDayMillis)

    data class Standing(val ghostSteps: Long, val mySteps: Long) {
        /** The ghost only exists once it has walked a little. */
        val present: Boolean get() = ghostSteps >= MIN_GHOST_STEPS

        /** Positive when ahead of the ghost. */
        val lead: Long get() = mySteps - ghostSteps

        /** True when you have just gone past a ghost that exists. */
        val overtaken: Boolean get() = present && mySteps > ghostSteps
    }
}
