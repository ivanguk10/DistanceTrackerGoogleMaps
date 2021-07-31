package com.example.googlemapsapp.util

import android.location.Location
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import java.text.DecimalFormat

object MapUtil {

    fun setLocationCamera(location: LatLng): CameraPosition {
        return CameraPosition.Builder()
            .target(location)
            .zoom(18f)
            .build()
    }

    fun calculateTime(startTime: Long, stopTime: Long): String {
        val time = stopTime - startTime

        val seconds = (time/1000).toInt() % 60
        val minutes = (time/(1000 * 60) % 60)
        val hours = (time/ (1000 * 60 * 60) % 24)

        return "$hours: $minutes: $seconds"
    }

    fun calculateDistance(locationList: MutableList<LatLng>): String {
        if (locationList.size > 1) {
            val meters = SphericalUtil.computeDistanceBetween(
                locationList.first(),
                locationList.last()
            )
            val kilometers = meters/1000

            return DecimalFormat("#.##").format(kilometers)
        }
        return "0.00"
    }
}