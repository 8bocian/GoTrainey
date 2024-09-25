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
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.launch
import org.json.JSONObject
import pl.gotrainey.gotrainey.adapters.CartsAdapter
import pl.gotrainey.gotrainey.adapters.StationAdapter
import pl.gotrainey.gotrainey.databinding.ActivityMainBinding
import pl.gotrainey.gotrainey.interfaces.JsonResponseCallback
import pl.gotrainey.gotrainey.services.TrainApiService
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // START STATION ADAPTER

        startStationAdapter = StationAdapter(startStationList) { selectedItem ->
            binding.startStation.setText(selectedItem)
            binding.startStationRecyclerView.visibility = View.GONE
        }

        binding.startStationRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.startStationRecyclerView.adapter = startStationAdapter

        // END STATION ADAPTER

        endStationAdapter = StationAdapter(endStationList) { selectedItem ->
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
                if (s.isNullOrEmpty()) {
                    binding.startStationRecyclerView.visibility = View.GONE
                } else {
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
                if (s.isNullOrEmpty()) {
                    binding.endStationRecyclerView.visibility = View.GONE
                } else {
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
            var startStation: String = binding.startStation.text.toString()
            var endStation: String = binding.endStation.text.toString()
            var trainNumber: String = binding.trainNumber.text.toString()
            lifecycleScope.launch {
                val connectionId = getTrain(startStation, endStation, trainNumber)
//                val connectionId = 10
                Log.d("CONNECTION_ID", connectionId.toString())
                if (connectionId !== null) {
                    val cartsJson = trainApiService.getTrainPlaces(connectionId, trainNumber)
                    val cartsString = """
                        {
                          "seats": [
                            {"carriage_nr": "1", "seat_nr": "12", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "15", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "1", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "5", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "3", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "23", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "13", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "67", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "59", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "15", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "1", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "5", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "3", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "23", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "13", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "67", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "59", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "15", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "1", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "5", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "3", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "23", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "13", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "67", "state": "FREE"},
                            {"carriage_nr": "1", "seat_nr": "59", "state": "FREE"},
                            {"carriage_nr": "2", "seat_nr": "23", "state": "FREE"},
                            {"carriage_nr": "2", "seat_nr": "13", "state": "FREE"},
                            {"carriage_nr": "2", "seat_nr": "67", "state": "FREE"},
                            {"carriage_nr": "2", "seat_nr": "59", "state": "FREE"}
                          ]
                        }

                    """.trimIndent()
//                    val cartsJson = JsonParser.parseString(cartsString).asJsonObject
                    if (cartsJson !== null) {
                        val carts = parseFreeSeats(cartsJson)
                        updateCarts(cartsList = cartsList, adapter = cartsAdapter, recyclerView = binding.cartsRecyclerView, carts = carts)
                    }
                }
            }
        }

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
        recyclerView.visibility = if (suggestionList.isEmpty()) View.GONE else View.VISIBLE
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


    suspend fun getTrain(startStation: String, endStation: String, trainNumber: String): String? {
        var connectionId: String? = null

        for(i in 0 until 4){
            val date = if (VERSION.SDK_INT >= VERSION_CODES.O) {
                val dateTime = LocalDateTime.now().minusHours(i.toLong())
                dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            } else {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.HOUR_OF_DAY, -i) // Subtract 1 hour
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                dateFormat.format(calendar.time)
            }
            if (connectionId !== null){
                break
            }
            val json = trainApiService.getConnections(startStation, endStation, date)

            if (json !== null) {
                val trains = json.getAsJsonArray("trains")
                val connections = json.getAsJsonArray("connections")
                if (trains != null && connections != null) {

                    for (i in 0 until trains.size()) {
                        val train = trains[i].asJsonObject
                        val trainNumberToCheck = train.get("train_nr").asString

                        if (trainNumberToCheck == trainNumber) {
                            connectionId = train.get("connection_id").asString
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
        return connectionId
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