package pl.gotrainey.gotrainey

import android.os.Build.*
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.work.*
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import pl.gotrainey.gotrainey.adapters.CartsAdapter
import pl.gotrainey.gotrainey.adapters.StationAdapter
import pl.gotrainey.gotrainey.data_classes.Path
import pl.gotrainey.gotrainey.data_classes.TrainDescription
import pl.gotrainey.gotrainey.data_classes.Trip
import pl.gotrainey.gotrainey.databinding.ActivityMainBinding
import pl.gotrainey.gotrainey.schedulers.Scheduler
import pl.gotrainey.gotrainey.services.TrainApiService
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val trainApiService = TrainApiService()
    private lateinit var binding: ActivityMainBinding

    private lateinit var startStationAdapter: ArrayAdapter<String>
    private lateinit var endStationAdapter: ArrayAdapter<String>

    private val startStationsList = mutableListOf<String>("s1", "s", "2qe", "wdadwadw")
    private val endStationsList = mutableListOf<String>()

    private lateinit var cartsAdapter: CartsAdapter
    private val cartsList = mutableListOf<Map<String, Any>>()

    private val trip = Trip()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // START STATION ADAPTER

        startStationAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, startStationsList)
        binding.startStationDropDown.setAdapter(startStationAdapter)
        startStationsList.clear()
        startStationsList.addAll(mutableListOf("DAWDWA", "DAWWAD"))
        startStationAdapter.notifyDataSetChanged()
        Log.d("STATIONS", startStationsList.toString())

        binding.startStationDropDown.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()){
                    lifecycleScope.launch {
                        Log.d("STATIONS", s.toString())
                        val stations = trainApiService.findStation(s.toString())
                        Log.d("STATIONS LIST", stations.toString())
                        if (stations !== null){
                            fetchSuggestions(adapter = startStationAdapter, autoCompleteTextView = binding.startStationDropDown, listOfStations = startStationsList, newStations = parseJsonToListOfStations(stations))
                        }
                    }
                }
            }
        })

        // END STATION ADAPTER

        endStationAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, endStationsList)
        binding.endStationDropDown.setAdapter(endStationAdapter)
        binding.endStationDropDown.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrEmpty()){
                    lifecycleScope.launch {
                        val stations = trainApiService.findStation(s.toString())
                        Log.d("STATIONS", stations.toString())
                        if (stations !== null){
                            fetchSuggestions(adapter = endStationAdapter, autoCompleteTextView = binding.endStationDropDown, listOfStations = endStationsList, newStations = parseJsonToListOfStations(stations))
                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // CARTS ADAPTER

        cartsAdapter = CartsAdapter(cartsList)

        binding.cartsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.cartsRecyclerView.adapter = cartsAdapter

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.cartsRecyclerView)


        binding.trainNumber.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                Log.d("MainActivity", "Numer Pociągu changed: ${s.toString()}")
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        //TODO: AFTER USER GETS THE TRAIN THEN GIVE HIM SOME KIND OF A LIST WITH FREE PLACES IN EACH CART
        binding.search.setOnClickListener {
            val startStation: String = binding.startStationDropDown.text.toString()
            val endStation: String = binding.endStationDropDown.text.toString()
            val trainNumber: String = binding.trainNumber.text.toString()
            trip.trainNumber = trainNumber
            lifecycleScope.launch {

                val trainDescription: TrainDescription = findTrain(startStation, endStation, trainNumber, timeStopLimit = Pair(-1, 4), arrivalDate = null)

                if (trainDescription.trainId !== null && trainDescription.connectionId !== null && trainDescription.startDate !== null) {
                    val paths = getPaths(trainDescription.trainId, trainDescription.startDate, startStation, endStation)
                    trip.paths = paths
                    trip.trainId = trainDescription.trainId
                    var delayInMillis = 10000
                    paths.forEach{

                        val inputData = Data.Builder()
                            .putString("trainNumber", trainNumber)
                            .putString("startStationName", it.startStationName)
                            .putString("startStationSlug", it.startStationSlug)
                            .putString("endStationName", it.endStationName)
                            .putString("endStationSlug", it.endStationSlug)
                            .putString("departureDateTime", it.departureDateTime.toString())
                            .putString("arrivalDateTime", it.arrivalDateTime.toString())

                            .build()

//                        val delayInMillis = localDateTimeToCalendar(it.departureDateTime).timeInMillis - Calendar.getInstance().timeInMillis
                        delayInMillis += 10000
                        val workRequest = OneTimeWorkRequestBuilder<Scheduler>()
                            .setInitialDelay(delayInMillis.toLong(), TimeUnit.MILLISECONDS)
                            .setInputData(inputData)
                            .build()

                        WorkManager.getInstance(applicationContext).enqueue(workRequest)

                        WorkManager.getInstance(applicationContext).getWorkInfoByIdLiveData(workRequest.id)
                            .observe(this@MainActivity) { workInfo ->
                                if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
                                    lifecycleScope.launch {
                                        val subTrainDescription: TrainDescription = findTrain(
                                            workInfo.outputData.getString("startStationName").toString(),
                                            workInfo.outputData.getString("endStationName").toString(),
                                            workInfo.outputData.getString("trainNumber").toString(),
                                            timeStopLimit = Pair(0, 1),
                                            arrivalDate = workInfo.outputData.getString("arrivalDateTime"))
                                        if (subTrainDescription.connectionId !== null){
                                            binding.startStationDropDown.setText(workInfo.outputData.getString("startStationName").toString())
                                            binding.endStationDropDown.setText(workInfo.outputData.getString("endStationName").toString())
                                            val cartsJson = trainApiService.getTrainPlaces(
                                                subTrainDescription.connectionId,
                                                workInfo.outputData.getString("trainNumber").toString()
                                            )
                                            if (cartsJson !== null) {
                                                val carts = parseFreeSeats(cartsJson)
                                                updateCarts(cartsList = cartsList, adapter = cartsAdapter, carts = carts)
                                            }
                                        }
                                    }
                                }
                            }
                    }
                    }
//                Log.d("CONNECTION_ID", trainDescription.connectionId.toString())
//                if (trainDescription.connectionId !== null) {
//                    val cartsJson = trainApiService.getTrainPlaces(trainDescription.connectionId, trainNumber)
//                    if (cartsJson !== null) {
//                        val carts = parseFreeSeats(cartsJson)
//                        updateCarts(cartsList = cartsList, adapter = cartsAdapter, recyclerView = binding.cartsRecyclerView, carts = carts)
//                    }
//                }
            }


        }
    }

    fun localDateTimeToCalendar(localDateTime: LocalDateTime): Calendar {
        val instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant()

        val date = Date.from(instant)

        val calendar = Calendar.getInstance()
        calendar.time = date

        return calendar
    }

    fun parseJsonToListOfStations(json: JsonObject): MutableList<String> {
        val stationsArray = json.getAsJsonArray("stations")

        val listOfStations = mutableListOf<String>()

        for (i in 0 until stationsArray.size()) {
            val station = stationsArray[i].asJsonObject
            listOfStations.add(station.get("name").asString)
        }

        return listOfStations
    }

    private fun fetchSuggestions(adapter: ArrayAdapter<String>, autoCompleteTextView: AutoCompleteTextView, listOfStations: MutableList<String>, newStations: MutableList<String>) {

            startStationsList.clear()
            startStationsList.addAll(newStations)
            startStationAdapter.notifyDataSetChanged()
            Log.d("STATIONS", "${startStationAdapter.count}, ${startStationsList.toString()}")


    }

    private fun updateCarts(cartsList: MutableList<Map<String, Any>>, adapter: CartsAdapter, carts: List<Map<String, Any>>) {
        cartsList.clear()
        cartsList.addAll(carts)
        adapter.notifyDataSetChanged()
        Log.d("CHANGE", cartsList.toString())
//        recyclerView.visibility = if (cartsList.isEmpty()) View.GONE else View.VISIBLE
    }

    fun parseFreeSeats(jsonObject: JsonObject): MutableList<Map<String, Any>> {
        val seats = jsonObject.getAsJsonArray("seats")
        val freeSeatsMap = mutableMapOf<String, MutableList<String>>()

        for (i in 0 until seats.size()) {
            val seat = seats.get(i).asJsonObject
            val carriageNr = seat.get("carriage_nr").asString
            val seatNr = seat.get("seat_nr").asString
            val state = seat.get("state").asString

            if (state == "FREE") {
                freeSeatsMap.putIfAbsent(carriageNr, mutableListOf())
                freeSeatsMap[carriageNr]?.add(seatNr)
            }
        }

        // Sort carriage numbers numerically in descending order
        return freeSeatsMap.entries
            .sortedByDescending { it.key.toInt() }  // Convert carriage numbers to integers for sorting
            .map { (carriageNr, seatList) ->
                mapOf("number" to carriageNr, "seats" to seatList)
            }
            .toMutableList()
    }

    suspend fun getPaths(
        trainId: String,
        startDate: LocalDate,
        startStationMain: String,
        endStationMain: String
    ): MutableList<Path> {
        val json = trainApiService.getTrainStations(trainId)
        val paths = mutableListOf<Path>()

        if (json !== null) {
            val stops = json.getAsJsonArray("stops")

            val startIndex = stops.indexOfFirst { it.asJsonObject.get("station_name").asString == startStationMain }
            val endIndex = stops.indexOfFirst { it.asJsonObject.get("station_name").asString == endStationMain }
            Log.d("PATHS", "$startIndex, $endIndex")
            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                for (i in startIndex until endIndex) {
                    val startStation = stops[i].asJsonObject
                    val endStation = stops[i + 1].asJsonObject

                    val startStationName = startStation.get("station_name").asString
                    val startStationSlug = startStation.get("station_slug").asString
                    val endStationName = endStation.get("station_name").asString
                    val endStationSlug = endStation.get("station_slug").asString

                    val departureTime = LocalTime.of(
                        startStation.getAsJsonObject("arrival").get("hour").asInt,
                        startStation.getAsJsonObject("arrival").get("minute").asInt,
                        startStation.getAsJsonObject("arrival").get("second").asInt
                    )

                    val arrivalTime = LocalTime.of(
                        endStation.getAsJsonObject("arrival").get("hour").asInt,
                        endStation.getAsJsonObject("arrival").get("minute").asInt,
                        endStation.getAsJsonObject("arrival").get("second").asInt
                    )

                    val departureDateTime = LocalDateTime.of(startDate, departureTime)
                    var arrivalDateTime = LocalDateTime.of(startDate, arrivalTime)

                    if (arrivalDateTime.isBefore(departureDateTime)) {
                        arrivalDateTime = arrivalDateTime.plusDays(1)
                    }

                    val path = Path(
                        startStationName,
                        startStationSlug,
                        endStationName,
                        endStationSlug,
                        departureDateTime,
                        arrivalDateTime
                    )

                    paths.add(path)
                }
            }
        }
        return paths
    }


    suspend fun findTrain(startStation: String, endStation: String, trainNumber: String, arrivalDate: String?, timeStopLimit: Pair<Int, Int>): TrainDescription {
        var connectionId: String? = null
        var trainId: String? = null
        var endDate: String? = null
        var startDate: String? = null
        for(i in timeStopLimit.first until timeStopLimit.second){
            if (connectionId !== null){
                break
            }

            val date = if (arrivalDate !== null) {
                arrivalDate
            } else {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.HOUR_OF_DAY, -i)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                dateFormat.format(calendar.time)
            }
            val json = trainApiService.getConnections(startStation, endStation, date)

            Log.d("WYJEBALO", date)
            if (json !== null) {
                val trains = json.getAsJsonArray("trains")
                val connections = json.getAsJsonArray("connections")
                if (trains != null && connections != null) {

                    for (i in 0 until trains.size()) {
                        val train = trains[i].asJsonObject
                        val trainNumberToCheck = train.get("train_nr").asString

                        if (trainNumberToCheck == trainNumber) {
                            connectionId = train.get("connection_id").asString
                            trainId = train.get("id").asString
                            endDate = train.get("arrival_date").asString
                            startDate = train.get("departure_date").asString
                            Log.d("WYJEBALO", train.toString())
                            break
                        }
                    }

                    for (i in 0 until connections.size()) {
                        val connection = connections[i].asJsonObject
                        val connectionIdToCheck = connection.get("id").asString
                        if (connectionId == connectionIdToCheck) {
                            break
                        }
                    }
                }
            }
        }
        try{
            TrainDescription(
                connectionId,
                trainId,
                LocalDate.parse(startDate, DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd")),
                LocalDate.parse(endDate, DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd"))
            )
        } catch (e: Exception) {
            Log.d("WYJEBALO", "$startStation, $endStation, $arrivalDate, ${startDate.toString()}, ${endDate.toString()}")
        }
        return TrainDescription(
            connectionId,
            trainId,
            LocalDate.parse(startDate, DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd")),
            LocalDate.parse(endDate, DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd"))
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

//    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        return navController.navigateUp(appBarConfiguration)
//                || super.onSupportNavigateUp()
//    }
}