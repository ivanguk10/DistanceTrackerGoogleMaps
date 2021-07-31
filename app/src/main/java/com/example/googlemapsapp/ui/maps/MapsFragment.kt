package com.example.googlemapsapp.ui.maps

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.googlemapsapp.R
import com.example.googlemapsapp.databinding.FragmentMapsBinding
import com.example.googlemapsapp.model.Result
import com.example.googlemapsapp.service.TrackerService
import com.example.googlemapsapp.util.*
import com.example.googlemapsapp.util.Constants.Companion.ACTION_SERVICE_START
import com.example.googlemapsapp.util.Constants.Companion.ACTION_SERVICE_STOP
import com.example.googlemapsapp.util.MapUtil.calculateDistance
import com.example.googlemapsapp.util.MapUtil.calculateTime
import com.example.googlemapsapp.util.MapUtil.setLocationCamera
import com.example.googlemapsapp.util.Permission.checkBackgroundLocationPermission
import com.example.googlemapsapp.util.Permission.requestBackgroundLocationPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, EasyPermissions.PermissionCallbacks {

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!

    private lateinit var map: GoogleMap
    private var locationsList = mutableListOf<LatLng>()
    private var polylineList = mutableListOf<Polyline>()

    private var startTime = 0L
    private var stopTime = 0L
    val started = MutableLiveData<Boolean>(false)

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentMapsBinding.inflate(layoutInflater, container, false)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.tracking = this

        binding.startBtn.setOnClickListener {
            onStartButtonClicked()
        }
        binding.stopBtn.setOnClickListener {
            onStopButtonClicked()
        }
        binding.resetBtn.setOnClickListener {
            resetButtonClicked()
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap!!
        map.isMyLocationEnabled = true
        map.setOnMyLocationButtonClickListener(this)

        map.uiSettings.apply {
            isZoomControlsEnabled = false
            isZoomGesturesEnabled = false
            isCompassEnabled = false
            isRotateGesturesEnabled = false
            isScrollGesturesEnabled = false
            isTiltGesturesEnabled = false
        }
        observeTrackerService()
    }

    private fun observeTrackerService() {
        TrackerService.locationsList.observe(viewLifecycleOwner, {
            if (it != null) {
                locationsList = it
                if (locationsList.size > 1) {
                    binding.stopBtn.enable()
                }
                drawPolyline()
                followPolyline()
            }
        })
        TrackerService.started.observe(viewLifecycleOwner, {
            started.value = it
        })

        TrackerService.starTime.observe(viewLifecycleOwner, {
            startTime = it
        })

        TrackerService.stopTime.observe(viewLifecycleOwner, {
            stopTime = it
            if (stopTime != 0L) {
                showAllPath()
                displayResults()
            }
        })
    }

    private fun drawPolyline() {
        val polyLine = map.addPolyline(
            PolylineOptions().apply {
                width(10f)
                color(Color.BLUE)
                jointType(JointType.ROUND)
                startCap(ButtCap())
                endCap(ButtCap())
                addAll(locationsList)
            }
        )
        polylineList.add(polyLine)
    }

    private fun followPolyline() {
        if (locationsList.isNotEmpty()) {
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    setLocationCamera(
                        locationsList.last()
                    )
                ), 1000, null)
        }
    }

    private fun showAllPath() {
        val bounds = LatLngBounds.Builder()
        for (location in locationsList) {
            bounds.include(location)
        }
        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(), 100
            ), 2000, null
        )
    }

    private fun displayResults() {
        val result = Result(
            calculateDistance(locationsList),
            calculateTime(startTime, stopTime)
        )
        lifecycleScope.launch {
            delay(2500)
            val action = MapsFragmentDirections.actionMapsFragmentToResultFragment(result)
            findNavController().navigate(action)
            binding.startBtn.apply {
                hide()
                enable()
            }
            binding.stopBtn.hide()
            binding.resetBtn.show()
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        binding.hintTextView.animate().alpha(0f).duration = 1500
        lifecycleScope.launch {
            delay(2500)
            binding.hintTextView.hide()
            binding.startBtn.show()
        }
        return false
    }

    private fun onStartButtonClicked() {
        if (checkBackgroundLocationPermission(requireContext())) {
            startCountDown()
            binding.startBtn.animate().alpha(0f).duration = 1500
            lifecycleScope.launch {
                delay(1500)
                binding.startBtn.disable()
                binding.startBtn.hide()
                delay(1500)
                binding.stopBtn.show()
            }
        }
        else {
            requestBackgroundLocationPermission(this)
        }
    }

    private fun startCountDown() {
        binding.timerTextView.show()
        binding.stopBtn.disable()
        val timer = object : CountDownTimer(4000, 1000) {
            override fun onTick(millisecondsUntilFinish: Long) {
                val currentSecond = millisecondsUntilFinish / 1000
                if (currentSecond.toString() == "0") {
                    binding.timerTextView.text = "GO"
                    binding.timerTextView.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.green_500
                    ))
                }
                else {
                    binding.timerTextView.text = currentSecond.toString()
                }

            }

            override fun onFinish() {
                sendActionCommandToService(ACTION_SERVICE_START)
                binding.timerTextView.hide()
                binding.stopBtn.enable()
            }

        }
        timer.start()
    }

    private fun onStopButtonClicked() {
        stopForegroundService()
        binding.stopBtn.hide()
        binding.startBtn.setBackgroundColor(ContextCompat.getColor(requireContext() ,R.color.green_500))
        binding.startBtn.show()
        binding.startBtn.animate().alpha(1f).duration = 1500
        if (binding.startBtn.visibility == View.VISIBLE) {
            Toast.makeText(requireContext(), "Visible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopForegroundService() {
        binding.startBtn.disable()
        sendActionCommandToService(ACTION_SERVICE_STOP)
    }

    private fun resetButtonClicked() {
        mapReset()
    }

    @SuppressLint("MissingPermission")
    private fun mapReset() {
        fusedLocationProviderClient.lastLocation.addOnCompleteListener {
            val lastKnownLocation = LatLng(
                it.result.latitude,
                it.result.longitude
            )
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    setLocationCamera(lastKnownLocation)
                )
            )
            for (polyLine in polylineList) {
                polyLine.remove()
            }
            locationsList.clear()
            binding.resetBtn.hide()
            binding.startBtn.show()
        }
    }

    private fun sendActionCommandToService(action: String) {
        Intent(
            requireContext(),
            TrackerService::class.java
        ).apply {
            this.action = action
            requireContext().startService(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            SettingsDialog.Builder(requireActivity()).build().show()
        }
        else
            requestBackgroundLocationPermission(this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        onStartButtonClicked()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}