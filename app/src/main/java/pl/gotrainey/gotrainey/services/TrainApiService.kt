package pl.gotrainey.gotrainey.services

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.*
import pl.gotrainey.gotrainey.interfaces.JsonResponseCallback
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPInputStream

class TrainApiService {

    private val client = OkHttpClient()
    private val gson = Gson()

    fun getTrainComposition(trainId: String, callback: JsonResponseCallback) {
        val url = "https://koleo.pl/train_composition/$trainId"

        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("TrainApiService", "Request failed: ${e.message}")
                callback.onError("Request failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                handleResponse(response, callback)
            }
        })
    }

    fun getTrainPlaces(trainId: String, callback: JsonResponseCallback) {
        val url = "https://koleo.pl/seats_availability/$trainId"

        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("TrainApiService", "Request failed: ${e.message}")
                callback.onError("Request failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                handleResponse(response, callback)
            }
        })
    }



    fun findStation(stationName: String, callback: JsonResponseCallback) {
        if (stationName.length < 3) {
            Log.e("TrainApiService", "Station name length must be at least 3 chars, received $stationName of length ${stationName.length}")
            callback.onError("Station name must be at least 3 characters long.")
            return
        }

        val url = "https://koleo.pl/ls"
        val queryParams = "q=$stationName&language=pl"
        val fullUrl = "$url?$queryParams"

        val request = Request.Builder()
            .url(fullUrl)
            .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
            .addHeader("Accept-Encoding", "gzip, deflate, br")
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .addHeader("Connection", "keep-alive")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("TrainApiService", "Request failed: ${e.message}")
                callback.onError("Request failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                handleResponse(response, callback)
            }
        })
    }

    private fun handleResponse(response: Response, callback: JsonResponseCallback) {
        if (response.isSuccessful) {
            val body = response.body
            val contentEncoding = response.header("Content-Encoding")
            var responseData: String? = null

            responseData = if (contentEncoding != null && contentEncoding.contains("gzip")) {
                // Handle GZIP-compressed response
                body?.byteStream()?.let { inputStream ->
                    val gzipInputStream = GZIPInputStream(inputStream)
                    val reader = BufferedReader(InputStreamReader(gzipInputStream))
                    val stringBuilder = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                    stringBuilder.toString()
                }
            } else {
                // Handle uncompressed response
                body?.string()
            }

            Log.d("TrainApiService", "Response: $responseData")
            callback.onSuccess(gson.fromJson(responseData, JsonObject::class.java))
        } else {
            Log.e("TrainApiService", "Request failed with code: ${response.code}")
            callback.onError("Request failed with code: ${response.code}")
        }
    }
}
