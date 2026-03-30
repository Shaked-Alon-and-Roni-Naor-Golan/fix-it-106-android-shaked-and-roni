package com.sr.fixit.ui.main.fragments.map

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.sr.fixit.R
import com.sr.fixit.data.posts.PostWithSender
import com.sr.fixit.ui.main.PostsViewModel
import com.sr.fixit.utils.CityLocationUtils
import com.sr.fixit.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.InfoWindowAdapter {

    companion object {
        private val DEFAULT_CITY_CENTER = LatLng(32.0853, 34.7818) // Tel Aviv
        private const val CITY_ZOOM = 13f
    }

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    private val viewModel: PostsViewModel by activityViewModels()

    private var latestPosts: List<PostWithSender> = emptyList()
    private var pendingCameraTarget: LatLng? = null
    private var mapLoaded = false
    private var shouldFitPosts = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        mapView = view.findViewById(R.id.map)
        MapsInitializer.initialize(requireContext())
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map.apply {
            mapType = GoogleMap.MAP_TYPE_NORMAL
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isCompassEnabled = true
            uiSettings.isMapToolbarEnabled = true
            setOnMarkerClickListener(this@MapFragment)
            setInfoWindowAdapter(this@MapFragment)
            setPadding(0, 0, 0, dpToPx(88))
        }

        map.setOnMapLoadedCallback {
            mapLoaded = true
            applyPendingCamera()
        }

        observeCurrentUserCity()
        observePosts()
    }

    private fun observeCurrentUserCity() {
        viewModel.getCurrentUserLive().observe(viewLifecycleOwner) { user ->
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
                    shouldFitPosts = coords == null
                    applyPendingCamera()
                }
            }
        }
    }

    private fun observePosts() {
        viewModel.getAllPosts().observe(viewLifecycleOwner) { postsList ->
            latestPosts = postsList

            if (postsList.isEmpty()) {
                viewModel.invalidatePosts()
            }

            renderMarkers(postsList)

            if (shouldFitPosts) {
                applyPendingCamera()
            }
        }
    }

    private fun renderMarkers(postsList: List<PostWithSender>) {
        val map = googleMap ?: return

        map.clear()

        postsList.forEach { post ->
            val latLng = LatLng(post.post.locationLat, post.post.locationLng)
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(post.post.title)
                    .snippet(post.post.description)
            )?.tag = post
        }
    }

    private fun applyPendingCamera() {
        val map = googleMap ?: return
        if (!mapLoaded) return

        if (shouldFitPosts && latestPosts.isNotEmpty()) {
            if (latestPosts.size == 1) {
                val onlyPost = latestPosts.first().post
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(onlyPost.locationLat, onlyPost.locationLng),
                        CITY_ZOOM
                    )
                )
                return
            }

            val boundsBuilder = LatLngBounds.builder()
            latestPosts.forEach { post ->
                boundsBuilder.include(LatLng(post.post.locationLat, post.post.locationLng))
            }

            map.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120))
            return
        }

        val target = pendingCameraTarget ?: DEFAULT_CITY_CENTER
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, CITY_ZOOM))
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        marker.showInfoWindow()
        return true
    }

    override fun getInfoContents(marker: Marker): View? {
        val post = marker.tag as? PostWithSender ?: return null
        val infoWindowView = layoutInflater.inflate(R.layout.custom_info_window, null)

        val titleTextView = infoWindowView.findViewById<TextView>(R.id.titleTextView)
        val descriptionTextView = infoWindowView.findViewById<TextView>(R.id.descriptionTextView)
        val imageView = infoWindowView.findViewById<ImageView>(R.id.imageView)
        val userNameTextView = infoWindowView.findViewById<TextView>(R.id.userIdTextView)

        titleTextView.text = post.post.title
        descriptionTextView.text = post.post.description
        userNameTextView.text = "By: ${post.sender.name}"

        post.post.image?.let {
            val bitmap = ImageUtils.decodeBase64ToImage(it)
            imageView.setImageBitmap(bitmap)
        } ?: imageView.setImageResource(R.drawable.add_image_icon)

        return infoWindowView
    }

    override fun getInfoWindow(marker: Marker): View? = null

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

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}