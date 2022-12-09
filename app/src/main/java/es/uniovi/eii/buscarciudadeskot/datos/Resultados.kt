package es.uniovi.eii.buscarciudadeskot.datos

import es.uniovi.eii.buscarciudadeskot.MapsActivity

class Resultados {
    var puntos = 0
        private set

    fun addPuntos(distancia: Float) {
        when {
            distancia < MapsActivity.franjas -> puntos += 10
            distancia < MapsActivity.franjas * 2 -> puntos += 5
            distancia < MapsActivity.franjas * 3 -> puntos += 2
        }
    }

    fun reiniciaPuntos() {
        puntos = 0
    }
}
