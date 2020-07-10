package com.mapbox.navigation.core.activity.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.common.logger.MapboxLogger
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.TimeFormat.TWENTY_FOUR_HOURS
import com.mapbox.navigation.base.internal.VoiceUnit.METRIC
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.base.trip.notification.NotificationAction
import com.mapbox.navigation.core.Rounding
import com.mapbox.navigation.core.internal.MapboxDistanceFormatter
import com.mapbox.navigation.core.internal.trip.service.MapboxTripService
import com.mapbox.navigation.core.test.R
import com.mapbox.navigation.trip.notification.MapboxTripNotification
import com.mapbox.navigation.ui.route.NavigationMapRoute
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.monitorChannelWithException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * This activity shows how to use the Navigation SDK's
 * [MapboxTripService] to update the Android-system
 * foreground service notification. The [MapboxTripNotification] class
 * updates the notification's displayed navigation information.
 */
class TripServiceActivityKt : AppCompatActivity(), OnMapReadyCallback {

    private var mainJobController = ThreadController.getMainScopeAndRootJob()
    private var mapboxMap: MapboxMap? = null
    private lateinit var mapView: MapView
    private lateinit var toggleNotification: Button
    private lateinit var notifyTextView: TextView
    private lateinit var mapboxTripNotification: MapboxTripNotification
    private lateinit var navigationMapRoute: NavigationMapRoute
    private lateinit var mapboxTripService: MapboxTripService
    private var textUpdateJob: Job = Job()

    @SuppressLint("MissingPermission")
    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            navigationMapRoute = NavigationMapRoute.Builder(mapView, mapboxMap, this).build()
            newOrigin()
            toggleNotification.setOnClickListener {
                when (mapboxTripService.hasServiceStarted()) {
                    true -> {
                        stopService()
                    }
                    false -> {
                        mapboxTripService.startService()
                        changeText()
                        toggleNotification.text = "Stop"
                        monitorNotificationActionButton(MapboxTripNotification.notificationActionButtonChannel)
                    }
                }
            }
        }
    }

    /*
     * Activity lifecycle methods
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test_activity_trip_service)

        mapView = findViewById(R.id.mapView)
        toggleNotification = findViewById(R.id.toggleNotification)
        notifyTextView = findViewById(R.id.notifyTextView)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val formatter = MapboxDistanceFormatter.builder()
                .withRoundingIncrement(Rounding.INCREMENT_FIFTY)
                .withUnitType(METRIC)
                .withLocale(this.inferDeviceLocale())
                .build(this)

        mapboxTripNotification = MapboxTripNotification(
                applicationContext,
                NavigationOptions.Builder()
                        .distanceFormatter(formatter)
                        .timeFormatType(TWENTY_FOUR_HOURS)
                        .build()
        )

        // If you want to use Mapbox provided Service do this
        mapboxTripService = MapboxTripService(applicationContext, mapboxTripNotification, MapboxLogger)

        /*
        // else do this
        val intent = Intent(applicationContext, <Your_own_service>::class.java)
        mapboxTripService = MapboxTripService(mapboxTripNotification, {
            try {
                applicationContext.startService(intent)
            } catch (e: IllegalStateException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(intent)
                } else {
                    throw e
                }
            }
        }, {
            stopService(intent)
        })*/
    }

    private fun monitorNotificationActionButton(channel: ReceiveChannel<NotificationAction>) {
        mainJobController.scope.monitorChannelWithException(channel, { notificationAction ->
            when (notificationAction) {
                NotificationAction.END_NAVIGATION -> stopService()
            }
        })
    }

    private fun stopService() {
        textUpdateJob.cancel()
        mapboxTripService.stopService()
        toggleNotification.text = "Start"
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
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
        mapboxTripService.stopService()
        ThreadController.cancelAllNonUICoroutines()
        ThreadController.cancelAllUICoroutines()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    private fun newOrigin() {
        mapboxMap?.let { map ->
            val latLng = LatLng(37.791674, -122.396469)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.0))
        }
    }

    private fun changeText() {
        textUpdateJob = mainJobController.scope.launch {
            while (isActive) {
                val text = "Time elapsed: + ${SystemClock.elapsedRealtime()}"
                notifyTextView.text = text
                mapboxTripService.updateNotification(
                        RouteProgress.Builder()
                                .currentLegProgress(
                                        RouteLegProgress.Builder()
                                                .currentStepProgress(
                                                        RouteStepProgress.Builder()
                                                                .distanceRemaining(100f)
                                                                .build()
                                                ).build()
                                ).build()
                )
                Timber.i(text)
                delay(1000L)
            }
        }
    }
}