package com.nexus.ai.domain.chess

/** Riwayat permainan. positions[i] adalah posisi sebelum langkah ke-i; positions.last() adalah posisi saat ini. */
data class ChessGame(
    val positions: List<Position> = listOf(Position.initial()),
    val moves: List<Move> = emptyList(),
    val sans: List<String> = emptyList(),
) {
    val position: Position get() = positions.last()

    val status: GameStatus get() = Rules.status(position)

    fun play(move: Move): ChessGame {
        val san = Rules.toSan(position, move)
        return copy(
            positions = positions + Rules.apply(position, move),
            moves = moves + move,
            sans = sans + san,
        )
    }

    fun undo(): ChessGame {
        if (moves.isEmpty()) return this
        return copy(
            positions = positions.dropLast(1),
            moves = moves.dropLast(1),
            sans = sans.dropLast(1),
        )
    }

    /** Hasil dalam format PGN: "1-0", "0-1", "1/2-1/2", atau "*" jika belum selesai. */
    val result: String
        get() = when (status) {
            GameStatus.CHECKMATE -> if (position.sideToMove == Side.WHITE) "0-1" else "1-0"
            GameStatus.STALEMATE, GameStatus.DRAW_FIFTY_MOVES -> "1/2-1/2"
            else -> "*"
        }

    fun pgn(headers: Map<String, String>): String = Rules.toPgn(sans, result, headers)
}
