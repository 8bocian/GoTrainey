package pl.gotrainey.gotrainey.data_classes

import java.time.LocalDateTime

data class Path(
    val startStationName: String,
    val startStationSlug: String,
    val endStationName: String,
    val endStationSlug: String,
    val departureDateTime: LocalDateTime,
    val arrivalDateTime: LocalDateTime
)
