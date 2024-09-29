package pl.gotrainey.gotrainey.schedulers

import android.content.Context
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.work.*
import pl.gotrainey.gotrainey.data_classes.TrainDescription
import pl.gotrainey.gotrainey.services.TrainApiService
import java.util.concurrent.TimeUnit
import java.util.Calendar

class Scheduler(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val a = inputData.getString("startStationName").toString()
        val b = inputData.getString("endStationName").toString()
        val c = inputData.getString("trainNumber").toString()
        Log.d("TEST", "$a, $b, $c")
        return Result.success(inputData)
    }
}