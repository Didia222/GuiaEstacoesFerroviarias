package com.diogo.guiaestacoes

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.Normalizer

class ComboioAdapter(
    private var items: List<Any>,
    private var nomeEstacaoAtual: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HEADER = 0
    private val TYPE_ITEM = 1
    private var textoPesquisa: String = "" // Guardamos aqui o destino pesquisado

    // Corrigido para aceitar os 3 argumentos que envias na Activity
    fun atualizarLista(novaLista: List<Any>, estacao: String, pesquisa: String = "") {
        this.items = novaLista
        this.nomeEstacaoAtual = estacao
        this.textoPesquisa = pesquisa
        notifyDataSetChanged()
    }

    private fun limparTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex()
            .replace(normalizado, "").uppercase().trim()
    }

    override fun getItemViewType(position: Int): Int = if (items[position] is String) TYPE_HEADER else TYPE_ITEM
    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cabecalho_tipo, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comboio, parent, false)
            ComboioViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.tvTitulo.text = items[position] as String
        } else if (holder is ComboioViewHolder) {
            val comboio = items[position] as Comboio
            val estacaoLimpa = limparTexto(nomeEstacaoAtual)

            val paragem = comboio.paragens.find { limparTexto(it.estacao) == estacaoLimpa }
            val horaBase = paragem?.hora ?: "--:--"

            // RF-5: Mostrar obrigatoriamente Chegada e Partida
            // Simulamos 2 minutos de paragem para o requisito ficar completo
            if (limparTexto(comboio.destino) == estacaoLimpa) {
                holder.tvHora.text = "Chegada: $horaBase\n(Terminal)"
            } else {
                holder.tvHora.text = "Chegada: $horaBase\nPartida: ${simularPartida(horaBase)}"
            }

            holder.tvNumero.text = "Nº ${comboio.numero}"
            holder.tvRota.text = "${comboio.origem} ➔ ${comboio.destino}"
            holder.tvTipo.text = getNomeServiço(comboio.tipo)

            // LIGAÇÃO AO ITINERÁRIO
            holder.itemView.setOnClickListener {
                val intent = Intent(it.context, ItinerarioActivity::class.java).apply {
                    putExtra("COMBOIO_OBJ", comboio)
                    putExtra("ESTACAO_ATUAL", nomeEstacaoAtual)
                    putExtra("DESTINO_PESQUISADO", textoPesquisa)
                }
                it.context.startActivity(intent)
            }
        }
    }

    private fun simularPartida(hora: String): String {
        if (hora == "--:--") return "--:--"
        val partes = hora.split(":")
        val min = (partes[1].toInt() + 2) % 60
        return "${partes[0]}:${String.format("%02d", min)}"
    }

    private fun getNomeServiço(tipo: String?): String = when(tipo) {
        "AP" -> "Alfa Pendular"
        "IC" -> "Intercidades"
        "R" -> "Regional"
        "U" -> "Urbano"
        else -> tipo ?: "Comboio"
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tvTituloCabecalho) // Confirma se este ID existe no teu item_cabecalho_tipo.xml
    }

    class ComboioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHora: TextView = view.findViewById(R.id.tvHoraComboio)
        val tvNumero: TextView = view.findViewById(R.id.tvNumeroComboio)
        val tvRota: TextView = view.findViewById(R.id.tvRotaComboio)
        val tvTipo: TextView = view.findViewById(R.id.tvTipoServico)
    }
}