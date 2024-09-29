package pl.gotrainey.gotrainey.data_classes

data class Trip(
    var trainId: String? = null,
    var trainNumber: String? = null,
    var paths: MutableList<Path>? = mutableListOf()
)
