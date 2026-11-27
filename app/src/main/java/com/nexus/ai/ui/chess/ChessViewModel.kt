package com.nexus.ai.ui.chess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nexus.ai.di.AppContainer
import com.nexus.ai.domain.chess.ChessGame
import com.nexus.ai.domain.chess.GameStatus
import com.nexus.ai.domain.chess.Move
import com.nexus.ai.domain.chess.PieceType
import com.nexus.ai.domain.chess.Rules
import com.nexus.ai.domain.chess.Side
import com.nexus.ai.domain.chess.SimpleAi
import com.nexus.ai.domain.chess.isOver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

enum class ChessMode { TWO_PLAYER, VS_AI }

data class ChessUiState(
    val game: ChessGame = ChessGame(),
    val mode: ChessMode = ChessMode.TWO_PLAYER,
    val selected: Int? = null,
    val targets: Set<Int> = emptySet(),
    /** Berisi langkah-langkah promosi yang menunggu pilihan bidak; kosong jika tidak ada. */
    val promotionChoices: List<Move> = emptyList(),
    val aiThinking: Boolean = false,
    val message: String? = null,
)

class ChessViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(ChessUiState())
    val state: StateFlow<ChessUiState> = _state.asStateFlow()

    /** Pemain manusia selalu Putih dalam mode lawan AI. */
    private val humanSide = Side.WHITE

    fun onSquareTapped(sq: Int) {
        val s = _state.value
        if (s.aiThinking || s.promotionChoices.isNotEmpty() || s.game.status.isOver()) return
        val pos = s.game.position
        if (s.mode == ChessMode.VS_AI && pos.sideToMove != humanSide) return

        val selected = s.selected
        if (selected != null && sq in s.targets) {
            val options = Rules.legalMovesFrom(pos, selected).filter { it.to == sq }
            if (options.size > 1) {
                _state.update { it.copy(promotionChoices = options) }
            } else {
                playMove(options.first())
            }
            return
        }

        val piece = pos.board[sq]
        if (piece != null && piece.side == pos.sideToMove) {
            val targets = Rules.legalMovesFrom(pos, sq).map { it.to }.toSet()
            _state.update { it.copy(selected = sq, targets = targets, message = null) }
        } else {
            _state.update { it.copy(selected = null, targets = emptySet()) }
        }
    }

    fun choosePromotion(piece: PieceType) {
        val move = _state.value.promotionChoices.firstOrNull { it.promotion == piece } ?: return
        _state.update { it.copy(promotionChoices = emptyList()) }
        playMove(move)
    }

    fun cancelPromotion() {
        _state.update { it.copy(promotionChoices = emptyList(), selected = null, targets = emptySet()) }
    }

    private fun playMove(move: Move) {
        _state.update {
            it.copy(
                game = it.game.play(move),
                selected = null,
                targets = emptySet(),
                message = null,
            )
        }
        maybeStartAiMove()
    }

    private fun maybeStartAiMove() {
        val s = _state.value
        if (s.mode != ChessMode.VS_AI || s.game.status.isOver() || s.game.position.sideToMove == humanSide) return

        viewModelScope.launch {
            val pos = _state.value.game.position
            _state.update { it.copy(aiThinking = true) }
            val move = withContext(Dispatchers.Default) { SimpleAi.bestMove(pos, depth = 2) }
            _state.update { current ->
                // Jika pengguna sudah restart atau undo saat AI berpikir, abaikan hasilnya.
                if (move != null && current.game.position == pos) {
                    current.copy(game = current.game.play(move), aiThinking = false)
                } else {
                    current.copy(aiThinking = false)
                }
            }
        }
    }

    fun undo() {
        val s = _state.value
        if (s.aiThinking || s.game.moves.isEmpty()) return
        val steps = if (s.mode == ChessMode.VS_AI) 2 else 1
        var game = s.game
        repeat(steps) { game = game.undo() }
        _state.update {
            it.copy(game = game, selected = null, targets = emptySet(), promotionChoices = emptyList(), message = null)
        }
    }

    fun claimDraw() {
        _state.update { it.copy(game = it.game.claimDraw(), message = null) }
    }

    fun restart() {
        _state.update { ChessUiState(mode = it.mode) }
    }

    fun setMode(mode: ChessMode) {
        _state.update { ChessUiState(mode = mode) }
    }

    fun savePgn() {
        val game = _state.value.game
        if (game.moves.isEmpty()) {
            _state.update { it.copy(message = "Belum ada langkah untuk disimpan.") }
            return
        }
        val mode = _state.value.mode
        val opponent = if (mode == ChessMode.VS_AI) "NEXUS AI" else "Pemain 2"
        val headers = mapOf(
            "Event" to "NEXUS AI",
            "Date" to LocalDate.now().toString().replace('-', '.'),
            "White" to if (mode == ChessMode.VS_AI) "Pemain" else "Pemain 1",
            "Black" to opponent,
        )
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val dir = File(container.appContext.filesDir, "pgn").apply { mkdirs() }
                val file = File(dir, "game-${System.currentTimeMillis()}.pgn")
                file.writeText(game.pgn(headers))
                file.absolutePath
            }
            _state.update { it.copy(message = "PGN disimpan di: $result") }
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { ChessViewModel(container) }
        }
    }
}
