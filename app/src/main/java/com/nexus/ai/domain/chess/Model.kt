package com.nexus.ai.domain.chess

enum class Side {
    WHITE, BLACK;

    val opposite: Side get() = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(val symbol: Char) {
    PAWN('P'), KNIGHT('N'), BISHOP('B'), ROOK('R'), QUEEN('Q'), KING('K')
}

data class Piece(val type: PieceType, val side: Side)

/** Indeks kotak 0..63: rank * 8 + file. a1 = 0, h1 = 7, a8 = 56, h8 = 63. */
fun squareIndex(file: Int, rank: Int): Int = rank * 8 + file
fun fileOf(sq: Int): Int = sq % 8
fun rankOf(sq: Int): Int = sq / 8
fun squareName(sq: Int): String = "${'a' + fileOf(sq)}${'1' + rankOf(sq)}"

data class Move(
    val from: Int,
    val to: Int,
    val promotion: PieceType? = null,
    val isCastle: Boolean = false,
    val isEnPassant: Boolean = false,
)

data class CastlingRights(
    val whiteKingSide: Boolean = true,
    val whiteQueenSide: Boolean = true,
    val blackKingSide: Boolean = true,
    val blackQueenSide: Boolean = true,
)

data class Position(
    val board: List<Piece?>,
    val sideToMove: Side,
    val castling: CastlingRights,
    /** Kotak yang bisa ditangkap en passant pada giliran ini, atau null. */
    val enPassantTarget: Int?,
    val halfmoveClock: Int,
    val fullmoveNumber: Int,
) {
    companion object {
        fun initial(): Position {
            val b = MutableList<Piece?>(64) { null }
            val back = listOf(
                PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
                PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK,
            )
            for (f in 0..7) {
                b[squareIndex(f, 0)] = Piece(back[f], Side.WHITE)
                b[squareIndex(f, 1)] = Piece(PieceType.PAWN, Side.WHITE)
                b[squareIndex(f, 6)] = Piece(PieceType.PAWN, Side.BLACK)
                b[squareIndex(f, 7)] = Piece(back[f], Side.BLACK)
            }
            return Position(b, Side.WHITE, CastlingRights(), null, 0, 1)
        }
    }
}

/** Notasi UCI, misalnya "e2e4" atau "e7e8q". Dipakai sebagai cadangan jika SAN tidak tersedia. */
fun Move.uci(): String =
    squareName(from) + squareName(to) + (promotion?.symbol?.lowercaseChar()?.toString() ?: "")
