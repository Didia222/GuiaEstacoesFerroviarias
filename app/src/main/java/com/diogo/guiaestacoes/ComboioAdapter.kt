package com.diogo.guiaestacoes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog

class ComboioAdapter(private var listaComboios: List<Comboio>) : RecyclerView.Adapter<ComboioAdapter.ComboioViewHolder>() {
    //ComboioAdapter: O adapter para uma RecyclerView. A sua função é pegar na lista de comboios que vem do firebase e transformá-la em cartões visuais no ecrã do telemóvel


    class ComboioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {


        //ComboioViewHolder: contentor das referencias visuais de cada cartão
        //txtNumero, txtTipo, txtRota e txtHoraPartida: São as variaveis que ligam o codigo kotlin que definiste no ficheiro xml item_comboio
        val txtNumero: TextView = itemView.findViewById(R.id.txtNumero)
        val txtTipo: TextView = itemView.findViewById(R.id.txtTipo)
        val txtRota: TextView = itemView.findViewById(R.id.txtRota)
        val txtHoraPartida: TextView = itemView.findViewById(R.id.txtHoraPartida)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComboioViewHolder {
        //onCreateViewHolder: infla (carrega) o layout do item_comboio.xml é aqui que o desenho do cartao azul é criado na memória
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comboio, parent, false)

        return ComboioViewHolder(view)
    }

    override fun onBindViewHolder(holder: ComboioViewHolder, position: Int) {
        //onBindViewHolder: Ele preenche o cartão com os dados reais (ex:põe "120" no número, "Porto -> Lisboa" na rota). Também define a hora de partida como sendo a hora da primeira paragem da lista.
        val comboio = listaComboios[position]

        holder.txtNumero.text = comboio.numero
        holder.txtTipo.text = comboio.tipo
        holder.txtRota.text = "${comboio.origem} -> ${comboio.destino}"

        if (comboio.paragens.isNotEmpty()) {
            holder.txtHoraPartida.text = comboio.paragens[0].hora
        } else {
            holder.txtHoraPartida.text = "--:--"
        }

        holder.itemView.setOnClickListener {
            //Define que, ao tocar em qualquer parte do cartão. a função mostrarParagens é chamada para abrir os detalhes.
            mostrarParagens(holder.itemView.context, comboio)
        }
    }

    private fun mostrarParagens(context: android.content.Context, comboio: Comboio) {
        //BottomSheetDialog(context): Cria uma janela com os desliza de baixo para cima

        val dialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.layout_dialog_paragens, null)
        dialog.setContentView(view)

        // 1. Obtemos o nome da estação que o utilizador escolheu no Mapa
        val nomeEstacaoSelecionada = (context as? HorariosActivity)?.intent?.getStringExtra("ESTACAO_NOME")

        // 2. Filtramos a lista: encontramos onde está a estação atual
        val indiceEstacaoAtual = comboio.paragens.indexOfFirst {
            it.estacao.contains(nomeEstacaoSelecionada ?: "", ignoreCase = true)
        }

        // 3. Criamos a sublista (da atual até ao fim). Se não encontrar, mostra tudo.
        val paragensParaMostrar = if (indiceEstacaoAtual != -1) {
            comboio.paragens.subList(indiceEstacaoAtual, comboio.paragens.size)
        } else {
            comboio.paragens
        }

        val tvTitulo = view.findViewById<TextView>(R.id.tvTituloDialog)
        tvTitulo.text = "Próximas paragens: ${comboio.numero}"

        val rvParagens = view.findViewById<RecyclerView>(R.id.recyclerViewParagens)
        rvParagens.layoutManager = LinearLayoutManager(context)

        // IMPORTANTE: Passamos a lista filtrada aqui!
        rvParagens.adapter = ParagemAdapter(paragensParaMostrar)

        dialog.show()
    }

    override fun getItemCount(): Int = listaComboios.size

    fun atualizarLista(novaLista: List<Comboio>) {
        // Atualiza a lista de comboios e notifica o adapter para que seja redesenhado
        listaComboios = novaLista
        notifyDataSetChanged()
    }
}