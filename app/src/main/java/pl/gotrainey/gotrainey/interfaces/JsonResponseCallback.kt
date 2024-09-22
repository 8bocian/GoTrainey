package pl.gotrainey.gotrainey.interfaces

import com.google.gson.JsonObject

interface JsonResponseCallback {
    fun onSuccess(json: JsonObject)
    fun onError(error: String)
}