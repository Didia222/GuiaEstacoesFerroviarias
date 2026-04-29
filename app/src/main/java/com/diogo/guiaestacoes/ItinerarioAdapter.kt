package com.diogo.guiaestacoes

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ItinerarioAdapter(
    // O adapter recebe a lista filtrada pela Activity Itinerario
    private val paragens: List<Paragem>,
    // O Adaptador não precisa saber a origem real do comboio, apenas processa
    // a "Timeline" visual da posição 0 até à última.
    private val estacaoAtual: String
) : RecyclerView.Adapter<ItinerarioAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_itinerario, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val paragem = paragens[position]
        holder.tvEstacao.text = paragem.estacao

        // Como o Activity já cortou a lista das estações anteriores e futuras
        // a posição visual 0 é sempre a estação de partida e a posição final é garantida
        // ser a de chegada. As outras são pontos de passagem (só hora).
        when (position) {
            0 -> {
                holder.tvHora.text = "Partida\n${paragem.hora}"
            }
            paragens.size - 1 -> {
                // A última estação da lista (cortada pela Activity) recebe o rótulo de Chegada
                holder.tvHora.text = "Chegada\n${paragem.hora}"
            }
            else -> {
                holder.tvHora.text = paragem.hora
            }
        }
        // Estilização (UI): Destaca a verde e a negrito a estação onde o utilizador se encontra.
        if (paragem.estacao.equals(estacaoAtual, ignoreCase = true)) {
            holder.tvEstacao.text = "${paragem.estacao} (Atual)"
            holder.tvEstacao.setTextColor(Color.parseColor("#00502F"))
            holder.tvEstacao.paint.isFakeBoldText = true
        } else {
            holder.tvEstacao.setTextColor(Color.parseColor("#212121"))
            holder.tvEstacao.paint.isFakeBoldText = false
        }
    }

    override fun getItemCount(): Int = paragens.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEstacao: TextView = view.findViewById(R.id.txtEstacaoItinerario)
        val tvHora: TextView = view.findViewById(R.id.txtHoraItinerario)
    }
}