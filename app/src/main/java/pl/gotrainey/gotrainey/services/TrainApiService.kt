package pl.gotrainey.gotrainey.services

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URLEncoder
import java.util.zip.GZIPInputStream

class TrainApiService {

    private val client = OkHttpClient()
    private val gson = Gson()

    private fun encodeParams(params: Map<String, String>): String {
        return params.map { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }.joinToString("&")
    }

    suspend fun getConnections(startStation: String, endStation: String, date: String): JsonObject? {
        val params = mapOf(
            "query[date]" to date,
            "query[start_station]" to startStation,
            "query[end_station]" to endStation,
            "query[brand_id]" to "28",
            "query[only_direct]" to "true",
            "query[only_purchasable]" to "false"
        )

        val url = "https://koleo.pl/pl/connections?" + encodeParams(params)

        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
            .addHeader("Accept-Encoding", "gzip, deflate, br")
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .addHeader("Connection", "keep-alive")
            .build()

        return performRequest(request)
    }

    suspend fun getTrainComposition(connectionId: String, trainId: String): JsonObject? {
        val url = "https://koleo.pl/train_composition/$connectionId/$trainId/5"

        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .build()

        return performRequest(request)
    }

    suspend fun getTrainStations(trainId: String): JsonObject? {
        val params = mapOf(
            "travel_train_id" to trainId
        )

        val url = "https://koleo.pl/travel_train?" + encodeParams(params)

        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .build()

        return performRequest(request)
    }

    suspend fun getTrainPlaces(connectionId: String, trainId: String): JsonObject? {
        val url = "https://koleo.pl/seats_availability/$connectionId/$trainId/5"

        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .build()

        return performRequest(request)
    }

    suspend fun findStation(stationName: String): JsonObject? {
        if (stationName.length < 3) {
            Log.d("TrainApiService", "Station name length must be at least 3 chars, received $stationName of length ${stationName.length}")
//            throw IllegalArgumentException("Station name must be at least 3 characters long.")
            return null
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

        return performRequest(request)
    }

    private suspend fun performRequest(request: Request): JsonObject? {
        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                handleResponse(response)
            } catch (e: IOException) {
                Log.e("TrainApiService", "Request failed: ${e.message}")
                null
            }
        }
    }

    private fun handleResponse(response: Response): JsonObject? {
        if (!response.isSuccessful) {
            Log.e("TrainApiService", "Request failed with code: ${response.code}")
            return null
        }

        val body = response.body
        val contentEncoding = response.header("Content-Encoding")
        val responseData = if (contentEncoding != null && contentEncoding.contains("gzip")) {
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
            body?.string()
        }

        Log.d("TrainApiService", "Response: $responseData")
        return responseData?.let { gson.fromJson(it, JsonObject::class.java) }
    }
}
