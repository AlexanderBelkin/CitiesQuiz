package com.alex.testgame

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CitiesIterator(context: Context, private val callback: Callback) {

    private var cityList = mutableListOf<CapitalCity>()

    private var currentPosition = 0

    private var placesCount = 0

    private var currentScore = TOTAL_KM

    companion object {
        const val TOTAL_KM = 1500
        const val MAX_DISTANCE_KM = 50
    }

    init {
        cityList = context.resources.openRawResource(R.raw.cities)
                .bufferedReader()
                .use { Gson().fromJson(it.readText(), object: TypeToken<MutableList<CapitalCity>>() {}.type)}
        callback.onNext(cityList[currentPosition], placesCount, currentScore)
        Log.d(MapsActivity.TAG, "list: $cityList")
    }

    fun checkDistance(latLng: LatLng, result: Result) {
        val currentCity = cityList[currentPosition]
        val distanceBetween = getDistanceKm(latLng, currentCity)
        currentScore -= distanceBetween.toInt()
        if (distanceBetween < MAX_DISTANCE_KM) {
            placesCount++
            result.onResult(true, cityList[currentPosition], distanceBetween)
        } else {
            result.onResult(false, cityList[currentPosition], distanceBetween)
        }
    }

    fun goNext() {
        currentPosition++
        when {
            currentScore <= 0 -> callback.onGameOver(currentScore)
            currentPosition < cityList.size -> callback.onNext(cityList[currentPosition], placesCount, currentScore)
            else -> callback.onComplete(currentScore)
        }
    }

    private fun getDistanceKm(latLng: LatLng, currentCity: CapitalCity): Float {
        val results = FloatArray(1)
        Location.distanceBetween(latLng.latitude, latLng.longitude, currentCity.lat, currentCity.lng, results)
        return results[0] / 1000
    }

    interface Callback {
        fun onNext(capitalCity: CapitalCity, placesCount: Int, kmLeft: Int)
        fun onComplete(score: Int)
        fun onGameOver(score: Int)
    }

    interface Result {
        fun onResult(isSuccess: Boolean, capitalCity: CapitalCity, distanceBetween: Float)
    }

    fun reset() {
        currentPosition = 0
        placesCount = 0
        currentScore = TOTAL_KM
        callback.onNext(cityList[currentPosition], placesCount, currentScore)
    }
}
