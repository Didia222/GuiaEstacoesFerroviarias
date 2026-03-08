package com.diogo.guiaestacoes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ParagemAdapter(private val listaParagens: List<Paragem>) : RecyclerView.Adapter<ParagemAdapter.ParagemViewHolder>() {

    class ParagemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //Estojo que guarda as referências para os textos de cada linha.

        val tvHora: TextView = itemView.findViewById(R.id.tvHoraParagem)
        val tvEstacao: TextView = itemView.findViewById(R.id.tvNomeEstacaoParagem)
    }    //componentes: Ele identifica onde deve escrever a Hora(tvHoraParagem) e o Nome da Estação (tvNomeEstacaoParagem) dentro do ficheiro de desenho item_paragem.xml.




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParagemViewHolder {
        //Carrega o desenho individual de cada paragem
        //Processo: Ele usa o LayoutInglater para "inflar" o layout XML. Podes imaginar isto como o processo de imprimir um formulário em branco que será preenchido logo a seguir.
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_paragem, parent, false)
        return ParagemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParagemViewHolder, position: Int) {
        //Caneta que escreve o formulário
        //Lógica: O código vai à lista, apanha a paragem na posição atual (ex: 1ª paragem, 2ª paragem...) e coloca o texto da hora e o nome da estação nos respetivos campos de texto.

        val paragem = listaParagens[position]
        holder.tvHora.text = paragem.hora
        holder.tvEstacao.text = paragem.estacao
    }

    override fun getItemCount(): Int {
        //O que faz: Informa a lista sobre quantas paragens existem no total para que ela saiba quando deve parar de desenhar.
        return listaParagens.size
    }
}