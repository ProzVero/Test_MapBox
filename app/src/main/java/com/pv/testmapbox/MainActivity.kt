package com.pv.testmapbox

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AlertDialog
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object{
        lateinit var permissionsManager: PermissionsManager
        lateinit var mapboxMap: MapboxMap
        lateinit var map_view : MapView
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_main)
        map_view = findViewById(R.id.map_view2)
        initMapView(savedInstanceState)
        initPermissions()
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
        mapboxMap.setStyle(Style.MAPBOX_STREETS)
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
}