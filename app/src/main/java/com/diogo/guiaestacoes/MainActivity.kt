package com.diogo.guiaestacoes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.location.Location // NOVA IMPORTAÇÃO
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import java.text.Normalizer
import kotlin.math.* // NOVA IMPORTAÇÃO PARA A FÓRMULA MATEMÁTICA

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var sugestoesAdapter: SimpleCursorAdapter
    private var todosOsNomesEstacoes: List<String> = emptyList()
    private var listaEstacoesOficiais: List<Estacao> = emptyList()

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val listaMarcadores = mutableListOf<Marker>()
    private lateinit var db: FirebaseFirestore

    // Variável para guardar o GPS do telemóvel
    private var localizacaoAtual: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        verificarRetornoDeDetalhes(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        verificarRetornoDeDetalhes(intent)
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

    // --- NOVA FUNÇÃO: FÓRMULA DE HAVERSINE ---
    // Calcula a distância real em linha reta entre dois pontos na Terra (em km)
    private fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Raio da Terra em KM
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
                val listaMapaTemp = mutableListOf<Estacao>() // Para o mapa (filtrado)
                val listaPesquisaTemp = mutableListOf<String>() // Para a pesquisa (todas)

                map.clear()
                listaMarcadores.clear()

                for (document in snapshots) {
                    val estacao = document.toObject(Estacao::class.java)

                    // 1. Guardamos SEMPRE o nome para a pesquisa
                    listaPesquisaTemp.add(estacao.nome)

                    // 2. Lógica do Mapa (Pins vermelhos) - Só se tiver coordenadas
                    if (estacao.latitude != 0.0) {
                        var dentroDoRaio = true

                        if (localizacaoAtual != null) {
                            val distanciaKm = calcularDistancia(
                                localizacaoAtual!!.latitude, localizacaoAtual!!.longitude,
                                estacao.latitude, estacao.longitude
                            )
                            if (distanciaKm > 10.0) {
                                dentroDoRaio = false
                            }
                        }

                        if (dentroDoRaio) {
                            listaMapaTemp.add(estacao)

                            val textoParaAnalise = (estacao.nome + " " + estacao.Discricao_hist).lowercase()
                            val tipoDetectado = when {
                                textoParaAnalise.contains("apeadeiro") -> "Apeadeiro"
                                textoParaAnalise.contains("paragem") || textoParaAnalise.contains("halte") -> "Apeadeiro"
                                else -> "Estação Ferroviária"
                            }

                            val marker = map.addMarker(MarkerOptions()
                                .position(LatLng(estacao.latitude, estacao.longitude))
                                .title(estacao.nome)
                                .snippet(tipoDetectado))

                            marker?.tag = estacao
                            if (marker != null) listaMarcadores.add(marker)
                        }
                    }
                }

                // ATUALIZAÇÃO DAS LISTAS
                listaEstacoesOficiais = snapshots.toObjects(Estacao::class.java) // Guardamos todas as estações
                todosOsNomesEstacoes = listaPesquisaTemp // A pesquisa agora tem TUDO!
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMarkerClickListener(this)
        ativarLocalizacaoUsuario()
        // O carregarDados() agora é chamado por dentro do ativarLocalizacaoUsuario()
        // para garantirmos que só carrega as estações depois de ter o teu GPS!
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val estacao = marker.tag as? Estacao
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_detalhes_estacao, null)
        dialog.setContentView(view)

        view.findViewById<TextView>(R.id.tvNomeEstacao).text = marker.title
        view.findViewById<TextView>(R.id.tvDescricao).text = marker.snippet

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
            val intent = Intent(this, GaleriaActivity::class.java).apply {
                putExtra("NOME", marker.title)
            }
            startActivity(intent)
            dialog.dismiss()
        }

        view.findViewById<Button>(R.id.btnAvaliar).setOnClickListener {
            val intent = Intent(this, AvaliacaoActivity::class.java).apply {
                putExtra("NOME", marker.title)
            }
            startActivity(intent)
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
                    // Guardamos a tua localização!
                    localizacaoAtual = it
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 12f))
                }
                // Só depois de descobrir onde estás é que disparamos o download e o filtro
                carregarDados()
            }.addOnFailureListener {
                carregarDados() // Se o GPS falhar, carrega tudo sem filtro
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            carregarDados() // Se não deste permissão, carrega tudo sem filtro
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
            override fun onSuggestionSelect(position: Int): Boolean {
                return false
            }

            override fun onSuggestionClick(position: Int): Boolean {
                val cursor = sugestoesAdapter.cursor as Cursor
                if (cursor.moveToPosition(position)) {
                    val nomeEstacaoClicada = cursor.getString(cursor.getColumnIndexOrThrow("estacaoNome"))
                    searchView.setQuery(nomeEstacaoClicada, false)
                    procurarEstacaoNoMapa(nomeEstacaoClicada)
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
            todosOsNomesEstacoes.filter { normalizarTexto(it).contains(q) }.forEachIndexed { i, nome -> cursor.addRow(arrayOf(i, nome)) }
        }
        sugestoesAdapter.changeCursor(cursor)
    }

    private fun procurarEstacaoNoMapa(nome: String) {
        val q = normalizarTexto(nome)
        listaMarcadores.find { normalizarTexto(it.title ?: "").contains(q) }?.let {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(it.position, 15f))
            it.showInfoWindow()
        }
    }
}