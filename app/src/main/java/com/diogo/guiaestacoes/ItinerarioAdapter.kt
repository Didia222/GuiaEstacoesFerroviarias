package com.diogo.guiaestacoes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ItinerarioAdapter(private val paragens: List<Paragem>) :
    RecyclerView.Adapter<ItinerarioAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val txtEstacao: TextView = v.findViewById(R.id.txtEstacaoItinerario)
        val txtHora: TextView = v.findViewById(R.id.txtHoraItinerario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_itinerario, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = paragens[position]
        holder.txtEstacao.text = p.estacao
        holder.txtHora.text = p.hora
    }

    override fun getItemCount() = paragens.size
}