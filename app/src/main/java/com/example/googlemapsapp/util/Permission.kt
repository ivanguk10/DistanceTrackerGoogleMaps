package com.example.googlemapsapp.util

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.fragment.app.Fragment
import com.example.googlemapsapp.util.Constants.Companion.PERMISSION_BACKGROUND_LOCATION_REQUEST_CODE
import com.example.googlemapsapp.util.Constants.Companion.PERMISSION_LOCATION_REQUEST_CODE
import com.vmadalin.easypermissions.EasyPermissions

object Permission {

    fun checkLocationPermission(context: Context): Boolean {
        return EasyPermissions.hasPermissions(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    fun requestLocationPermission(fragment: Fragment) {
        EasyPermissions.requestPermissions(
            fragment,
            "This app can't work without Location Permission...",
            PERMISSION_LOCATION_REQUEST_CODE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    fun checkBackgroundLocationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return EasyPermissions.hasPermissions(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
        return true
    }

    fun requestBackgroundLocationPermission(fragment: Fragment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                fragment,
                "This app can't work without Background Location Permission...",
                PERMISSION_BACKGROUND_LOCATION_REQUEST_CODE,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }
}