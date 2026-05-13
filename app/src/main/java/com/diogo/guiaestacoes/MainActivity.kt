package com.diogo.guiaestacoes

import android.Manifest
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

class MainActivity : BaseActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

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
                    listaPesquisaTemp.add(estacao.nome)

                    if (estacao.latitude != 0.0) {
                        var dentroDoRaio = true

                        if (localizacaoAtual != null) {
                            val distanciaKm = calcularDistancia(
                                localizacaoAtual!!.latitude, localizacaoAtual!!.longitude,
                                estacao.latitude, estacao.longitude
                            )
                            dentroDoRaio = distanciaKm <= 10.0
                        }

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

            val searchView = findViewById<SearchView>(R.id.searchViewEstacoes)
            searchView.setQuery("", false)
            filtrarMarcadoresNoMapa(checkedId)
        }

        verificarRetornoDeDetalhes(intent)
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val estacao = marker.tag as? Estacao
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_detalhes_estacao, null)
        dialog.setContentView(view)

        // 1. Determinar o Tipo Base (Estação ou Apeadeiro)
        var tipoBase = marker.snippet ?: "Estação Ferroviária"

        // Se for um pino laranja da pesquisa, o snippet é "Fora do raio...", por isso
        // temos de extrair o verdadeiro tipo lendo a história.
        if (tipoBase == "Fora do raio de 10km") {
            tipoBase = if (estacao?.Discricao_hist?.lowercase()?.contains("apeadeiro") == true) "Apeadeiro" else "Estação Ferroviária"
        }

        // 2. Calcular a Distância Real e formatar o texto
        var avisoDistancia = ""
        if (localizacaoAtual != null && estacao != null) {
            val km = calcularDistancia(
                localizacaoAtual!!.latitude, localizacaoAtual!!.longitude,
                estacao.latitude, estacao.longitude
            )
            if (km > 10.0) {
                avisoDistancia = " • Fora do raio de 10km (a ${String.format("%.1f", km)} km de ti)"
            }
        } else if (marker.snippet == "Fora do raio de 10km") {
            // Fallback: Se o GPS falhou mas sabemos que veio da pesquisa de fora do raio
            avisoDistancia = " • Fora do raio de 10km"
        }

        // 3. Juntar tudo
        val subtituloCompleto = "$tipoBase$avisoDistancia"

        // Atualizamos a Bottom Sheet
        view.findViewById<TextView>(R.id.tvNomeEstacao).text = marker.title
        view.findViewById<TextView>(R.id.tvDescricao).text = subtituloCompleto

        view.findViewById<Button>(R.id.btnVerMais).setOnClickListener {
            val intent = Intent(this, DetalhesActivity::class.java).apply {
                putExtra("NOME", marker.title)
                putExtra("TIPO", subtituloCompleto)
                putExtra("HISTORIA", estacao?.Discricao_hist)
                putExtra("LATITUDE", estacao?.latitude ?: 0.0)
                putExtra("LONGITUDE", estacao?.longitude ?: 0.0)
                putExtra("FOTO_CAPA", estacao?.imagemUrl)
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

        // Mantém a barra aberta
        searchView.setIconifiedByDefault(false)
        searchView.queryHint = "Pesquisar estação..."

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
                // Atualiza o mapa ao vivo à medida que o utilizador digita
                n?.let { filtrarMarcadoresNoMapaLive(it) }
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

    // NOVA FUNÇÃO: Filtra o mapa dinamicamente com base no texto digitado
    private fun filtrarMarcadoresNoMapaLive(query: String) {
        val textoPesquisa = normalizarTexto(query)

        val chipGroup = findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupFiltros)
        val chipIdSelecionado = chipGroup?.checkedChipId ?: R.id.chipTodas

        for (marker in listaMarcadores) {
            val tipoMarcador = marker.snippet ?: ""
            val nomeMarcador = normalizarTexto(marker.title ?: "")

            val correspondeAoTexto = textoPesquisa.isEmpty() || nomeMarcador.contains(textoPesquisa)

            val correspondeAoFiltro = when (chipIdSelecionado) {
                R.id.chipTodas -> true
                R.id.chipEstacoes -> tipoMarcador.contains("Estação", ignoreCase = true)
                R.id.chipApeadeiros -> tipoMarcador.contains("Apeadeiro", ignoreCase = true)
                else -> true
            }

            marker.isVisible = correspondeAoTexto && correspondeAoFiltro
        }
    }
}