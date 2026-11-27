package com.nexus.ai.domain.chess

/**
 * Lawan lokal sederhana: minimax dengan alpha-beta dan evaluasi materi.
 * Ini BUKAN Stockfish. Kekuatannya terbatas dan dimaksudkan sebagai lawan dasar.
 */
object SimpleAi {

    private const val INF = 1_000_000
    private const val MATE = 100_000

    private fun value(t: PieceType): Int = when (t) {
        PieceType.PAWN -> 100
        PieceType.KNIGHT -> 320
        PieceType.BISHOP -> 330
        PieceType.ROOK -> 500
        PieceType.QUEEN -> 900
        PieceType.KING -> 0
    }

    /** Skor dari sudut pandang pihak yang sedang jalan. */
    fun evaluate(pos: Position): Int {
        var score = 0
        for (sq in 0 until 64) {
            val p = pos.board[sq] ?: continue
            val v = value(p.type)
            score += if (p.side == Side.WHITE) v else -v
        }
        return if (pos.sideToMove == Side.WHITE) score else -score
    }

    fun bestMove(pos: Position, depth: Int = 2): Move? {
        val moves = Rules.legalMoves(pos).shuffled()
        var best: Move? = null
        var bestScore = -INF
        for (m in moves) {
            val score = -negamax(Rules.apply(pos, m), depth - 1, -INF, INF)
            if (score > bestScore) {
                bestScore = score
                best = m
            }
        }
        return best
    }

    private fun negamax(pos: Position, depth: Int, alpha: Int, beta: Int): Int {
        val moves = Rules.legalMoves(pos)
        if (moves.isEmpty()) {
            return if (Rules.isInCheck(pos, pos.sideToMove)) -MATE else 0
        }
        if (depth <= 0) return evaluate(pos)

        var best = -INF
        var a = alpha
        for (m in moves) {
            val score = -negamax(Rules.apply(pos, m), depth - 1, -beta, -a)
            if (score > best) best = score
            if (best > a) a = best
            if (a >= beta) break
        }
        return best
    }
}
