package com.diogo.guiaestacoes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import android.database.MatrixCursor
import android.provider.BaseColumns
import androidx.cursoradapter.widget.SimpleCursorAdapter


//
class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {


    private lateinit var sugestoesAdapter: androidx.cursoradapter.widget.SimpleCursorAdapter
    private var todosOsNomesEstacoes: List<String> = emptyList()

    //OnMapReadyCallback: Prepara o terrono para o google maps, quando o mapa está pronto, o gerente define o ponto de vista inicial (Portugal) e coloca os marcadores nas posições corretas.
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    // --- NOVIDADE: Lista para guardar os marcadores físicos do mapa ---
    private val listaMarcadores = mutableListOf<Marker>()

    private val URL_DADOS =
        "https://gist.githubusercontent.com/Didia222/ce7ecbc46a6eebcb912d47c0741eb02f/raw/d74e02f3a639db9fa250fb73a249dc99b517743d/estacoes.csv"




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val searchView = findViewById<SearchView>(R.id.searchViewEstacoes)
        val from = arrayOf("estacaoNome")
        val to = intArrayOf(android.R.id.text1)
        sugestoesAdapter = androidx.cursoradapter.widget.SimpleCursorAdapter(
            this,
            android.R.layout.simple_list_item_1,
            null,
            from,
            to,
            androidx.cursoradapter.widget.CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER

        )

        searchView.suggestionsAdapter = sugestoesAdapter
        searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean = true
            override fun onSuggestionClick(position: Int): Boolean {
                val cursor =
                    searchView.suggestionsAdapter.getItem(position) as android.database.Cursor
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

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMarkerClickListener(this)

        val centroPortugal = LatLng(39.3999, -8.2245)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(centroPortugal, 7f))

        ativarLocalizacaoUsuario()
        carregarDados()
        // Função com tres formas de fazer a mesma coisa:
        //Nível 1 (Mercado Online): Tenta descarregar o ficheiro CSV mais recente da Internet (GitHub Gist) usando o OkHttpClient. Se conseguir, renova a base de dados.
        //Nível 2 (Despensa): Se não houver internet, vai buscar o que guardou anteriormente na base de dados Room.
        //Nível 3 (Caixa de Emergência): Se for a primeira vez que a app abre e não houver rede, usa o ficheiro estacoes.csv que está guardado nos Assets.
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_detalhes_estacao, null)
        dialog.setContentView(view)

        val nomeEstacao = marker.title ?: ""
        view.findViewById<TextView>(R.id.tvNomeEstacao).text = nomeEstacao
        view.findViewById<TextView>(R.id.tvDescricao).text = marker.snippet

        view.findViewById<Button>(R.id.btnVerMais).setOnClickListener {
            val intent = Intent(this, DetalhesActivity::class.java)
            intent.putExtra("NOME", nomeEstacao)
            intent.putExtra("DESCRICAO", marker.snippet)
            startActivity(intent)
            dialog.dismiss()
        }

        view.findViewById<Button>(R.id.btnVerHorarios).setOnClickListener {
            val intent = Intent(this, HorariosActivity::class.java)
            intent.putExtra("ESTACAO_NOME", nomeEstacao)
            startActivity(intent)
            dialog.dismiss()
        }

        dialog.show()
        return true
    }

    private fun carregarDados() {
        lifecycleScope.launch(Dispatchers.IO) {
            //O gerente não faz o trabalho pesado sozinho para não "congelar"(bloquear o ecrã). Ele contrata trabalhadores para carregar os dados em segundo plano.
            //withContext(Dispatchers.Main): Quando os dados chegam, o gerente volta à linha da frente para desenhar os marcadores no mapa, pois só ele pode mexer na interface visual.

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
            if (listaFinal.isEmpty()) {
                listaFinal = CsvHelper.carregarEstacoesDoCsv(applicationContext)
                if (listaFinal.isNotEmpty()) dao.inserirTodas(listaFinal)
            }

            withContext(Dispatchers.Main) {
                atualizarListaDeNomesParaSugestoes(listaFinal)
                if (listaFinal.isNotEmpty()) {
                    map.clear()
                    listaMarcadores.clear() // Limpa a lista de marcadores antiga

                    for (estacao in listaFinal) {
                        val marker = map.addMarker(
                            MarkerOptions()
                                .position(LatLng(estacao.latitude, estacao.longitude))
                                .title(estacao.nome)
                                .snippet(estacao.descricao_hist)
                        )
                        // Guarda o marcador na nossa lista para podermos pesquisá-lo depois
                        if (marker != null) listaMarcadores.add(marker)
                    }
                }
            }
        }
    }

    // --- PESQUISA ATUALIZADA: Move o mapa E abre o marcador automaticamente ---
    private fun procurarEstacaoNoMapa(nome: String) {
        // Procura na nossa lista de marcadores físicos
        val marcadorEncontrado = listaMarcadores.find {
            it.title?.contains(nome, ignoreCase = true) == true
        }

        if (marcadorEncontrado != null) {
            // 1. Zoom na estação
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(marcadorEncontrado.position, 15f))

            // 2. ABRE O BALÃO (InfoWindow) AUTOMATICAMENTE!
            marcadorEncontrado.showInfoWindow()

        } else {
            Toast.makeText(this, "Estação '$nome' não encontrada.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun ativarLocalizacaoUsuario() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userPos = LatLng(location.latitude, location.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(userPos, 12f))
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ativarLocalizacaoUsuario()
        }
    }

    private fun filtrarSugestoes(query: String?) {
        val cursor =
            android.database.MatrixCursor(arrayOf(android.provider.BaseColumns._ID, "estacaoNome"))

        if (!query.isNullOrBlank()) {
            val sugestoes = todosOsNomesEstacoes.filter {
                it.contains(query, ignoreCase = true)
            }
            sugestoes.forEachIndexed { index, nome ->
                cursor.addRow(arrayOf(index, nome))
            }
        }
        sugestoesAdapter.changeCursor(cursor)
    }


    private fun atualizarListaDeNomesParaSugestoes(lista: List<Estacao>) {
        todosOsNomesEstacoes = lista.map { it.nome }
    }
}




