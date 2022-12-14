package org.dblp.command

import java.time.LocalDate

data class WatchCheckResponseProperties(
    val issueKey: String,
    var expectedDateToBeResolved: LocalDate,
) {
    var daysLeft = LocalDate.now().until(expectedDateToBeResolved).days.toString()

    init {
        daysLeft =
            if (daysLeft.toInt() < 0) "Outdated"
            else if (daysLeft.toInt() == 1) "$daysLeft day"
            else "$daysLeft days"
    }
}