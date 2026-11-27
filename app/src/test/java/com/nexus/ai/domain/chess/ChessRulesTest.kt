package com.nexus.ai.domain.chess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessRulesTest {

    private fun sq(name: String): Int = squareIndex(name[0] - 'a', name[1] - '1')
    private fun mv(from: String, to: String): Move = Move(sq(from), sq(to))

    /** Ulangi bolak-balik kuda: g1-f3, g8-f6, f3-g1, f6-g8 (satu siklus = 4 langkah). */
    private fun knightCycle(game: ChessGame): ChessGame {
        var g = game
        g = g.play(mv("g1", "f3"))
        g = g.play(mv("g8", "f6"))
        g = g.play(mv("f3", "g1"))
        g = g.play(mv("f6", "g8"))
        return g
    }

    @Test
    fun threefoldRepetition_isClaimable_notAutomatic() {
        var g = ChessGame()
        g = knightCycle(g)
        g = knightCycle(g)
        // Posisi awal sudah muncul 3 kali.
        assertEquals(3, g.repetitionCount)
        assertTrue(g.canClaimDraw)
        assertFalse(g.status.isOver())
        val claimed = g.claimDraw()
        assertEquals(GameStatus.DRAW_CLAIMED, claimed.status)
        assertEquals("1/2-1/2", claimed.result)
    }

    @Test
    fun fivefoldRepetition_isAutomatic() {
        var g = ChessGame()
        repeat(4) { g = knightCycle(g) }
        assertEquals(5, g.repetitionCount)
        assertEquals(GameStatus.DRAW_REPETITION, g.status)
        assertEquals("1/2-1/2", g.result)
    }

    @Test
    fun insufficientMaterial_kingVersusKing_isDraw() {
        val fen = "8/8/4k3/8/8/8/4K3/8 w - - 0 1"
        val pos = fenPosition(fen)
        assertEquals(GameStatus.DRAW_INSUFFICIENT, Rules.status(pos))
    }

    @Test
    fun insufficientMaterial_singleKnight_isDraw_butTwoKnightsIsNot() {
        assertTrue(Rules.insufficientMaterial(fenPosition("8/8/4k3/8/8/3N4/4K3/8 w - - 0 1")))
        assertFalse(Rules.insufficientMaterial(fenPosition("8/8/4k3/8/8/2NN4/4K3/8 w - - 0 1")))
    }

    @Test
    fun insufficientMaterial_sameColorBishops_isDraw_oppositeColorIsNot() {
        // Kedua gajah di kotak yang sama warnanya: c2 dan d3 keduanya (file+rank) ganjil.
        assertTrue(Rules.insufficientMaterial(fenPosition("8/8/4k3/8/8/3B4/2B1K3/8 w - - 0 1")))
        // Gajah di kotak berbeda warna: skakmat masih mungkin.
        // f4 (gelap) dan c2 (terang): berbeda warna, skakmat masih mungkin.
        assertFalse(Rules.insufficientMaterial(fenPosition("8/8/4k3/8/5B2/8/2b1K3/8 w - - 0 1")))
    }

    @Test
    fun seventyFiveMoveRule_isAutomatic_atHalfmove150() {
        val pos = fenPosition("8/8/4k3/8/8/3N4/4K3/8 w - - 150 80")
        // Materi tidak cukup dicek lebih dulu, jadi pakai posisi dengan materi cukup.
        val pos2 = fenPosition("8/8/4k3/8/8/3R4/4K3/8 w - - 150 80")
        assertEquals(GameStatus.DRAW_75_MOVES, Rules.status(pos2))
        assertTrue(pos.halfmoveClock == 150)
    }

    @Test
    fun checkmate_takesPrecedence_overSeventyFiveMoveRule() {
        // Hitam di h8 diskak Qg7 yang dilindungi Kg6: skakmat, dengan halfmove 150.
        val pos = fenPosition("7k/6Q1/6K1/8/8/8/8/8 b - - 150 80")
        assertEquals(GameStatus.CHECKMATE, Rules.status(pos))
    }

    /** Parser FEN minimal untuk tes. */
    private fun fenPosition(fen: String): Position {
        val parts = fen.split(" ")
        val board = MutableList<Piece?>(64) { null }
        for ((i, row) in parts[0].split("/").withIndex()) {
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
        val c = parts[2]
        return Position(
            board = board,
            sideToMove = if (parts[1] == "w") Side.WHITE else Side.BLACK,
            castling = CastlingRights(c.contains('K'), c.contains('Q'), c.contains('k'), c.contains('q')),
            enPassantTarget = if (parts[3] == "-") null else sq(parts[3]),
            halfmoveClock = parts[4].toInt(),
            fullmoveNumber = parts[5].toInt(),
        )
    }
}
