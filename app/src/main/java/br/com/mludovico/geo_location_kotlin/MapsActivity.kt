 package br.com.mludovico.geo_location_kotlin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import java.util.*

 class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1000
        private const val REQUEST_CHECK_SETTING = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(location: LocationResult) {
                super.onLocationResult(location)

                lastLocation = location.lastLocation
                placeMarkerOnMap(LatLng(lastLocation.latitude, lastLocation.longitude))
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lastLocation.latitude, lastLocation.longitude), 15f))
            }
        }

        createLocationRequest()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.uiSettings.isZoomControlsEnabled = true
        map.setOnMarkerClickListener(this)
        setupMap()
    }

    private fun setupMap() {
        if (
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
            !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        map.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                placeMarkerOnMap(currentLatLng)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            }
        }
    }

    private fun placeMarkerOnMap(location: LatLng) {
        val markerOptions = MarkerOptions()
            .position(location)
            .title(getAddress(location))
            .icon(bitmapDescriptionFromVector(this, R.drawable.bike))
        map.addMarker(markerOptions)
    }

    private fun bitmapDescriptionFromVector(context: Context, vectorResourceId: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(context, vectorResourceId)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

     private fun getAddress(latLng: LatLng): String {
         val geocoder: Geocoder = Geocoder(this, Locale.getDefault())
         val locations: List<Address> = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

         val location = locations.first()
         val address = location.getAddressLine(0)
         val city = location.locality
         val state = location.adminArea
         val country = location.countryName
         val postalCode = location.postalCode
         return address
     }

     private fun startLocationUpdates() {
         if (
             ActivityCompat.checkSelfPermission(
                 this,
                 android.Manifest.permission.ACCESS_FINE_LOCATION
             )
             !=
             PackageManager.PERMISSION_GRANTED
         ) {
             ActivityCompat.requestPermissions(
                 this,
                 arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                 LOCATION_PERMISSION_REQUEST
             )
             return
         }

         fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
     }

     private fun createLocationRequest() {
         // Setup location request
         locationRequest = LocationRequest()
         locationRequest.interval = 10000
         locationRequest.fastestInterval = 5000
         locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

         // Build task
         val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

         val client = LocationServices.getSettingsClient(this)
         val task = client.checkLocationSettings(builder.build())

         // Add listeners
         task.addOnSuccessListener {
             locationUpdateState = true
             startLocationUpdates()
         }
         task.addOnFailureListener { error ->
             if (error is ResolvableApiException) {
                 // Location settings are not satisfied, but this can be fixed by showing a dialog
                 // to the user
                 try {
                     // Show the dialog by calling startResolutionForResult(), and check the
                     // in onActivityRsult().
                     error.startResolutionForResult(this@MapsActivity, REQUEST_CHECK_SETTING)
                 } catch (sendEx: IntentSender.SendIntentException) {
                     Log.w("update failure", "Send intent error ${sendEx.message}")
                 }
             }
         }
     }

     override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
         super.onActivityResult(requestCode, resultCode, data)
         if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CHECK_SETTING) {

         }
     }

     override fun onPause() {
         super.onPause()
         fusedLocationClient.removeLocationUpdates(locationCallback)
     }

     override fun onResume() {
         super.onResume()
         if (!locationUpdateState) {
             startLocationUpdates()
         }
     }

     override fun onMarkerClick(marker: Marker?): Boolean = false

}