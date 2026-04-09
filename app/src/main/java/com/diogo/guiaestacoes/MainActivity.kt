package com.diogo.guiaestacoes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
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

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var sugestoesAdapter: SimpleCursorAdapter
    private var todosOsNomesEstacoes: List<String> = emptyList()
    private var listaEstacoesOficiais: List<Estacao> = emptyList()

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val listaMarcadores = mutableListOf<Marker>()
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Firebase com Cache Offline
        db = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        db.firestoreSettings = settings

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        configurarPesquisa()

        // Verifica se viemos da DetalhesActivity para focar numa estação
        verificarRetornoDeDetalhes(intent)
    }

    // Lida com o retorno da DetalhesActivity sem reiniciar a App
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
            // Espera o mapa carregar se necessário e faz zoom
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(localizacao, 16f))
            listaMarcadores.find { it.position == localizacao }?.showInfoWindow()
        }
    }

    private fun carregarDados() {
        db.collection("Estacao").addSnapshotListener { snapshots, error ->
            if (error != null) return@addSnapshotListener

            if (snapshots != null) {
                val listaTemp = mutableListOf<Estacao>()
                map.clear()
                listaMarcadores.clear()

                for (document in snapshots) {
                    val estacao = document.toObject(Estacao::class.java)
                    if (estacao.latitude != 0.0) {
                        listaTemp.add(estacao)

                        val textoParaAnalise = (estacao.nome + " " + estacao.Discricao_hist).lowercase()

                        val tipoDetectado = when {
                            textoParaAnalise.contains("apeadeiro") -> "Apeadeiro"
                            textoParaAnalise.contains("paragem") || textoParaAnalise.contains("halte") -> "Apeadeiro"
                            else -> "Estação Ferroviária"
                        }

                        val marker = map.addMarker(MarkerOptions().position(LatLng(estacao.latitude, estacao.longitude)).title(estacao.nome).snippet(tipoDetectado))
                        marker?.tag = estacao
                        if (marker != null) listaMarcadores.add(marker)
                    }
                }
                listaEstacoesOficiais = listaTemp
                todosOsNomesEstacoes = listaTemp.map { it.nome }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMarkerClickListener(this)
        ativarLocalizacaoUsuario()
        carregarDados()
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
            // CORREÇÃO: Removido o startActivity(intent) duplicado que causava erro
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

    // --- Auxiliares GPS e Pesquisa ---

    private fun ativarLocalizacaoUsuario() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let { map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 12f)) }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
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
                searchView.clearFocus() // Esconde o teclado
                return true
            }
            override fun onQueryTextChange(n: String?): Boolean {
                filtrarSugestoes(n)
                return true
            }
        })

        // A GRANDE CORREÇÃO ESTÁ AQUI: O Listener para os cliques nas sugestões!
        searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                return false
            }

            override fun onSuggestionClick(position: Int): Boolean {
                val cursor = sugestoesAdapter.cursor as Cursor
                if (cursor.moveToPosition(position)) {
                    // Pega o nome da estação em que o utilizador clicou
                    val nomeEstacaoClicada = cursor.getString(cursor.getColumnIndexOrThrow("estacaoNome"))

                    // Coloca o nome na barra e faz a pesquisa no mapa
                    searchView.setQuery(nomeEstacaoClicada, false)
                    procurarEstacaoNoMapa(nomeEstacaoClicada)

                    // Esconde o teclado após o clique
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
            // Nível de zoom definido para 15f (suficiente para ver o marcador e a rua)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(it.position, 15f))
            it.showInfoWindow()
        }
    }
}