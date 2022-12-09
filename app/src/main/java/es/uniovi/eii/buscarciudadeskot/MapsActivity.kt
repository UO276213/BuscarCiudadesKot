package es.uniovi.eii.buscarciudadeskot

import android.app.AlertDialog
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_SATELLITE
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.data.geojson.GeoJsonLayer
import es.uniovi.eii.buscarciudadeskot.datos.Resultados
import es.uniovi.eii.sdm.buscarciudadeskot.datos.GestorCiudades
import kotlinx.android.synthetic.main.activity_maps.*
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var posCiudad: LatLng
    private lateinit var marcaUsuario: Marker

    private val gc = GestorCiudades()
    private val res = Resultados()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Preparamos manejadores de eventos
        botonAceptar.setOnClickListener { onClickAceptar() }

        botonSiguiente.setOnClickListener {
            onClickSiguiente()
        }

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Siguiente ciudad aleatoria
        siguienteCiudad()
        vistaInicial()
        definirInteraccionMapa()
    }


    private fun siguienteCiudad() {
        // pasamos a la siguiente ciudad y la ponemos en el campo
        try {
            val c = gc.nextCiudad()
            if (c != null) {
                campoCiudad.text = c.nombre
                posCiudad = LatLng(c.latitud, c.longitud)
            }
        } catch (e: NoSuchElementException) {
            finalCiudades()

//            gc.reiniciarCiudades()
//            siguienteCiudad()
        }
    }

    private fun finalCiudades() {
        // 1. Instantiate an <code><a href="/reference/android/app/AlertDialog.Builder.html">AlertDialog.Builder</a></code> with its constructor
        val builder = AlertDialog.Builder(this@MapsActivity)

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage("No hay más ciudades\nTu puntuación final es: ${res.puntos} puntos")//R.string.dialog_message)
            .setTitle("Fin del juego")//R.string.dialog_title)

        // 3. Get the <code><a href="/reference/android/app/AlertDialog.html">AlertDialog</a></code> from <code><a href="/reference/android/app/AlertDialog.Builder.html#create()">create()</a></code>
        val dialog = builder.create()
        dialog.show()
    }

    private fun vistaInicial() {
        mMap.mapType = MAP_TYPE_SATELLITE
        val sw = LatLng(33.385177, -9.680849)
//        mMap.addMarker(
//            MarkerOptions()
//                .position(sw)
//        )
        val ne = LatLng(44.952361, 4.847552)
//        mMap.addMarker(
//            MarkerOptions()
//                .position(ne)
//        )
        val peninsulaBounds = LatLngBounds(
            sw,  // SW bounds
            ne // NE bounds
        )
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(peninsulaBounds, 1080, 1080, 0))

        dibujarProvincias()
    }

    private fun dibujarProvincias() {
//        val layer = KmlLayer(mMap, R.raw.provincias_espanolas, applicationContext)
//        layer.addLayerToMap()

        val layer = GeoJsonLayer(mMap, R.raw.comunidades_autonomas_espanolas, applicationContext)
        layer.defaultLineStringStyle.color = Color.GREEN

        layer.addLayerToMap()

    }

    private fun definirInteraccionMapa() {
        // Recupera la referencia a los controles
        val controles = mMap.getUiSettings()
        // Muestra el control de zoom
        controles.isZoomControlsEnabled = false
        // Deshabilita el gesto de zoom
        controles.isZoomGesturesEnabled = false

        // evento pulsación larga sobre el mapa para establecer marcador
        mMap.setOnMapLongClickListener { punto ->
            if (::marcaUsuario.isInitialized)
                marcaUsuario.remove()
            marcaUsuario = mMap.addMarker(
                MarkerOptions()
                    .position(punto)
                    .title("Marcador creado por el usuario")
            ) as Marker
        }
    }

    private fun onClickAceptar() {
        Log.d(Companion.TAG, "onClickAceptar: llegamos al onclick")
        mMap.addMarker(
            MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.estrella32))
                .anchor(0.5f, 0.5f)
                .position(posCiudad)
        )
        for (i in 1..3) {
            mMap.addCircle(
                CircleOptions()
                    .center(posCiudad)
                    .radius(i.toDouble() * franjas)
                    .fillColor(0x0F00FF00)
                    .strokeColor(0xFF0000FF.toInt())
            )
        }
        mMap.addPolyline(
            PolylineOptions()
                .add(posCiudad)
                .add(marcaUsuario.position)
                .color(-0xff0100)
                .width(5f)
        )

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(posCiudad, 8f))
        res.addPuntos(calcularDistancia(posCiudad, marcaUsuario.position))
    }

    private fun calcularDistancia(posCiudad: LatLng, posUsuario: LatLng): Float {
        // mide la distancia entre los puntos
        val distancias = FloatArray(1)
        Location.distanceBetween(
            posCiudad.latitude, posCiudad.longitude,
            posUsuario.latitude, posUsuario.longitude, distancias
        )
        return distancias[0]
    }

    /**
     * Click sobre el botón siguiente
     */
    fun onClickSiguiente() {
        mMap.clear()    // deja el mapa límpio sin ningún elemento
        siguienteCiudad()
        vistaInicial()

    }

    companion object {
        const val TAG = "MapsActivity"
        const val franjas = 30000    // distancia separación en metros
    }
}