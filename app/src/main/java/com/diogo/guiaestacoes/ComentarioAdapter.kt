package com.diogo.guiaestacoes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class ComentarioAdapter(private var lista: List<Comentario>) :
    RecyclerView.Adapter<ComentarioAdapter.ComentarioViewHolder>() {

    class ComentarioViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvAutor: TextView = v.findViewById(R.id.tvAutorItem)
        val tvData: TextView = v.findViewById(R.id.tvDataHoraItem)
        val tvTexto: TextView = v.findViewById(R.id.tvTextoItem)
        val rbEstrelas: RatingBar = v.findViewById(R.id.rbEstrelasItem)
        val ivFoto: ImageView = v.findViewById(R.id.ivFotoItem)
    }

    fun atualizar(novaLista: List<Comentario>) {
        this.lista = novaLista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComentarioViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_comentario, parent, false)
        return ComentarioViewHolder(v)
    }

    override fun onBindViewHolder(holder: ComentarioViewHolder, position: Int) {
        val c = lista[position]
        holder.tvAutor.text = c.autor
        holder.tvTexto.text = c.texto
        holder.rbEstrelas.rating = c.estrelas

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.tvData.text = sdf.format(Date(c.timestamp))

        if (!c.url_foto.isNullOrEmpty()) {
            holder.ivFoto.visibility = View.VISIBLE
            Glide.with(holder.itemView.context).load(c.url_foto).into(holder.ivFoto)
        } else {
            holder.ivFoto.visibility = View.GONE
        }
    }

    override fun getItemCount() = lista.size
}