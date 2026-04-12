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

class ComentarioAdapter(private val lista: List<Comentario>) :
    RecyclerView.Adapter<ComentarioAdapter.ComentarioViewHolder>() {

    class ComentarioViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvAutor: TextView = v.findViewById(R.id.tvAutorItem)
        val tvData: TextView = v.findViewById(R.id.tvDataHoraItem)
        val tvTexto: TextView = v.findViewById(R.id.tvTextoItem)
        val rbEstrelas: RatingBar = v.findViewById(R.id.rbEstrelasItem)
        val ivFoto: ImageView = v.findViewById(R.id.ivFotoItem)
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

        // Formatar a data (ex: 12/04/2026 15:30)
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.tvData.text = sdf.format(Date(c.timestamp))

        // --- LÓGICA DA FOTO ---
        if (!c.url_foto.isNullOrEmpty()) {
            holder.ivFoto.visibility = View.VISIBLE

            Glide.with(holder.itemView.context)
                .load(c.url_foto)
                .placeholder(R.drawable.ic_launcher_background) // Imagem enquanto carrega
                .error(android.R.drawable.stat_notify_error)   // Se houver erro
                .into(holder.ivFoto)
        } else {
            // Se não houver foto, escondemos a ImageView para não ficar um buraco vazio
            holder.ivFoto.visibility = View.GONE
        }
    }

    override fun getItemCount() = lista.size
}