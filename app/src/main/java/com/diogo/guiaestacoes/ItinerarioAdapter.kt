package com.diogo.guiaestacoes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ItinerarioAdapter(private val paragens: List<Paragem>) :
    RecyclerView.Adapter<ItinerarioAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtEstacao: TextView = view.findViewById(R.id.txtEstacaoItinerario)
        // 1. Agarramos na caixa de texto da hora
        val txtHora: TextView = view.findViewById(R.id.txtHoraItinerario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_itinerario, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val paragem = paragens[position]
        holder.txtEstacao.text = paragem.estacao

        // 2. Preenchemos a caixa com a hora que vem do Firebase
        holder.txtHora.text = paragem.hora
    }

    override fun getItemCount() = paragens.size
}