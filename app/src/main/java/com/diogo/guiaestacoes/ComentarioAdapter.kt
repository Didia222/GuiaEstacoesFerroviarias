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

// [UI/UX & PERFOMANCE - RNF-2 e RNF-9]
// O ComentarioAdapter implementa o padrão Separação de Responsabilidades.
// A sua única função é receber a lista de comentários puxada do Firebase e renderizá-la de forma eficiente
// no RecyclerView, reciclando as Views que saem do ecrã para poupar Memória.
class ComentarioAdapter(private var lista: List<Comentario>) :
    RecyclerView.Adapter<ComentarioAdapter.ComentarioViewHolder>() {

        // [PADRÂO VIEWHOLDER]
        // Faz cache das referências visuais para evitar chamar o 'findViewById' repetidamente
        // durante o scroll.
    class ComentarioViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvAutor: TextView = v.findViewById(R.id.tvAutorItem)
        val tvData: TextView = v.findViewById(R.id.tvDataHoraItem)
        val tvTexto: TextView = v.findViewById(R.id.tvTextoItem)
        val rbEstrelas: RatingBar = v.findViewById(R.id.rbEstrelasItem)
        val ivFoto: ImageView = v.findViewById(R.id.ivFotoItem)
    }

    // Função utilizada para atualizar a lista de comentários quando houver alterações em tempo real.

    fun atualizar(novaLista: List<Comentario>) {
        this.lista = novaLista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComentarioViewHolder {
        // "Infla" (constrói na memória) o ficheiro XML que desenha o "cartão" de um único comentário
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_comentario, parent, false)
        return ComentarioViewHolder(v)
    }

    // [LÓGICA DE BINDING e TRATAMENTO DE DADOS]
    override fun onBindViewHolder(holder: ComentarioViewHolder, position: Int) {
        val c = lista[position]
        // 1. Preenchimento de textl e avaliação
        holder.tvAutor.text = c.autor
        holder.tvTexto.text = c.texto
        holder.rbEstrelas.rating = c.estrelas

        // 2. Formatação da data e hora
        // O Firebase fornece o Timestamp para uma leitura legível para o utilizador.

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.tvData.text = sdf.format(Date(c.timestamp))

        // 3. Renderizaçãoo Assíncrona de Imagens (Gestão de Memória)
        if (!c.url_foto.isNullOrEmpty()) {
            holder.ivFoto.visibility = View.VISIBLE
            // Descarrega a imagem em background
            //Gera a cache (memória e disco) automaticamente, evitando OutOfMemoryErrors.
            Glide.with(holder.itemView.context).load(c.url_foto).into(holder.ivFoto)
        } else {
            // Segurança de Reciclagem: Se o comentário não tiver foto, temos de esconder o ImageView.
            holder.ivFoto.visibility = View.GONE
        }
    }

    override fun getItemCount() = lista.size
}