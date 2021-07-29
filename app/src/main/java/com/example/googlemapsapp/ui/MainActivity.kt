package com.example.googlemapsapp.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.googlemapsapp.R
import com.example.googlemapsapp.util.Permission.checkLocationPermission
import com.example.googlemapsapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragmentContainerView) as NavHostFragment

        navController = navHostFragment.navController

        if (checkLocationPermission(this)) {
            navController.navigate(R.id.action_permissionFragment_to_mapsFragment)
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}