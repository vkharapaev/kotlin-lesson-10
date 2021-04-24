package com.headmostlab.showonmap.ui.main

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.headmostlab.showonmap.R

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
        private const val PERMISSION_REQUEST_CODE = 1000
        private const val LOCATION_REFRESH_TIME = 15000L
        private const val LOCATION_MIN_DISTANCE = 100f
    }

    private lateinit var viewModel: MainViewModel

    private var map: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.getLocationButton).setOnClickListener {
            getLocationIfPermitted()
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { map ->
            this.map = map
            activateCurrentLocation()
        }

        view.findViewById<Button>(R.id.searchButton).setOnClickListener {
            val address = view.findViewById<EditText>(R.id.addressField).text.toString()
            val foundPlaces = Geocoder(context).getFromLocationName(address, 10)
            setCurrentLocation(foundPlaces[0].latitude, foundPlaces[0].longitude)
        }
    }

    private fun activateCurrentLocation() {
        map?.let {
            it.isMyLocationEnabled = checkPermission()
            it.uiSettings.isMyLocationButtonEnabled = checkPermission()
        }
    }

    private fun getLocationIfPermitted() {
        context?.let {
            when {
                checkPermission() -> getLocation()
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> showRationalDialog()
                else -> requestPermissions()
            }
        }
    }

    private fun showRationalDialog() {
        showDialog(
            "Request permission",
            "Please, provide location permission. It is necessary for the app."
        ) { requestPermissions() }
    }

    private fun getLocation() {
        val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.getProvider(LocationManager.GPS_PROVIDER)?.let {
                if (checkPermission()) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        LOCATION_REFRESH_TIME,
                        LOCATION_MIN_DISTANCE
                    ) { location ->
                        context?.let {
//                            getAddress(it, location)
                            setCurrentLocation(location.latitude, location.longitude)
                        }
                    }
                }
            }
        } else {
            showDialog("Get Location", "Please, enable GPS to get location")
        }
    }

    private fun setCurrentLocation(lat: Double, lon: Double) {
        map?.let {
            it.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        lat,
                        lon
                    ), 15.0f
                )
            )
        }
    }

    private fun getAddress(context: Context, location: Location) {
        val addressList =
            Geocoder(context).getFromLocation(location.latitude, location.longitude, 10)
        showDialog("Address", addressList.joinToString())
    }

    private fun checkPermission(): Boolean {
        val context = context ?: return false
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        getLocation()
                    } else {
                        showRationalDialog()
                    }
                }
            }
        }
    }

    private fun showDialog(title: String, message: String, action: (() -> Unit)? = null) {
        val builder = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }
        action?.let { builder.setPositiveButton("OK") { _, _ -> action() } }
        builder.create().show()
    }

}