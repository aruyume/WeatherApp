package com.example.weatherapp.ui.fragments.home

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.example.weatherapp.R
import com.example.weatherapp.data.model.CurrentLocation
import com.example.weatherapp.databinding.FragmentHomeBinding
import com.example.weatherapp.storage.SharedPreferenceManager
import com.google.android.gms.location.LocationServices
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class HomeFragment : Fragment() {

    companion object {
        const val REQUEST_KEY_MANUAL_LOCATION_SEARCH = "manualLocationSearch"
        const val KEY_LOCATION_TEXT = "locationText"
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModel()

    private val fusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }

    private val geocoder by lazy {
        Geocoder(requireContext())
    }

    private val weatherDataAdapter = WeatherDataAdapter(
        onLocationClicked = {
            showLocationOptions()
        }
    )

    private val sharedPreferenceManager: SharedPreferenceManager by inject()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentLocation()
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private var isInitialLocationSet: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setCurrentLocation(currentLocation = sharedPreferenceManager.getCurrentLocation())
        setWeatherDataAdapter()
        setObservers()
        setListeners()
        if (!isInitialLocationSet) {
            setCurrentLocation(currentLocation = sharedPreferenceManager.getCurrentLocation())
            isInitialLocationSet = true
        }
    }

    private fun setListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            setCurrentLocation(sharedPreferenceManager.getCurrentLocation())
        }
    }

    private fun setObservers() {
        with(viewModel) {
            currentLocation.observe(viewLifecycleOwner) {
                val currentLocationDataState = it.getContentIfNotHandled() ?: return@observe
                if (currentLocationDataState.isLoading) {
                    showLoading()
                }
                currentLocationDataState.currentLocation?.let { currentLocation ->
                    hideLoading()
                    sharedPreferenceManager.saveCurrentLocation(currentLocation)
                    setCurrentLocation(currentLocation)
                }
                currentLocationDataState.error?.let { error ->
                    hideLoading()
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                }
            }
            weatherData.observe(viewLifecycleOwner) {
                val weatherDataState = it.getContentIfNotHandled() ?: return@observe
                binding.swipeRefreshLayout.isRefreshing = weatherDataState.isLoading
                weatherDataState.currentWeather?.let { currentWeather ->
                    weatherDataAdapter.setCurrentWeather(currentWeather)
                }
                weatherDataState.forecast?.let { forecasts ->
                    weatherDataAdapter.setForecastData(forecasts)
                }
                weatherDataState.error?.let { error ->
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setWeatherDataAdapter() {
        binding.rvWeatherData.itemAnimator = null
        binding.rvWeatherData.adapter = weatherDataAdapter
    }

    private fun setCurrentLocation(currentLocation: CurrentLocation? = null) {
        weatherDataAdapter.setCurrentLocation(currentLocation ?: CurrentLocation())
        currentLocation?.let { getWeatherData(currentLocation = it) }
    }

    private fun getCurrentLocation() {
        viewModel.getCurrentLocation(fusedLocationProviderClient, geocoder)
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun proceedWithCurrentLocation() {
        if (isLocationPermissionGranted()) {
            getCurrentLocation()
        } else {
            requestLocationPermission()
        }
    }

    private fun showLocationOptions() {
        val options = arrayOf("Current Location", "Search Manually")
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Choose Location Method")
            setItems(options) { _, which ->
                when (which) {
                    0 -> proceedWithCurrentLocation()
                    1 -> startManualLocationSearch()
                }
            }
            show()
        }
    }

    private fun showLoading() = with(binding) {
        rvWeatherData.visibility = View.GONE
        swipeRefreshLayout.isEnabled = false
        swipeRefreshLayout.isRefreshing = true
    }

    private fun hideLoading() = with(binding) {
        rvWeatherData.visibility = View.VISIBLE
        swipeRefreshLayout.isEnabled = true
        swipeRefreshLayout.isRefreshing = false
    }

    private fun startManualLocationSearch() {
        startListeningManualLocationSelection()
        findNavController().navigate(R.id.action_homeFragment_to_locationFragment)
    }

    private fun startListeningManualLocationSelection() {
        setFragmentResultListener(REQUEST_KEY_MANUAL_LOCATION_SEARCH) { _, bundle ->
            stopListeningManualLocationSelection()
            val currentLocation = CurrentLocation(
                location = bundle.getString(KEY_LOCATION_TEXT) ?: "N/A",
                latitude = bundle.getDouble(KEY_LATITUDE),
                longitude = bundle.getDouble(KEY_LONGITUDE)
            )
            sharedPreferenceManager.saveCurrentLocation(currentLocation)
            setCurrentLocation(currentLocation)
        }
    }

    private fun stopListeningManualLocationSelection() {
        clearFragmentResultListener(REQUEST_KEY_MANUAL_LOCATION_SEARCH)
    }

    private fun getWeatherData(currentLocation: CurrentLocation) {
        if (currentLocation.latitude != null && currentLocation.longitude != null) {
            viewModel.getWeatherData(
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}