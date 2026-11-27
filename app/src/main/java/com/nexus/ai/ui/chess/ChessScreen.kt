package com.nexus.ai.ui.chess

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.ai.di.AppContainer
import com.nexus.ai.domain.chess.GameStatus
import com.nexus.ai.domain.chess.Piece
import com.nexus.ai.domain.chess.PieceType
import com.nexus.ai.domain.chess.Rules
import com.nexus.ai.domain.chess.Side
import com.nexus.ai.domain.chess.squareIndex
import com.nexus.ai.ui.theme.BoardCheck
import com.nexus.ai.ui.theme.BoardDark
import com.nexus.ai.ui.theme.BoardLastMove
import com.nexus.ai.ui.theme.BoardLegalDot
import com.nexus.ai.ui.theme.BoardLight
import com.nexus.ai.ui.theme.NexusAccent
import com.nexus.ai.ui.theme.NexusBackground
import com.nexus.ai.ui.theme.NexusPrimary
import com.nexus.ai.ui.theme.NexusSurface
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

    // Satu sumber animasi bersama: denyut 0..1 untuk glow, highlight, dan titik langkah legal.
    val infinite = rememberInfiniteTransition(label = "chess-pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val shimmer by infinite.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer",
    )

    val status = state.game.status
    val inCheck = status == GameStatus.CHECK || status == GameStatus.CHECKMATE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0B0E), NexusBackground, Color(0xFF191521))))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Catur",
            style = MaterialTheme.typography.headlineMedium.copy(
                brush = Brush.linearGradient(
                    colors = listOf(NexusTextPrimary, NexusAccent, NexusTextPrimary),
                    start = Offset(shimmer, 0f),
                    end = Offset(shimmer + 260f, 0f),
                ),
            ),
        )

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

        StatusPill(text = statusText(state), alarm = inCheck, pulse = pulse)

        Board(state = state, pulse = pulse, onSquare = vm::onSquareTapped)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = vm::undo,
                enabled = !state.aiThinking && state.game.moves.isNotEmpty(),
            ) { Text("Undo") }
            OutlinedButton(onClick = vm::restart) { Text("Ulang") }
            if (state.game.canClaimDraw) {
                OutlinedButton(onClick = vm::claimDraw) { Text("Klaim remis") }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = vm::savePgn,
                colors = ButtonDefaults.buttonColors(containerColor = NexusPrimary),
            ) { Text("Simpan PGN") }
        }

        state.message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = NexusTextSecondary)
        }

        if (state.game.sans.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = NexusSurface.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = state.game.sans.chunked(2)
                        .withIndex()
                        .joinToString("\n") { (i, pair) -> "${i + 1}. ${pair.joinToString("   ")}" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = NexusTextSecondary,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (state.promotionChoices.isNotEmpty()) {
        val mover = state.game.position.sideToMove
        AlertDialog(
            onDismissRequest = vm::cancelPromotion,
            containerColor = NexusSurface,
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

@Composable
private fun StatusPill(text: String, alarm: Boolean, pulse: Float) {
    val container by animateColorAsState(
        targetValue = if (alarm) NexusPrimary.copy(alpha = 0.22f) else NexusSurface,
        animationSpec = tween(400),
        label = "pill",
    )
    Surface(
        shape = CircleShape,
        color = container,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(NexusPrimary.copy(alpha = 0.3f + 0.6f * pulse), NexusAccent.copy(alpha = 0.2f)),
            ),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = if (alarm) NexusAccent else NexusTextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

private fun statusText(state: ChessUiState): String {
    val g = state.game
    val turn = sideName(g.position.sideToMove)
    return when (g.status) {
        GameStatus.CHECKMATE -> "Skakmat! ${sideName(g.position.sideToMove.opposite)} menang"
        GameStatus.STALEMATE -> "Remis: stalemate"
        GameStatus.DRAW_CLAIMED -> "Remis: diklaim"
        GameStatus.DRAW_REPETITION -> "Remis: posisi terulang 5 kali"
        GameStatus.DRAW_75_MOVES -> "Remis: aturan 75 langkah"
        GameStatus.DRAW_INSUFFICIENT -> "Remis: materi tidak cukup"
        GameStatus.CHECK -> "Skak! Giliran $turn"
        GameStatus.ONGOING -> when {
            state.aiThinking -> "NEXUS sedang berpikir…"
            g.canClaimDraw -> "Giliran $turn · remis bisa diklaim"
            else -> "Giliran $turn"
        }
    }
}

@Composable
private fun Board(state: ChessUiState, pulse: Float, onSquare: (Int) -> Unit) {
    val pos = state.game.position
    val last = state.game.moves.lastOrNull()
    val checkSq = if (state.game.status == GameStatus.CHECK || state.game.status == GameStatus.CHECKMATE) {
        Rules.kingSquare(pos, pos.sideToMove)
    } else {
        null
    }
    val frame = RoundedCornerShape(20.dp)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(
                elevation = (10 + 12 * pulse).dp,
                shape = frame,
                ambientColor = NexusPrimary,
                spotColor = NexusPrimary,
            )
            .clip(frame)
            .background(BoardDark)
            .border(
                1.dp,
                Brush.linearGradient(listOf(NexusPrimary.copy(alpha = 0.3f + 0.5f * pulse), NexusAccent.copy(alpha = 0.15f))),
                frame,
            ),
    ) {
        Column(Modifier.fillMaxSize()) {
            for (rank in 7 downTo 0) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (file in 0..7) {
                        val sq = squareIndex(file, rank)
                        ChessSquare(
                            piece = pos.board[sq],
                            sq = sq,
                            light = (file + rank) % 2 == 1,
                            selected = state.selected == sq,
                            target = sq in state.targets,
                            lastMove = last != null && (sq == last.from || sq == last.to),
                            inCheck = checkSq == sq,
                            pulse = pulse,
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
    piece: Piece?,
    sq: Int,
    light: Boolean,
    selected: Boolean,
    target: Boolean,
    lastMove: Boolean,
    inCheck: Boolean,
    pulse: Float,
    onClick: (Int) -> Unit,
) {
    val base = if (light) BoardLight else BoardDark
    val background = when {
        inCheck -> BoardCheck.copy(alpha = 0.45f + 0.4f * pulse)
        selected -> NexusPrimary.copy(alpha = 0.3f + 0.4f * pulse)
        lastMove -> BoardLastMove
        else -> base
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(background)
            .clickable { onClick(sq) },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = piece,
            transitionSpec = {
                (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.7f))
                    .togetherWith(fadeOut(tween(120)))
            },
            label = "piece-$sq",
        ) { p ->
            if (p != null) {
                val white = p.side == Side.WHITE
                Text(
                    text = glyph(p),
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center,
                    color = if (white) NexusTextPrimary else Color(0xFF8E8A99),
                    style = TextStyle(
                        shadow = Shadow(
                            color = if (white) NexusAccent.copy(alpha = 0.35f + 0.25f * pulse) else NexusPrimary.copy(alpha = 0.3f),
                            blurRadius = 18f,
                        ),
                    ),
                )
            }
        }

        if (target) {
            Box(
                Modifier
                    .size(if (piece == null) (10 + 4 * pulse).dp else 40.dp)
                    .clip(CircleShape)
                    .background(
                        BoardLegalDot.copy(
                            alpha = if (piece == null) 0.55f + 0.45f * pulse else 0.2f + 0.25f * pulse,
                        ),
                    ),
            )
        }
    }
}
