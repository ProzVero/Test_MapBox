package com.pv.testmapbox

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.WeakReference

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object{
        lateinit var permissionsManager: PermissionsManager
        lateinit var mapboxMap: MapboxMap
        lateinit var map_view : MapView

        private val markers = ArrayList<Marker>() // Marker

        private lateinit var currentRoute: DirectionsRoute // Navigation
        private var navigationMapRoute: NavigationMapRoute? = null // Navigation
        lateinit var button_start_navigation : Button // Navigation


        private var locationEngine: LocationEngine? = null
        private val callback: MainActivityLocationCallback = MainActivityLocationCallback(
            MainActivity()
        )

        lateinit var mContext: Context

        lateinit var location: Location
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_main)
        map_view = findViewById(R.id.map_view2)
        button_start_navigation = findViewById(R.id.button_start_navigation)
        mContext = this
        initMapView(savedInstanceState)
        initPermissions()

        button_start_navigation.setOnClickListener {
            val navigationLauncherOptions = NavigationLauncherOptions.builder()
                .directionsRoute(currentRoute)
                .shouldSimulateRoute(false) // simulasi navigasi perjalanan
                .build()
            NavigationLauncher.startNavigation(this, navigationLauncherOptions)
        }
    }

    override fun onStart() {
        super.onStart()
        map_view.onStart()
    }

    override fun onResume() {
        super.onResume()
        map_view.onResume()
    }

    override fun onPause() {
        super.onPause()
        map_view.onPause()
    }

    override fun onStop() {
        super.onStop()
        map_view.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map_view.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (locationEngine != null){
            locationEngine?.removeLocationUpdates(callback)
        }
        map_view.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        map_view.onSaveInstanceState(outState)
    }

    private fun initMapView(savedInstanceState: Bundle?) {
        map_view.onCreate(savedInstanceState)
    }

    override fun onMapReady(mapboxMap2: MapboxMap) {
        mapboxMap = mapboxMap2
        mapboxMap.setStyle(Style.MAPBOX_STREETS){
            showingDeviceLocation(mapboxMap) //posisi
        }

        //marker
        mapboxMap.addOnMapClickListener {
            //Navigation
            if (markers.size == 1) {
                mapboxMap.removeMarker(markers[0])
                markers.removeAt(0)
            }


            markers.add(
                    mapboxMap.addMarker(
                            MarkerOptions().position(it)
                    )
            )

            if (markers.size == 1) {
                val originPoint = Point.fromLngLat(location.longitude, location.latitude)
                val destinationPoint = Point.fromLngLat(markers[0].position.longitude, markers[0].position.latitude)
                NavigationRoute.builder(this)
                    .accessToken(Mapbox.getAccessToken()!!)
                    .origin(originPoint)
                    .destination(destinationPoint)
                    .alternatives(true)
                    .voiceUnits(DirectionsCriteria.IMPERIAL)
                    .build()
                    .getRoute(object : Callback<DirectionsResponse> {
                        override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                            Toast.makeText(this@MainActivity, "Error occured: ${t.message}", Toast.LENGTH_LONG)
                                .show()
                        }

                        override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                            if (response.body() == null) {
                                Toast.makeText(this@MainActivity, "No routes found, make sure you set the right user and access token.", Toast.LENGTH_LONG)
                                    .show()
                                button_start_navigation.visibility = View.GONE
                                return
                            } else if (response.body()!!.routes().size < 1) {
                                Toast.makeText(this@MainActivity, "No routes found", Toast.LENGTH_LONG)
                                    .show()
                                button_start_navigation.visibility = View.GONE
                                return
                            }
                            currentRoute = response.body()!!.routes()[0]

                            Log.e("Distance", currentRoute.distance().toString())
                            if (navigationMapRoute != null) {
                                navigationMapRoute?.removeRoute()
                            } else {
                                navigationMapRoute = NavigationMapRoute(null, map_view, mapboxMap, R.style.NavigationMapRoute)
                            }
                            navigationMapRoute?.addRoute(currentRoute)
                            button_start_navigation.visibility = View.VISIBLE
                        }
                    })
            } else {
                button_start_navigation.visibility = View.GONE
            }
            true
        }


        //delete marker
        mapboxMap.setOnMarkerClickListener {
            for (marker in markers) {
                if (marker.position == it.position) {
                    markers.remove(marker)
                    mapboxMap.removeMarker(marker)
                }
            }
            true
        }

    }

    private fun initPermissions() {
        val permissionListener = object : PermissionsListener {
            override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
                /* Nothing to do in here */
            }

            override fun onPermissionResult(granted: Boolean) {
                if (granted) {
                    syncMapbox()
                } else {
                    val alertDialogInfo = AlertDialog.Builder(this@MainActivity)
                        .setTitle("test")
                        .setCancelable(false)
                        .setMessage("permission denied")
                        .setPositiveButton("dissmiss") { _, _ ->
                            finish()
                        }
                        .create()
                    alertDialogInfo.show()
                }
            }
        }
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            syncMapbox()
        } else {
            permissionsManager = PermissionsManager(permissionListener)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun syncMapbox() {
        map_view.getMapAsync(this)
    }

    //Menubah Gaya Peta
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item?.itemId) {
            R.id.menu_item_change_style -> {
                val items = arrayOf("Mapbox Street", "Outdoor", "Light", "Dark", "Satellite", "Satellite Street", "Traffic Day", "Traffic Night")
                val alertDialogChangeStyleMaps = AlertDialog.Builder(this)
                        .setItems(items) { dialog, item ->
                            when (item) {
                                0 -> {
                                    mapboxMap.setStyle(Style.MAPBOX_STREETS)
                                    dialog.dismiss()
                                }
                                1 -> {
                                    mapboxMap.setStyle(Style.OUTDOORS)
                                    dialog.dismiss()
                                }
                                2 -> {
                                    mapboxMap.setStyle(Style.LIGHT)
                                    dialog.dismiss()
                                }
                                3 -> {
                                    mapboxMap.setStyle(Style.DARK)
                                    dialog.dismiss()
                                }
                                4 -> {
                                    mapboxMap.setStyle(Style.SATELLITE)
                                    dialog.dismiss()
                                }
                                5 -> {
                                    mapboxMap.setStyle(Style.SATELLITE_STREETS)
                                    dialog.dismiss()
                                }
                                6 -> {
                                    mapboxMap.setStyle(Style.TRAFFIC_DAY)
                                    dialog.dismiss()
                                }
                                7 -> {
                                    mapboxMap.setStyle(Style.TRAFFIC_NIGHT)
                                    dialog.dismiss()
                                }
                            }
                        }
                        .setTitle("change style map")
                        .create()
                alertDialogChangeStyleMaps.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //posisi
    @SuppressLint("MissingPermission")
    private fun showingDeviceLocation(mapboxMap: MapboxMap) {
        val locationComponent = mapboxMap.locationComponent
        locationComponent.activateLocationComponent(this, mapboxMap.style!!)
        locationComponent.isLocationComponentEnabled = true
        locationComponent.cameraMode = CameraMode.TRACKING
        locationComponent.renderMode = RenderMode.COMPASS

        initLocationEngine()
    }

    private fun initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this)

        val request = LocationEngineRequest.Builder(1000L)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(1000L*5)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        locationEngine!!.requestLocationUpdates(request, callback, mainLooper)
        locationEngine!!.getLastLocation(callback)

    }

    private class MainActivityLocationCallback internal constructor(activity: MainActivity?) :
        LocationEngineCallback<LocationEngineResult?> {
        private val activityWeakReference: WeakReference<MainActivity?>?

        /**
         * The LocationEngineCallback interface's method which fires when the device's location has changed.
         *
         * @param result the LocationEngineResult object which has the last known location within it.
         */
        override fun onSuccess(result: LocationEngineResult?) {
            val activity = mContext
            if (activity != null) {
                location = result?.getLastLocation() ?: return

                // Create a Toast which displays the new location's coordinates
                Toast.makeText(
                    activity, java.lang.String.format(
                        "New Location",
                        java.lang.String.valueOf(result.getLastLocation()!!.getLatitude()),
                        java.lang.String.valueOf(result?.getLastLocation()!!.getLongitude())
                    ),
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("New Location","Lat : ${result.lastLocation?.latitude} & Lang : ${result.lastLocation?.longitude} ")

                // Pass the new location to the Maps SDK's LocationComponent
                if (mapboxMap != null && result.getLastLocation() != null) {
                    mapboxMap.getLocationComponent()
                        .forceLocationUpdate(result.getLastLocation())
                }
            }
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location can't be captured
         *
         * @param exception the exception message
         */




        override fun onFailure(exception: Exception) {
            val activity: MainActivity? = activityWeakReference!!.get()
            if (activity != null) {
                Toast.makeText(
                    activity, exception.localizedMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }

            Log.e("New Location",exception.localizedMessage.toString())
        }

        init {
            activityWeakReference = WeakReference(activity)
        }
    }

}