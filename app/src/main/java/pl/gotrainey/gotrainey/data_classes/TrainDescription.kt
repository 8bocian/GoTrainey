package pl.gotrainey.gotrainey.data_classes

import java.time.LocalDate

data class TrainDescription(
    val connectionId: String?,
    val trainId: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?
)
