package com.diogo.guiaestacoes

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.Normalizer

class ComboioAdapter(
    private var lista: List<Any>,
    private var estacaoSelecionada: String,
    private var destinoPesquisado: String = ""
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HEADER = 0
    private val TYPE_ITEM = 1

    // FUNÇÃO DE LIMPEZA: Remove acentos, traços e espaços a mais para comparar nomes facilmente
    private fun limparTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        val semAcentos = "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(normalizado, "")
        return semAcentos.replace("-", " ").replace("\\s+".toRegex(), " ").trim()
    }

    // Verifica se é Cabeçalho ou Item de Comboio
    override fun getItemViewType(position: Int): Int = if (lista[position] is String) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_cabecalho_tipo, parent, false)
            HeaderViewHolder(v)
        } else {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_comboio, parent, false)
            ComboioViewHolder(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = lista[position]
        if (holder is HeaderViewHolder) {
            holder.tvTitulo.text = item as String
        } else if (holder is ComboioViewHolder) {
            val comboio = item as Comboio
            holder.txtNumero.text = comboio.numero
            holder.txtTipo.text = comboio.tipo

            val estacaoMapaLimpa = limparTexto(estacaoSelecionada)
            val destinoPesquisadoLimpo = limparTexto(destinoPesquisado)

            // 1. Em que estação estamos nós? (Descobrir o índice da estação de partida na lista do comboio)
            val indexOrigem = comboio.paragens.indexOfFirst {
                val nomeLimpo = limparTexto(it.estacao)
                nomeLimpo.contains(estacaoMapaLimpa, true) || estacaoMapaLimpa.contains(nomeLimpo, true)
            }

            // 2. Destino por defeito (vai buscar o destino final oficial do JSON)
            var destinoAExibir = comboio.destino

            // Se por acaso o destino vier vazio do Firebase, usamos a última paragem da lista como plano B
            if (destinoAExibir.isNullOrEmpty()) {
                destinoAExibir = if (comboio.paragens.isNotEmpty()) comboio.paragens.last().estacao else "Desconhecido"
            }

            // 3. A MAGIA: Se o utilizador escreveu um destino na barra de pesquisa, mudamos o texto do cartão para essa estação!
            if (destinoPesquisadoLimpo.isNotEmpty() && indexOrigem != -1) {
                val paragemDeSaida = comboio.paragens.withIndex().find { (index, paragem) ->
                    val nomeParagem = limparTexto(paragem.estacao)
                    // Tem de ser uma paragem DEPOIS da nossa origem, e que corresponda à pesquisa
                    index > indexOrigem && (nomeParagem.contains(destinoPesquisadoLimpo, true) || destinoPesquisadoLimpo.contains(nomeParagem, true))
                }

                // Encontrou a paragem onde a pessoa quer sair? Atualiza o texto!
                if (paragemDeSaida != null) {
                    destinoAExibir = paragemDeSaida.value.estacao
                }
            }

            // Escrevemos o destino certo no ecrã
            holder.txtRota.text = destinoAExibir

            // 4. Escrevemos a hora de partida da nossa estação
            val paragemAqui = comboio.paragens.getOrNull(indexOrigem)
            holder.txtHoraPartida.text = paragemAqui?.hora ?: "--:--"

            // 5. Clicar no cartão para abrir o Itinerário
            holder.itemView.setOnClickListener {
                val intent = Intent(it.context, ItinerarioActivity::class.java)
                intent.putExtra("COMBOIO_OBJ", comboio)
                intent.putExtra("ESTACAO_ATUAL", estacaoSelecionada)
                intent.putExtra("DESTINO_PESQUISADO", destinoPesquisado)
                it.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = lista.size

    fun atualizarLista(novaLista: List<Any>, estacao: String, destino: String = "") {
        this.lista = novaLista
        this.estacaoSelecionada = estacao
        this.destinoPesquisado = destino
        notifyDataSetChanged()
    }

    class HeaderViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitulo: TextView = v.findViewById(R.id.tvTituloTipo)
    }

    class ComboioViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val txtNumero: TextView = v.findViewById(R.id.txtNumero)
        val txtTipo: TextView = v.findViewById(R.id.txtTipo)
        val txtRota: TextView = v.findViewById(R.id.txtRota) // Caixa de texto do Destino
        val txtHoraPartida: TextView = v.findViewById(R.id.txtHoraPartida)
    }
}