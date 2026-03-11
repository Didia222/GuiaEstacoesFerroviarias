package com.diogo.guiaestacoes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.MatrixCursor
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.cursoradapter.widget.CursorAdapter
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.Normalizer

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var sugestoesAdapter: SimpleCursorAdapter
    private var todosOsNomesEstacoes: List<String> = emptyList()
    private var listaEstacoesOficiais: List<Estacao> = emptyList()

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val listaMarcadores = mutableListOf<Marker>()

    // DICA: Usei o link /raw/estacoes.csv para que as tuas edições no Gist entrem direto na app!
    // Link "mestre": A app não tem as estações "presas" no código.
    // Ela vai buscar este ficheiro à internet (GitHub) para ter sempre a lista mais atualizada.
    private val URL_DADOS = "https://gist.githubusercontent.com/Didia222/ce7ecbc46a6eebcb912d47c0741eb02f/raw/estacoes.csv"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        configurarPesquisa()

        // GATILHO DE ADMIN: Clique longo na SearchView para limpar o Firebase
        findViewById<SearchView>(R.id.searchViewEstacoes).setOnLongClickListener {
            if (listaEstacoesOficiais.isNotEmpty()) {
                iniciarSaneamentoDeDados(listaEstacoesOficiais)
            } else {
                Toast.makeText(this, "Carrega o mapa primeiro!", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    private fun configurarPesquisa() {
        val searchView = findViewById<SearchView>(R.id.searchViewEstacoes)
        val from = arrayOf("estacaoNome")
        val to = intArrayOf(android.R.id.text1)
        sugestoesAdapter = SimpleCursorAdapter(
            this, android.R.layout.simple_list_item_1, null, from, to,
            CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        )

        searchView.suggestionsAdapter = sugestoesAdapter
        searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean = true
            override fun onSuggestionClick(position: Int): Boolean {
                val cursor = searchView.suggestionsAdapter.getItem(position) as android.database.Cursor
                val nomeSelecionado = cursor.getString(cursor.getColumnIndexOrThrow("estacaoNome"))
                searchView.setQuery(nomeSelecionado, true)
                return true
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { procurarEstacaoNoMapa(it) }
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                filtrarSugestoes(newText)
                return true
            }
        })
    }

    // --- LÓGICA DE NORMALIZAÇÃO PROFISSIONAL ---
    private fun normalizarTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        val semAcentos = "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(normalizado, "")
        return semAcentos.uppercase().replace("[^A-Z0-9]".toRegex(), "").trim()
    }

    private fun iniciarSaneamentoDeDados(estacoesOficiais: List<Estacao>) {
        val db = FirebaseFirestore.getInstance()
        val nomesOficiaisSet = estacoesOficiais.map { normalizarTexto(it.nome) }.toSet()

        Toast.makeText(this, "A iniciar saneamento de dados...", Toast.LENGTH_SHORT).show()

        db.collection("comboios").get().addOnSuccessListener { documents ->
            var comboiosLimpas = 0
            for (document in documents) {
                val comboio = document.toObject(Comboio::class.java)
                val paragensOriginais = comboio.paragens

                // Filtra mantendo apenas o que existe no CSV
                val paragensValidas = paragensOriginais.filter { paragem ->
                    val existe = nomesOficiaisSet.contains(normalizarTexto(paragem.estacao))
                    if (!existe) Log.w("SANEAMENTO", "Removida paragem inválida: ${paragem.estacao}")
                    existe
                }

                if (paragensValidas.size != paragensOriginais.size) {
                    comboiosLimpas++
                    db.collection("comboios").document(document.id).update("paragens", paragensValidas)
                }
            }
            Toast.makeText(this, "Saneamento concluído! $comboiosLimpas comboios corrigidos.", Toast.LENGTH_LONG).show()
        }
    }
    // Gestor de Dados
    // 1º Tenta descarregar a lista nova da Internet
    // 2º Se não houver rede, carrega a ultima versão guardada na base de dados interna (Room)
    private fun carregarDados() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(applicationContext).estacaoDao()
            var listaFinal: List<Estacao> = emptyList()

            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(URL_DADOS).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.byteStream()?.let { stream ->
                            val listaOnline = CsvHelper.carregarDeStream(stream)
                            if (listaOnline.isNotEmpty()) {
                                dao.limparTudo()
                                dao.inserirTodas(listaOnline)
                                listaFinal = listaOnline
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Erro: ${e.message}")
            }

            if (listaFinal.isEmpty()) listaFinal = dao.obterTodas()

            withContext(Dispatchers.Main) {
                listaEstacoesOficiais = listaFinal // Guarda para a auditoria
                atualizarListaDeNomesParaSugestoes(listaFinal)

                if (listaFinal.isNotEmpty()) {
                    map.clear()
                    listaMarcadores.clear()
                    for (estacao in listaFinal) {
                        val marker = map.addMarker(
                            MarkerOptions()
                                .position(LatLng(estacao.latitude, estacao.longitude))
                                .title(estacao.nome)
                                .snippet(estacao.descricao_hist)
                        )
                        if (marker != null) listaMarcadores.add(marker)
                    }
                }
            }
        }
    }

    private fun procurarEstacaoNoMapa(nome: String) {
        val nomeProcurado = normalizarTexto(nome)
        val marcadorEncontrado = listaMarcadores.find {
            normalizarTexto(it.title ?: "") == nomeProcurado ||
                    normalizarTexto(it.title ?: "").contains(nomeProcurado)
        }

        if (marcadorEncontrado != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(marcadorEncontrado.position, 15f))
            marcadorEncontrado.showInfoWindow()
        } else {
            Toast.makeText(this, "Estação não encontrada.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- MÉTODOS OBRIGATÓRIOS DO MAPA E PERMISSÕES ---
    // O "Cenógrafo" do mapa: Assim que o Google Maps acaba de carregar,
    // esta função centra a câmara exatamente em Portugal Continental com o zoom ideal.
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMarkerClickListener(this)
        val centroPortugal = LatLng(39.3999, -8.2245)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(centroPortugal, 7f))
        ativarLocalizacaoUsuario()
        carregarDados()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_detalhes_estacao, null)
        dialog.setContentView(view)

        view.findViewById<TextView>(R.id.tvNomeEstacao).text = marker.title
        view.findViewById<TextView>(R.id.tvDescricao).text = marker.snippet

        view.findViewById<Button>(R.id.btnVerMais).setOnClickListener {
            val intent = Intent(this, DetalhesActivity::class.java).apply {
                putExtra("NOME", marker.title)
                putExtra("DESCRICAO", marker.snippet)
            }
            startActivity(intent)
            dialog.dismiss()
        }

        view.findViewById<Button>(R.id.btnVerHorarios).setOnClickListener {
            val intent = Intent(this, HorariosActivity::class.java).apply {
                putExtra("ESTACAO_NOME", marker.title)
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
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 12f))
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    // O nosso "Tradutor": Remove acentos, maiúsculas, traços e espaços extra.
    private fun filtrarSugestoes(query: String?) {
        val cursor = MatrixCursor(arrayOf(BaseColumns._ID, "estacaoNome"))
        if (!query.isNullOrBlank()) {
            val queryLimpa = normalizarTexto(query)
            val sugestoes = todosOsNomesEstacoes.filter { normalizarTexto(it).contains(queryLimpa) }
            sugestoes.forEachIndexed { index, nome -> cursor.addRow(arrayOf(index, nome)) }
        }
        sugestoesAdapter.changeCursor(cursor)
    }

    private fun atualizarListaDeNomesParaSugestoes(lista: List<Estacao>) {
        todosOsNomesEstacoes = lista.map { it.nome }
    }
}