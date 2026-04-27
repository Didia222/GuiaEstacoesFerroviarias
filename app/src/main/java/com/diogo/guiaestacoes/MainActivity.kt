package com.diogo.guiaestacoes

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.location.Location
import android.os.Bundle
import android.provider.BaseColumns
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.cursoradapter.widget.CursorAdapter
import androidx.cursoradapter.widget.SimpleCursorAdapter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import java.text.Normalizer
import kotlin.math.*
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var sugestoesAdapter: SimpleCursorAdapter
    private var todosOsNomesEstacoes: List<String> = emptyList()
    private var listaEstacoesOficiais: List<Estacao> = emptyList()


    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val listaMarcadores = mutableListOf<Marker>()
    private lateinit var db: FirebaseFirestore

    private var localizacaoAtual: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        db.firestoreSettings = settings

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        configurarPesquisa()




        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabLocation).setOnClickListener {
            verificarPermissoesECentrar()
        }
    }

    override fun onNewIntent(intent: Intent) {



        super.onNewIntent(intent)

        setIntent(intent)
        if (::map.isInitialized){
            verificarRetornoDeDetalhes(intent)
        }

    }

    private fun verificarRetornoDeDetalhes(intent: Intent) {
        val lat = intent.getDoubleExtra("LAT_RETORNO", 0.0)
        val lng = intent.getDoubleExtra("LNG_RETORNO", 0.0)
        if (lat != 0.0 && lng != 0.0) {
            val localizacao = LatLng(lat, lng)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(localizacao, 16f))
            listaMarcadores.find { it.position == localizacao }?.showInfoWindow()
        }
    }

    private fun verificarPermissoesECentrar() {
        if (!::map.isInitialized) return
        // Atenção ao != (NÃO É IGUAL A GRANTED)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // Se chegou aqui, é porque tem permissão!
        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = false

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            }
        }
    }












    private fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun carregarDados() {
        db.collection("Estacao").addSnapshotListener { snapshots, error ->
            if (error != null) return@addSnapshotListener

            if (snapshots != null) {
                val listaPesquisaTemp = mutableListOf<String>()
                listaEstacoesOficiais = snapshots.toObjects(Estacao::class.java)

                map.clear()
                listaMarcadores.clear()

                for (estacao in listaEstacoesOficiais) {
                    // Adicionamos sempre o nome à lista de pesquisa (para a pessoa poder pesquisar mesmo estações longe)
                    listaPesquisaTemp.add(estacao.nome)

                    if (estacao.latitude != 0.0) {

                        // 1. LÓGICA REPOSTA: O Filtro dos 10km (RF-1)
                        var dentroDoRaio = true // Por defeito, assumimos que está dentro

                        // Se o telemóvel já tiver o GPS trancado, calculamos a distância
                        if (localizacaoAtual != null) {
                            val distanciaKm = calcularDistancia(
                                localizacaoAtual!!.latitude, localizacaoAtual!!.longitude,
                                estacao.latitude, estacao.longitude
                            )
                            // Só é válido se for menor ou igual a 10 km
                            dentroDoRaio = distanciaKm <= 10.0
                        }

                        // 2. Só desenha o pino no mapa SE estiver dentro do raio!
                        if (dentroDoRaio) {
                            val textoParaAnalise = (estacao.nome + " " + estacao.Discricao_hist).lowercase()
                            val tipoDetectado = when {
                                textoParaAnalise.contains("apeadeiro") -> "Apeadeiro"
                                textoParaAnalise.contains("paragem") || textoParaAnalise.contains("halte") -> "Apeadeiro"
                                else -> "Estação Ferroviária"
                            }

                            val markerOptions = MarkerOptions()
                                .position(LatLng(estacao.latitude, estacao.longitude))
                                .title(estacao.nome)
                                .snippet(tipoDetectado)

                            // Diferenciação por cor: Apeadeiro (Azul) / Estação (Vermelho)
                            if (tipoDetectado == "Apeadeiro") {
                                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            }

                            val marker = map.addMarker(markerOptions)
                            marker?.tag = estacao
                            if (marker != null) listaMarcadores.add(marker)
                        }
                    }
                }
                todosOsNomesEstacoes = listaPesquisaTemp

                // Mantém os filtros dos Chips a funcionar corretamente com as novas estações
                val chipGroup = findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupFiltros)
                if (chipGroup != null) {
                    filtrarMarcadoresNoMapa(chipGroup.checkedChipId)
                }
            }
        }
    }







    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMarkerClickListener(this)
        ativarLocalizacaoUsuario()
        val chipGroup = findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupFiltros)
        chipGroup?.setOnCheckedChangeListener { _, checkedId ->
            filtrarMarcadoresNoMapa(checkedId)
        }

        verificarRetornoDeDetalhes(intent)
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val estacao = marker.tag as? Estacao
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_detalhes_estacao, null)
        dialog.setContentView(view)

        var distanciaTexto = ""
        if (localizacaoAtual != null && estacao != null) {
            val km = calcularDistancia(
                localizacaoAtual!!.latitude, localizacaoAtual!!.longitude,
                estacao.latitude, estacao.longitude
            )
            distanciaTexto = " (a ${String.format("%.1f", km)} km de ti)"
        }

        view.findViewById<TextView>(R.id.tvNomeEstacao).text = marker.title
        view.findViewById<TextView>(R.id.tvDescricao).text = "${marker.snippet}$distanciaTexto"

        view.findViewById<Button>(R.id.btnVerMais).setOnClickListener {
            val intent = Intent(this, DetalhesActivity::class.java).apply {
                putExtra("NOME", marker.title)
                putExtra("TIPO", marker.snippet)
                putExtra("HISTORIA", estacao?.Discricao_hist)
                putExtra("LATITUDE", estacao?.latitude ?: 0.0)
                putExtra("LONGITUDE", estacao?.longitude ?: 0.0)
            }
            startActivity(intent)
            dialog.dismiss()
        }

        view.findViewById<Button>(R.id.btnVerHorarios).setOnClickListener {
            startActivity(Intent(this, HorariosActivity::class.java).apply {
                putExtra("ESTACAO_NOME", marker.title)
            })
            dialog.dismiss()
        }

        view.findViewById<Button>(R.id.btnGaleria).setOnClickListener {
            startActivity(Intent(this, GaleriaActivity::class.java).apply {
                putExtra("NOME", marker.title)
            })
            dialog.dismiss()
        }

        view.findViewById<Button>(R.id.btnAvaliar).setOnClickListener {
            startActivity(Intent(this, AvaliacaoActivity::class.java).apply {
                putExtra("NOME", marker.title)
            })
            dialog.dismiss()
        }

        dialog.show()
        return true
    }

    private fun ativarLocalizacaoUsuario() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let {
                    localizacaoAtual = it
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 12f))
                }
                carregarDados()
            }.addOnFailureListener { carregarDados() }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            carregarDados()
        }
    }

    private fun normalizarTexto(t: String): String {
        return Normalizer.normalize(t, Normalizer.Form.NFD).replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").uppercase().trim()
    }

    private fun configurarPesquisa() {
        val searchView = findViewById<SearchView>(R.id.searchViewEstacoes)
        sugestoesAdapter = SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, null, arrayOf("estacaoNome"), intArrayOf(android.R.id.text1), CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER)
        searchView.suggestionsAdapter = sugestoesAdapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?): Boolean {
                q?.let { procurarEstacaoNoMapa(it) }
                searchView.clearFocus()
                return true
            }
            override fun onQueryTextChange(n: String?): Boolean {
                filtrarSugestoes(n)
                return true
            }
        })

        searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(p: Int): Boolean = false
            override fun onSuggestionClick(p: Int): Boolean {
                val cursor = sugestoesAdapter.cursor as Cursor
                if (cursor.moveToPosition(p)) {
                    val nome = cursor.getString(cursor.getColumnIndexOrThrow("estacaoNome"))
                    searchView.setQuery(nome, false)
                    procurarEstacaoNoMapa(nome)
                    searchView.clearFocus()
                }
                return true
            }
        })
    }

    private fun filtrarSugestoes(query: String?) {
        val cursor = MatrixCursor(arrayOf(BaseColumns._ID, "estacaoNome"))
        if (!query.isNullOrBlank()) {
            val q = normalizarTexto(query)
            todosOsNomesEstacoes.filter { normalizarTexto(it).contains(q) }
                .forEachIndexed { i, nome -> cursor.addRow(arrayOf(i, nome)) }
        }
        sugestoesAdapter.changeCursor(cursor)
    }

    private fun procurarEstacaoNoMapa(nome: String) {
        val q = normalizarTexto(nome)
        val marcadorExistente = listaMarcadores.find { normalizarTexto(it.title ?: "").contains(q) }

        if (marcadorExistente != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(marcadorExistente.position, 15f))
            marcadorExistente.showInfoWindow()
        } else {
            val estacaoLonge = listaEstacoesOficiais.find { normalizarTexto(it.nome).contains(q) }
            estacaoLonge?.let {
                val loc = LatLng(it.latitude, it.longitude)
                val novoMarker = map.addMarker(MarkerOptions()
                    .position(loc)
                    .title(it.nome)
                    .snippet("Fora do raio de 10km")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))
                novoMarker?.tag = it
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15f))
                novoMarker?.showInfoWindow()
            }

        }
    }
    private fun filtrarMarcadoresNoMapa(chipId: Int) {
        for (marker in listaMarcadores) {
            val tipoMarcador = marker.snippet ?: ""

            // Oculta ou mostra os pinos no mapa dependendo do filtro selecionado
            when (chipId) {
                R.id.chipTodas -> marker.isVisible = true
                R.id.chipEstacoes -> marker.isVisible = tipoMarcador.contains("Estação", ignoreCase = true)
                R.id.chipApeadeiros -> marker.isVisible = tipoMarcador.contains("Apeadeiro", ignoreCase = true)
            }
        }
    }
}