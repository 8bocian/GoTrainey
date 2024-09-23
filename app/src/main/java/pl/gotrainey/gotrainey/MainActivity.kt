package pl.gotrainey.gotrainey

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
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import org.json.JSONObject
import pl.gotrainey.gotrainey.adapters.StationAdapter
import pl.gotrainey.gotrainey.databinding.ActivityMainBinding
import pl.gotrainey.gotrainey.interfaces.JsonResponseCallback
import pl.gotrainey.gotrainey.services.TrainApiService
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

class MainActivity : AppCompatActivity() {

    private val trainApiService = TrainApiService()
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var startStationAdapter: StationAdapter
    private lateinit var endStationAdapter: StationAdapter
    private val startStationList = mutableListOf<Map<String, Any>>()  // List to hold suggestions
    private val endStationList = mutableListOf<Map<String, Any>>()  // List to hold suggestions


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
                            fetchSuggestions(suggestionList = parseJsonToListOfMaps(stations), adapter = startStationAdapter, recyclerView = binding.startStationRecyclerView, stations = startStationList)
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
                            fetchSuggestions(suggestionList = parseJsonToListOfMaps(stations), adapter = endStationAdapter, recyclerView = binding.endStationRecyclerView, stations = endStationList)
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
                Log.d("CONNECTION_ID", connectionId.toString())
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

    suspend fun getTrain(startStation: String, endStation: String, trainNumber: String): String? {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).toString()
        var connectionId: String? = null

        for(i in 0 until 24){
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