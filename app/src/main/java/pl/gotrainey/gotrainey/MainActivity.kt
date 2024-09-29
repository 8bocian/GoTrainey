package pl.gotrainey.gotrainey

import android.os.Build
import android.os.Build.*
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.launch
import org.json.JSONObject
import pl.gotrainey.gotrainey.adapters.CartsAdapter
import pl.gotrainey.gotrainey.adapters.StationAdapter
import pl.gotrainey.gotrainey.data_classes.Path
import pl.gotrainey.gotrainey.data_classes.TrainDescription
import pl.gotrainey.gotrainey.data_classes.Trip
import pl.gotrainey.gotrainey.databinding.ActivityMainBinding
import pl.gotrainey.gotrainey.interfaces.JsonResponseCallback
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
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var startStationAdapter: StationAdapter
    private lateinit var endStationAdapter: StationAdapter

    private val startStationList = mutableListOf<Map<String, Any>>()  // List to hold suggestions
    private val endStationList = mutableListOf<Map<String, Any>>()  // List to hold suggestions

    private lateinit var cartsAdapter: CartsAdapter
    private val cartsList = mutableListOf<Map<String, Any>>()  // List to hold suggestions

    private val trip = Trip()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // START STATION ADAPTER

        startStationAdapter = StationAdapter(startStationList, binding.startStationRecyclerView) { selectedItem ->
            binding.startStation.setText(selectedItem)
            binding.startStationRecyclerView.visibility = View.GONE
        }

        binding.startStationRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.startStationRecyclerView.adapter = startStationAdapter

        // END STATION ADAPTER

        endStationAdapter = StationAdapter(endStationList, binding.endStationRecyclerView) { selectedItem ->
            binding.endStation.setText(selectedItem)
            binding.endStationRecyclerView.visibility = View.GONE
        }

        binding.endStationRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.endStationRecyclerView.adapter = endStationAdapter

        // CARTS ADAPTER

        cartsAdapter = CartsAdapter(cartsList)

        binding.cartsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.cartsRecyclerView.adapter = cartsAdapter

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.cartsRecyclerView)


        //TODO: AFTER CHANGE OF TEXT DISPLAY A LIST OF POSSIBLE STATIONS AND LET USER PICK ONE

        binding.startStation.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                Log.d("BLANK 2", (s.isNullOrEmpty() || startStationList.isEmpty()).toString())
                if (s.isNullOrEmpty() || startStationList.isEmpty()) {
                    binding.startStationRecyclerView.visibility = View.GONE
                }
                if (!s.isNullOrEmpty()){
                    binding.startStationRecyclerView.visibility = View.VISIBLE

                    lifecycleScope.launch {
                        val stations = trainApiService.findStation(s.toString())
                        Log.d("STATIONS", stations.toString())
                        if (stations !== null){
                            fetchSuggestions(suggestionList = startStationList, adapter = startStationAdapter, recyclerView = binding.startStationRecyclerView, stations = parseJsonToListOfMaps(stations))
                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        binding.endStation.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty() || endStationList.isEmpty()) {
                    binding.endStationRecyclerView.visibility = View.GONE
                }
                if (!s.isNullOrEmpty()){
                    binding.endStationRecyclerView.visibility = View.VISIBLE
                    lifecycleScope.launch {
                        val stations = trainApiService.findStation(s.toString())
                        Log.d("STATIONS", stations.toString())
                        if (stations !== null){
                            fetchSuggestions(suggestionList = endStationList, adapter = endStationAdapter, recyclerView = binding.endStationRecyclerView, stations = parseJsonToListOfMaps(stations))
                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

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
            val startStation: String = binding.startStation.text.toString()
            val endStation: String = binding.endStation.text.toString()
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
                                            binding.startStation.setText(workInfo.outputData.getString("startStationName").toString())
                                            binding.endStation.setText(workInfo.outputData.getString("endStationName").toString())
                                            val cartsJson = trainApiService.getTrainPlaces(
                                                subTrainDescription.connectionId,
                                                workInfo.outputData.getString("trainNumber").toString()
                                            )
                                            if (cartsJson !== null) {
                                                val carts = parseFreeSeats(cartsJson)
                                                updateCarts(cartsList = cartsList, adapter = cartsAdapter, recyclerView = binding.cartsRecyclerView, carts = carts)
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

    fun parseJsonToListOfMaps(json: JsonObject): MutableList<Map<String, Any>> {
        val stationsArray = json.getAsJsonArray("stations")

        val listOfMaps = mutableListOf<Map<String, Any>>()

        for (i in 0 until stationsArray.size()) {
            val station = stationsArray[i].asJsonObject
            val map = mapOf(
                "id" to station.get("id").asInt,
                "name" to station.get("name").asString,
                "name_slug" to station.get("name_slug").asString
            )
            listOfMaps.add(map)
        }

        return listOfMaps
    }

    private fun fetchSuggestions(suggestionList: MutableList<Map<String, Any>>, adapter: StationAdapter, recyclerView: RecyclerView, stations: List<Map<String, Any>>) {
        suggestionList.clear()
        suggestionList.addAll(stations)
        adapter.notifyDataSetChanged()
        Log.d("CHANGE", suggestionList.toString())
    }

    private fun updateCarts(cartsList: MutableList<Map<String, Any>>, adapter: CartsAdapter, recyclerView: RecyclerView, carts: List<Map<String, Any>>) {
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