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
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.google.gson.JsonObject
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.startStation.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                Log.d("MainActivity", "Stacja Startowa changed: ${s.toString()}")
                trainApiService.findStation(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        binding.endStation.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                Log.d("MainActivity", "Stacja Końcowa changed: ${s.toString()}")
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

        binding.search.setOnClickListener {
            var startStation: String = binding.startStation.text.toString()
            var endStation: String = binding.endStation.text.toString()
            var trainNumber: String = binding.trainNumber.text.toString()

            getTrain(startStation, endStation, trainNumber)
        }

    }

    fun getTrain(startStation: String, endStation: String, trainNumber: String) {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).toString()
        val json = trainApiService.getTrains(startStation, endStation, date, object :
            JsonResponseCallback {
            override fun onSuccess(json: JsonObject) {
                Log.d("TrainApiService", "Received trains: $json")

            }

            override fun onError(error: String) {
                Log.e("TrainApiService", "Error: $error")
            }
        })

        val trains = json.getAsJsonArray("trains")
        val connections = json.getAsJsonArray("connections")
        if (trains != null && connections != null) {
            var connectionId: String? = null

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
                if (connectionId == connectionIdToCheck){
                    return connectionId
                }
            }
        }
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

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}