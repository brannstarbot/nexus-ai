package com.nexus.ai.domain.chess

import org.junit.Assert.assertEquals
import org.junit.Test

class PerftTest {

    /** Parser FEN minimal untuk tes. */
    private fun fromFen(fen: String): Position {
        val parts = fen.split(" ")
        val board = MutableList<Piece?>(64) { null }
        val rows = parts[0].split("/")
        for ((i, row) in rows.withIndex()) {
            val rank = 7 - i
            var file = 0
            for (ch in row) {
                if (ch.isDigit()) {
                    file += ch.digitToInt()
                } else {
                    val side = if (ch.isUpperCase()) Side.WHITE else Side.BLACK
                    val type = when (ch.lowercaseChar()) {
                        'p' -> PieceType.PAWN
                        'n' -> PieceType.KNIGHT
                        'b' -> PieceType.BISHOP
                        'r' -> PieceType.ROOK
                        'q' -> PieceType.QUEEN
                        else -> PieceType.KING
                    }
                    board[squareIndex(file, rank)] = Piece(type, side)
                    file++
                }
            }
        }
        val side = if (parts[1] == "w") Side.WHITE else Side.BLACK
        val c = parts[2]
        val castling = CastlingRights(
            whiteKingSide = c.contains('K'),
            whiteQueenSide = c.contains('Q'),
            blackKingSide = c.contains('k'),
            blackQueenSide = c.contains('q'),
        )
        val ep = if (parts[3] == "-") null else squareFromName(parts[3])
        return Position(board, side, castling, ep, parts[4].toInt(), parts[5].toInt())
    }

    private fun squareFromName(name: String): Int = squareIndex(name[0] - 'a', name[1] - '1')

    private fun perft(pos: Position, depth: Int): Long {
        if (depth == 0) return 1
        var nodes = 0L
        for (m in Rules.legalMoves(pos)) nodes += perft(Rules.apply(pos, m), depth - 1)
        return nodes
    }

    @Test
    fun startingPosition() {
        val start = Position.initial()
        assertEquals(20L, perft(start, 1))
        assertEquals(400L, perft(start, 2))
        assertEquals(8902L, perft(start, 3))
    }

    @Test
    fun kiwipete_castlingEnPassantPromotion() {
        val pos = fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1")
        assertEquals(48L, perft(pos, 1))
        assertEquals(2039L, perft(pos, 2))
    }

    @Test
    fun positionThree_enPassantAndPins() {
        val pos = fromFen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1")
        assertEquals(14L, perft(pos, 1))
        assertEquals(191L, perft(pos, 2))
        assertEquals(2812L, perft(pos, 3))
    }
}
