package com.pv.testmapbox

import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.expressions.Expression.*
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils


class CircleToIconTransitionActivity : AppCompatActivity(), OnMapReadyCallback, MapboxMap.OnMapClickListener {
    lateinit var mapView: MapView
    lateinit var mapboxMap: MapboxMap
    companion object {
        private const val BASE_CIRCLE_INITIAL_RADIUS = 3.4f
        private const val RADIUS_WHEN_CIRCLES_MATCH_ICON_RADIUS = 14f
        private const val ZOOM_LEVEL_FOR_START_OF_BASE_CIRCLE_EXPANSION = 11f
        private const val ZOOM_LEVEL_FOR_SWITCH_FROM_CIRCLE_TO_ICON = 12f
        private const val FINAL_OPACITY_OF_SHADING_CIRCLE = .5f
        private const val BASE_CIRCLE_COLOR = "#3BC802"
        private const val SHADING_CIRCLE_COLOR = "#858585"
        private const val SOURCE_ID = "SOURCE_ID"
        private const val ICON_LAYER_ID = "ICON_LAYER_ID"
        private const val BASE_CIRCLE_LAYER_ID = "BASE_CIRCLE_LAYER_ID"
        private const val SHADOW_CIRCLE_LAYER_ID = "SHADOW_CIRCLE_LAYER_ID"
        private const val ICON_IMAGE_ID = "ICON_ID"
        private const val RED_ICON_ID = "RED_ICON_ID"
        private const val YELLOW_ICON_ID = "YELLOW_ICON_ID"
        private const val LAYER_ID = "LAYER_ID"
        private const val ICON_PROPERTY = "ICON_PROPERTY"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_circle_to_icon_transition)
        mapView = findViewById(R.id.map_view1)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap2: MapboxMap) {
        mapboxMap = mapboxMap2
        mapboxMap.setStyle(
            Style.Builder()
                .fromUri(Style.LIGHT) // Add images to the map so that the SymbolLayers can reference the images.
                .withImage(
                    ICON_IMAGE_ID,
                    BitmapUtils.getBitmapFromDrawable(
                        resources.getDrawable(R.drawable.ic_baseline_local_hospital_24)
                    )!!
                ) // Add GeoJSON data to the GeoJsonSource and then add the GeoJsonSource to the map
                .withSource(
                    GeoJsonSource(
                        SOURCE_ID,
                        FeatureCollection.fromFeatures(initFeatureArray())
                    )
                )

                // Adding the actual SymbolLayer to the map style. The match expression will check the
                // ICON_PROPERTY property key and then use the partner value for the actual icon id.
                .withLayer(
                    SymbolLayer(LAYER_ID, SOURCE_ID)
                        .withProperties(iconImage(match(
                            get(ICON_PROPERTY), literal(ICON_IMAGE_ID),
                            stop(YELLOW_ICON_ID, YELLOW_ICON_ID),
                            stop(ICON_IMAGE_ID, ICON_IMAGE_ID))),
                            iconAllowOverlap(true),
                            iconAnchor(Property.ICON_ANCHOR_BOTTOM))
                )

        ) { style -> // Add the base CircleLayer, which will show small circles when the map is zoomed far enough
            // away from the map.
            val baseCircleLayer = CircleLayer(
                BASE_CIRCLE_LAYER_ID,
                SOURCE_ID
            ).withProperties(
                PropertyFactory.circleColor(
                    Color.parseColor(
                        BASE_CIRCLE_COLOR
                    )
                ),
                PropertyFactory.circleRadius(
                    Expression.interpolate(
                        Expression.linear(),
                        Expression.zoom(),
                        Expression.stop(
                            ZOOM_LEVEL_FOR_START_OF_BASE_CIRCLE_EXPANSION,
                            BASE_CIRCLE_INITIAL_RADIUS
                        ),
                        Expression.stop(
                            ZOOM_LEVEL_FOR_SWITCH_FROM_CIRCLE_TO_ICON,
                            RADIUS_WHEN_CIRCLES_MATCH_ICON_RADIUS
                        )
                    )
                )
            )
            style.addLayer(baseCircleLayer)

            // Add a "shading" CircleLayer, whose circles' radii will match the radius of the SymbolLayer
            // circular icon
            val shadowTransitionCircleLayer = CircleLayer(
                SHADOW_CIRCLE_LAYER_ID,
                SOURCE_ID
            )
                .withProperties(
                    PropertyFactory.circleColor(
                        Color.parseColor(
                            SHADING_CIRCLE_COLOR
                        )
                    ),
                    PropertyFactory.circleRadius(
                        RADIUS_WHEN_CIRCLES_MATCH_ICON_RADIUS
                    ),
                    PropertyFactory.circleOpacity(
                        Expression.interpolate(
                            Expression.linear(),
                            Expression.zoom(),
                            Expression.stop(
                                ZOOM_LEVEL_FOR_START_OF_BASE_CIRCLE_EXPANSION - .5,
                                0
                            ),
                            Expression.stop(
                                ZOOM_LEVEL_FOR_START_OF_BASE_CIRCLE_EXPANSION,
                                FINAL_OPACITY_OF_SHADING_CIRCLE
                            )
                        )
                    )
                )
            style.addLayerBelow(
                shadowTransitionCircleLayer,
                BASE_CIRCLE_LAYER_ID
            )

            // Add the SymbolLayer
            val symbolIconLayer = SymbolLayer(
                ICON_LAYER_ID,
                SOURCE_ID
            )
            symbolIconLayer.withProperties(
                PropertyFactory.iconImage(
                    ICON_IMAGE_ID
                ),
                PropertyFactory.iconSize(1.5f),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconAllowOverlap(true)
            )
            symbolIconLayer.minZoom = ZOOM_LEVEL_FOR_SWITCH_FROM_CIRCLE_TO_ICON
            style.addLayer(symbolIconLayer)
            Toast.makeText(
                this@CircleToIconTransitionActivity,
                "zoom_map_in_and_out_circle_to_icon_transition", Toast.LENGTH_SHORT
            ).show()
            mapboxMap.animateCamera(
                CameraUpdateFactory
                    .newCameraPosition(
                        CameraPosition.Builder()
                            .zoom(12.5)
                            .build()
                    ), 3000
            )
            mapboxMap.addOnMapClickListener(this@CircleToIconTransitionActivity)
        }
    }


    private fun initFeatureArray(): List<Feature> {

        val singleFeatureOne = Feature.fromGeometry(
            Point.fromLngLat(
                135.516316,
                34.681345
            )
        )
        singleFeatureOne.addStringProperty(ICON_PROPERTY, RED_ICON_ID)
        val singleFeatureTwo = Feature.fromGeometry(
            Point.fromLngLat(
                135.509537,
                34.707929
            )
        )
        singleFeatureTwo.addStringProperty(ICON_PROPERTY, ICON_IMAGE_ID)
        val singleFeatureThree = Feature.fromGeometry(
            Point.fromLngLat(
                135.487953,
                34.680369
            )
        )
        singleFeatureThree.addStringProperty(ICON_PROPERTY, ICON_IMAGE_ID)

        val symbolLayerIconFeatureList: MutableList<Feature> = ArrayList()
        symbolLayerIconFeatureList.add(singleFeatureOne)
        symbolLayerIconFeatureList.add(singleFeatureTwo)
        symbolLayerIconFeatureList.add(singleFeatureThree)
        return symbolLayerIconFeatureList
    }

    public override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    public override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    fun inside(view: View?) {

    }

    fun out(view: View?) {

    }



    override fun onMapClick(point: LatLng): Boolean {
        Log.e("OnMapClick","$point")
        Log.e("OnMapClick","${mapboxMap.projection.toScreenLocation(point)}")
        return handleClickIcon(mapboxMap.projection.toScreenLocation(point))
    }

    private fun handleClickIcon(screenPoint: PointF): Boolean {
        val features: List<Feature> = mapboxMap.queryRenderedFeatures(screenPoint, "LAYER_ID")
        return if (features.isNotEmpty()) {
            // Show the Feature in the TextView to show that the icon is based on the ICON_PROPERTY key/value

            Toast.makeText(this,features[0].toJson().toString(),Toast.LENGTH_LONG).show()
            Log.e("Click",features[0].toString())
            Log.e("handleClickIcon","true")
            true
        } else {
            Log.e("handleClickIcon","false")
            false
        }
    }
}