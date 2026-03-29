package com.sr.fixit106.ui.main.fragments.create_post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.sr.fixit106.R
import com.sr.fixit106.ui.main.PostsViewModel
import com.sr.fixit106.utils.CityLocationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PickLocationFragment : Fragment() {

    companion object {
        const val RESULT_KEY = "pick_location_result"
        const val BUNDLE_LAT = "lat"
        const val BUNDLE_LNG = "lng"

        private val DEFAULT_CITY_CENTER = LatLng(32.0853, 34.7818)
        private const val DEFAULT_ZOOM = 13f
    }

    private val postsViewModel: PostsViewModel by activityViewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var mapView: MapView
    private lateinit var confirmBtn: MaterialButton

    private var googleMap: GoogleMap? = null
    private var marker: Marker? = null
    private var selected: LatLng? = null

    private var pendingCameraTarget: LatLng? = null
    private var mapLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_pick_location, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.pick_location_toolbar)
        mapView = view.findViewById(R.id.pick_location_map)
        confirmBtn = view.findViewById(R.id.pick_location_confirm)

        applyToolbarTopInset(toolbar)

        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        MapsInitializer.initialize(requireContext())

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            googleMap = map.apply {
                mapType = GoogleMap.MAP_TYPE_NORMAL
                uiSettings.isZoomControlsEnabled = true
                uiSettings.isCompassEnabled = true
                uiSettings.isMapToolbarEnabled = true
            }

            map.setOnMapLoadedCallback {
                mapLoaded = true
                applyPendingCamera()
            }

            centerMapOnCurrentUserCity()

            map.setOnMapClickListener { latLng ->
                selected = latLng
                marker?.remove()
                marker = map.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("Selected location")
                )
                map.animateCamera(CameraUpdateFactory.newLatLng(latLng))
            }
        }

        confirmBtn.setOnClickListener {
            val sel = selected ?: return@setOnClickListener

            setFragmentResult(
                RESULT_KEY,
                Bundle().apply {
                    putDouble(BUNDLE_LAT, sel.latitude)
                    putDouble(BUNDLE_LNG, sel.longitude)
                }
            )
            findNavController().navigateUp()
        }
    }

    private fun applyToolbarTopInset(toolbar: MaterialToolbar) {
        val initialTopPadding = toolbar.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = initialTopPadding + systemBars.top)
            insets
        }
        ViewCompat.requestApplyInsets(toolbar)
    }

    private fun centerMapOnCurrentUserCity() {
        postsViewModel.getCurrentUserLive().observe(viewLifecycleOwner) { user ->
            val city = user?.city.orEmpty()

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val coords = if (city.isNotBlank()) {
                    CityLocationUtils.getCoordinatesForCity(requireContext(), city)
                } else {
                    null
                }

                withContext(Dispatchers.Main) {
                    pendingCameraTarget = coords?.let { LatLng(it.first, it.second) }
                        ?: DEFAULT_CITY_CENTER
                    applyPendingCamera()
                }
            }
        }
    }

    private fun applyPendingCamera() {
        val map = googleMap ?: return
        val target = pendingCameraTarget ?: return
        if (!mapLoaded) return

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, DEFAULT_ZOOM))
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapView.onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        mapView.onDestroy()
        super.onDestroyView()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}