package com.diogo.guiaestacoes
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
//Diz ao compilador do Room que este ficheiro contém as regras de acesso aos dados. O android vai ler as anotações e gerar automaticamente
// o código necessário para a base de dados
interface EstacaoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    //Este é um seguro para garantir que se existir dados que tenham o mesmo id no telemovel e na base de dados a informação é atualizada de acordo com a base de dados online.
    suspend fun inserirTodas(estacoes: List<Estacao>)
    // Indicação que a função usa Coroutines. Como operações de base de dados podem demorar milissegundos preciosos
    //o kotlin obriga estas funções a correrem em "segundo plano" para não bloquear o ecrã do utilizador.
    // consultas personalizadas
    @Query("SELECT * FROM estacao_table")
    suspend fun obterTodas(): List<Estacao>
    //Pede á base de dados para listar todas as estações guardadas.
    @Query("DELETE FROM estacao_table")
    //Limpeza da tabela para quando carregar novos dados da base de dados online o telemóvel não fica com lixo inutilizado.
    suspend fun limparTudo()
}