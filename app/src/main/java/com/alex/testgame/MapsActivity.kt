package com.alex.testgame

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_maps.*
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.CameraUpdate





class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap

    private lateinit var citiesIterator: CitiesIterator

    private var myMarker: Marker? = null

    private var cityMarker: Marker? = null

    private var canSetPin = false

    companion object {
        const val TAG = "MapsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)

        citiesIterator = CitiesIterator(this, object : CitiesIterator.Callback {
            override fun onNext(capitalCity: CapitalCity, placesCount: Int, kmLeft: Int) {
                canSetPin = true
                citiesLeftTV.text = getString(R.string.cities_placed, placesCount.toString())
                kmLeftTv.text = getString(R.string.km_left, kmLeft.toString())
                locationTV.text = getString(R.string.select_location, capitalCity.capitalCity)

                resetViews()
            }
            override fun onComplete(score: Int) {
                showAlertDialog(getString(R.string.game_complete_title), getString(R.string.game_complete_message, score.toString()),
                        DialogInterface.OnClickListener { dialog, id ->
                            citiesIterator.reset()
                        })
            }

            override fun onGameOver(score: Int) {
                showAlertDialog(getString(R.string.game_over_title), getString(R.string.game_complete_message, score.toString()),
                        DialogInterface.OnClickListener { dialog, id ->
                            citiesIterator.reset()
                        })
            }
        })

        placeBtn.setOnClickListener({
            if (myMarker == null) {
                Toast.makeText(this, getString(R.string.place_pin), Toast.LENGTH_SHORT).show()
            } else {
                citiesIterator.checkDistance(myMarker!!.position, object : CitiesIterator.Result {
                    override fun onResult(isSuccess: Boolean, capitalCity: CapitalCity, distanceBetween: Float) {
                        canSetPin = false
                        if (isSuccess) {
                            resultTextView.setTextColor(ContextCompat.getColor(this@MapsActivity, R.color.green))
                        } else {
                            resultTextView.setTextColor(ContextCompat.getColor(this@MapsActivity, R.color.red))
                        }
                        nextBtn.visibility = View.VISIBLE
                        placeBtn.visibility = View.INVISIBLE
                        resultTextView.text = getString(R.string.distance, distanceBetween.toInt().toString())
                        addCityMarker(capitalCity)
                        animateToBounds()
                    }
                })
            }
        })

        nextBtn.setOnClickListener {
            citiesIterator.goNext()
        }
    }

    private fun animateToBounds() {
        if (myMarker == null && cityMarker == null) {
            return
        }
        val builder = LatLngBounds.Builder()
        builder.include(myMarker!!.position)
        builder.include(cityMarker!!.position)
        val bounds = builder.build()
        val cu = CameraUpdateFactory.newLatLngBounds(bounds, resources.getDimension(R.dimen.map_padding).toInt())
        googleMap.animateCamera(cu)
    }

    private fun showAlertDialog(title: String, message: String, listener: DialogInterface.OnClickListener) {
        val builder = AlertDialog.Builder(this@MapsActivity)
        builder.setTitle(title)
                .setMessage(message)
                .setIcon(R.drawable.ic_warning_black_24dp)
                .setCancelable(false)
                .setPositiveButton("ОК", { dialog, id ->
                    dialog.cancel()
                    listener.onClick(dialog, id)
                })
        val alert = builder.create()
        alert.show()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap

        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(47.456105, 14.261623), 4f))

        googleMap.setOnMarkerClickListener {
            true
        }
        googleMap.setOnMapClickListener {
            if(canSetPin) {
                addTapMarker(it)
            }
        }
    }

    private fun addCityMarker(capitalCity: CapitalCity) {
        cityMarker?.remove()
        val options = MarkerOptions()
        options.position(LatLng(capitalCity.lat, capitalCity.lng))
        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        cityMarker = googleMap.addMarker(options)
    }

    private fun addTapMarker(latLng: LatLng) {
        myMarker?.remove()
        val options = MarkerOptions()
        options.position(latLng)
        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        myMarker = googleMap.addMarker(options)
    }

    private fun resetViews() {
        nextBtn.visibility = View.INVISIBLE
        placeBtn.visibility = View.VISIBLE
        resultTextView.text = ""
        myMarker?.remove()
        cityMarker?.remove()
        myMarker = null
        cityMarker = null
    }
}
