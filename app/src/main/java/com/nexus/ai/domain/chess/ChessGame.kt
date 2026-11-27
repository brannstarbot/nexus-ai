package com.nexus.ai.domain.chess

/**
 * Riwayat permainan. positions[i] adalah posisi sebelum langkah ke-i;
 * positions.last() adalah posisi saat ini.
 */
data class ChessGame(
    val positions: List<Position> = listOf(Position.initial()),
    val moves: List<Move> = emptyList(),
    val sans: List<String> = emptyList(),
    /** Pemain sudah mengklaim remis (berlaku hanya jika klaim memang tersedia). */
    val claimedDraw: Boolean = false,
) {
    val position: Position get() = positions.last()

    /** Berapa kali posisi saat ini muncul dalam riwayat (termasuk yang sekarang). */
    val repetitionCount: Int
        get() {
            val key = Rules.repetitionKey(position)
            return positions.count { Rules.repetitionKey(it) == key }
        }

    val status: GameStatus
        get() {
            val base = Rules.status(position)
            if (base == GameStatus.CHECKMATE || base == GameStatus.STALEMATE) return base
            if (base == GameStatus.DRAW_INSUFFICIENT || base == GameStatus.DRAW_75_MOVES) return base
            if (repetitionCount >= 5) return GameStatus.DRAW_REPETITION
            if (claimedDraw) return GameStatus.DRAW_CLAIMED
            return base
        }

    /** Remis bisa diklaim: posisi terulang 3 kali, atau sudah 50 langkah tanpa gerakan pion/tangkapan. */
    val canClaimDraw: Boolean
        get() = !status.isOver() && (repetitionCount >= 3 || position.halfmoveClock >= 100)

    fun play(move: Move): ChessGame {
        val san = Rules.toSan(position, move)
        return copy(
            positions = positions + Rules.apply(position, move),
            moves = moves + move,
            sans = sans + san,
            claimedDraw = false,
        )
    }

    fun claimDraw(): ChessGame = if (canClaimDraw) copy(claimedDraw = true) else this

    fun undo(): ChessGame {
        if (moves.isEmpty()) return this
        return copy(
            positions = positions.dropLast(1),
            moves = moves.dropLast(1),
            sans = sans.dropLast(1),
            claimedDraw = false,
        )
    }

    /** Hasil dalam format PGN: "1-0", "0-1", "1/2-1/2", atau "*" jika belum selesai. */
    val result: String
        get() = when (status) {
            GameStatus.CHECKMATE -> if (position.sideToMove == Side.WHITE) "0-1" else "1-0"
            GameStatus.STALEMATE, GameStatus.DRAW_CLAIMED, GameStatus.DRAW_REPETITION,
            GameStatus.DRAW_75_MOVES, GameStatus.DRAW_INSUFFICIENT -> "1/2-1/2"
            else -> "*"
        }

    fun pgn(headers: Map<String, String>): String = Rules.toPgn(sans, result, headers)
}
