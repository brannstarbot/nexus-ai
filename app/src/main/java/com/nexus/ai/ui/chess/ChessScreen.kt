package com.nexus.ai.ui.chess

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.ai.di.AppContainer
import com.nexus.ai.domain.chess.GameStatus
import com.nexus.ai.domain.chess.Piece
import com.nexus.ai.domain.chess.PieceType
import com.nexus.ai.domain.chess.Rules
import com.nexus.ai.domain.chess.Side
import com.nexus.ai.domain.chess.fileOf
import com.nexus.ai.domain.chess.rankOf
import com.nexus.ai.domain.chess.squareIndex
import com.nexus.ai.domain.chess.squareName
import com.nexus.ai.ui.theme.BoardCheck
import com.nexus.ai.ui.theme.BoardDark
import com.nexus.ai.ui.theme.BoardLastMove
import com.nexus.ai.ui.theme.BoardLegalDot
import com.nexus.ai.ui.theme.BoardLight
import com.nexus.ai.ui.theme.NexusPrimary
import com.nexus.ai.ui.theme.NexusTextPrimary
import com.nexus.ai.ui.theme.NexusTextSecondary

private val WHITE_GLYPHS = mapOf(
    PieceType.KING to "♔", PieceType.QUEEN to "♕", PieceType.ROOK to "♖",
    PieceType.BISHOP to "♗", PieceType.KNIGHT to "♘", PieceType.PAWN to "♙",
)
private val BLACK_GLYPHS = mapOf(
    PieceType.KING to "♚", PieceType.QUEEN to "♛", PieceType.ROOK to "♜",
    PieceType.BISHOP to "♝", PieceType.KNIGHT to "♞", PieceType.PAWN to "♟",
)

private fun glyph(p: Piece): String =
    if (p.side == Side.WHITE) WHITE_GLYPHS.getValue(p.type) else BLACK_GLYPHS.getValue(p.type)

private fun sideName(side: Side): String = if (side == Side.WHITE) "Putih" else "Hitam"

@Composable
fun ChessScreen(container: AppContainer) {
    val vm: ChessViewModel = viewModel(factory = ChessViewModel.factory(container))
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Catur", style = MaterialTheme.typography.headlineMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.mode == ChessMode.TWO_PLAYER,
                onClick = { vm.setMode(ChessMode.TWO_PLAYER) },
                label = { Text("2 pemain") },
            )
            FilterChip(
                selected = state.mode == ChessMode.VS_AI,
                onClick = { vm.setMode(ChessMode.VS_AI) },
                label = { Text("Lawan NEXUS") },
            )
        }

        Text(
            text = statusText(state),
            style = MaterialTheme.typography.titleMedium,
            color = if (state.game.status == GameStatus.CHECK || state.game.status == GameStatus.CHECKMATE) NexusPrimary else NexusTextPrimary,
        )

        Board(state = state, onSquare = vm::onSquareTapped)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = vm::undo, enabled = !state.aiThinking && state.game.moves.isNotEmpty()) {
                Text("Undo")
            }
            OutlinedButton(onClick = vm::restart) { Text("Ulang") }
            Button(
                onClick = vm::savePgn,
                colors = ButtonDefaults.buttonColors(containerColor = NexusPrimary),
            ) { Text("Simpan PGN") }
        }

        state.message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = NexusTextSecondary)
        }

        if (state.game.sans.isNotEmpty()) {
            Text(
                text = state.game.sans.chunked(2)
                    .withIndex()
                    .joinToString("\n") { (i, pair) -> "${i + 1}. ${pair.joinToString("   ")}" },
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = NexusTextSecondary,
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (state.promotionChoices.isNotEmpty()) {
        val mover = state.game.position.sideToMove
        AlertDialog(
            onDismissRequest = vm::cancelPromotion,
            title = { Text("Pilih bidak promosi") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach { t ->
                        OutlinedButton(onClick = { vm.choosePromotion(t) }) {
                            Text(glyph(Piece(t, mover)), style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }
}

private fun statusText(state: ChessUiState): String {
    val g = state.game
    val turn = sideName(g.position.sideToMove)
    return when (g.status) {
        GameStatus.CHECKMATE -> "Skakmat! ${sideName(g.position.sideToMove.opposite)} menang."
        GameStatus.STALEMATE -> "Remis: stalemate."
        GameStatus.DRAW_FIFTY_MOVES -> "Remis: aturan 50 langkah."
        GameStatus.CHECK -> "Skak! Giliran $turn."
        GameStatus.ONGOING -> if (state.aiThinking) "NEXUS sedang berpikir…" else "Giliran $turn."
    }
}

@Composable
private fun Board(state: ChessUiState, onSquare: (Int) -> Unit) {
    val pos = state.game.position
    val last = state.game.moves.lastOrNull()
    val checkSq = if (state.game.status == GameStatus.CHECK || state.game.status == GameStatus.CHECKMATE) {
        Rules.kingSquare(pos, pos.sideToMove)
    } else {
        null
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(BoardDark),
    ) {
        Column(Modifier.fillMaxSize()) {
            for (rank in 7 downTo 0) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (file in 0..7) {
                        val sq = squareIndex(file, rank)
                        ChessSquare(
                            sq = sq,
                            piece = pos.board[sq],
                            light = (file + rank) % 2 == 1,
                            selected = state.selected == sq,
                            target = sq in state.targets,
                            lastMove = last != null && (sq == last.from || sq == last.to),
                            inCheck = checkSq == sq,
                            onClick = onSquare,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.ChessSquare(
    sq: Int,
    piece: Piece?,
    light: Boolean,
    selected: Boolean,
    target: Boolean,
    lastMove: Boolean,
    inCheck: Boolean,
    onClick: (Int) -> Unit,
) {
    val background = when {
        inCheck -> BoardCheck.copy(alpha = 0.55f)
        selected -> NexusPrimary.copy(alpha = 0.55f)
        lastMove -> BoardLastMove
        light -> BoardLight
        else -> BoardDark
    }
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(background)
            .clickable { onClick(sq) },
        contentAlignment = Alignment.Center,
    ) {
        if (piece != null) {
            Text(
                text = glyph(piece),
                fontSize = 30.sp,
                textAlign = TextAlign.Center,
                color = if (piece.side == Side.WHITE) NexusTextPrimary else Color(0xFF9C98A6),
            )
        }
        if (target) {
            Box(
                Modifier
                    .size(if (piece == null) 12.dp else 34.dp)
                    .clip(CircleShape)
                    .background(BoardLegalDot.copy(alpha = if (piece == null) 1f else 0.35f)),
            )
        }
    }
}
