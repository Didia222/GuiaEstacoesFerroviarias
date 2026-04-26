package com.diogo.guiaestacoes

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

    fun atualizarLista(novaLista: List<Any>, estacao: String) {
        this.items = novaLista
        this.nomeEstacaoAtual = estacao
        notifyDataSetChanged()
    }

    private fun limparTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex()
            .replace(normalizado, "").uppercase().trim()
    }

    override fun getItemViewType(position: Int): Int = if (items[position] is String) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
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

            // Procura a paragem específica desta estação
            val paragem = comboio.paragens.find { limparTexto(it.estacao) == estacaoLimpa }

            holder.tvHora.text = paragem?.hora ?: "--:--"
            holder.tvNumero.text = "Nº ${comboio.numero}"
            holder.tvRota.text = "${comboio.origem} ➔ ${comboio.destino}"

            holder.tvTipo.text = when(comboio.tipo) {
                "AP" -> "Alfa Pendular"
                "IC" -> "Intercidades"
                "R" -> "Regional"
                "U" -> "Urbano"
                else -> comboio.tipo
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(android.R.id.text1)
    }

    class ComboioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHora: TextView = view.findViewById(R.id.tvHoraComboio)
        val tvNumero: TextView = view.findViewById(R.id.tvNumeroComboio)
        val tvRota: TextView = view.findViewById(R.id.tvRotaComboio)
        val tvTipo: TextView = view.findViewById(R.id.tvTipoServico)
    }
}