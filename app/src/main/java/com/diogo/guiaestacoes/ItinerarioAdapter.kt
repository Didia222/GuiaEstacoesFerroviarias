package com.diogo.guiaestacoes

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ItinerarioAdapter(
    private val paragens: List<Paragem>,
    private val estacaoAtual: String
) : RecyclerView.Adapter<ItinerarioAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_itinerario, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val paragem = paragens[position]
        holder.tvEstacao.text = paragem.estacao

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