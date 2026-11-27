package com.nexus.ai.domain.chess

import kotlin.math.abs

enum class GameStatus { ONGOING, CHECK, CHECKMATE, STALEMATE, DRAW_FIFTY_MOVES }

fun GameStatus.isOver(): Boolean =
    this == GameStatus.CHECKMATE || this == GameStatus.STALEMATE || this == GameStatus.DRAW_FIFTY_MOVES

/** Aturan catur lengkap: langkah legal, skak, skakmate, stalemate, rokade, en passant, promosi. */
object Rules {

    private val KNIGHT_STEPS = listOf(1 to 2, 2 to 1, 2 to -1, 1 to -2, -1 to -2, -2 to -1, -2 to 1, -1 to 2)
    private val KING_STEPS = listOf(1 to 0, 1 to 1, 0 to 1, -1 to 1, -1 to 0, -1 to -1, 0 to -1, 1 to -1)
    private val ROOK_DIRS = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
    private val BISHOP_DIRS = listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)
    private val PROMOTION_PIECES = listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)

    private fun onBoard(f: Int, r: Int): Boolean = f in 0..7 && r in 0..7

    fun legalMoves(pos: Position): List<Move> =
        pseudoLegalMoves(pos).filter { !isInCheck(apply(pos, it), pos.sideToMove) }

    fun legalMovesFrom(pos: Position, from: Int): List<Move> = legalMoves(pos).filter { it.from == from }

    fun kingSquare(pos: Position, side: Side): Int? =
        (0 until 64).firstOrNull { sq ->
            val p = pos.board[sq]
            p != null && p.type == PieceType.KING && p.side == side
        }

    fun isInCheck(pos: Position, side: Side): Boolean {
        val king = kingSquare(pos, side) ?: return false
        return isAttacked(pos, king, side.opposite)
    }

    fun status(pos: Position): GameStatus {
        val inCheck = isInCheck(pos, pos.sideToMove)
        if (legalMoves(pos).isEmpty()) {
            return if (inCheck) GameStatus.CHECKMATE else GameStatus.STALEMATE
        }
        if (pos.halfmoveClock >= 100) return GameStatus.DRAW_FIFTY_MOVES
        return if (inCheck) GameStatus.CHECK else GameStatus.ONGOING
    }

    fun pseudoLegalMoves(pos: Position): List<Move> {
        val out = ArrayList<Move>(48)
        val us = pos.sideToMove
        for (sq in 0 until 64) {
            val p = pos.board[sq] ?: continue
            if (p.side != us) continue
            when (p.type) {
                PieceType.PAWN -> pawnMoves(pos, sq, us, out)
                PieceType.KNIGHT -> steps(pos, sq, us, KNIGHT_STEPS, out)
                PieceType.BISHOP -> slides(pos, sq, us, BISHOP_DIRS, out)
                PieceType.ROOK -> slides(pos, sq, us, ROOK_DIRS, out)
                PieceType.QUEEN -> slides(pos, sq, us, ROOK_DIRS + BISHOP_DIRS, out)
                PieceType.KING -> {
                    steps(pos, sq, us, KING_STEPS, out)
                    castles(pos, us, out)
                }
            }
        }
        return out
    }

    private fun steps(pos: Position, sq: Int, us: Side, deltas: List<Pair<Int, Int>>, out: MutableList<Move>) {
        val f = fileOf(sq)
        val r = rankOf(sq)
        for ((df, dr) in deltas) {
            val nf = f + df
            val nr = r + dr
            if (!onBoard(nf, nr)) continue
            val t = squareIndex(nf, nr)
            val occ = pos.board[t]
            if (occ == null || occ.side != us) out.add(Move(sq, t))
        }
    }

    private fun slides(pos: Position, sq: Int, us: Side, dirs: List<Pair<Int, Int>>, out: MutableList<Move>) {
        val f = fileOf(sq)
        val r = rankOf(sq)
        for ((df, dr) in dirs) {
            var nf = f + df
            var nr = r + dr
            while (onBoard(nf, nr)) {
                val t = squareIndex(nf, nr)
                val occ = pos.board[t]
                if (occ == null) {
                    out.add(Move(sq, t))
                } else {
                    if (occ.side != us) out.add(Move(sq, t))
                    break
                }
                nf += df
                nr += dr
            }
        }
    }

    private fun pawnMoves(pos: Position, sq: Int, us: Side, out: MutableList<Move>) {
        val f = fileOf(sq)
        val r = rankOf(sq)
        val dir = if (us == Side.WHITE) 1 else -1
        val startRank = if (us == Side.WHITE) 1 else 6
        val promoRank = if (us == Side.WHITE) 7 else 0
        val oneR = r + dir

        if (onBoard(f, oneR)) {
            val one = squareIndex(f, oneR)
            if (pos.board[one] == null) {
                addPawnMove(sq, one, promoRank, out)
                if (r == startRank) {
                    val two = squareIndex(f, r + 2 * dir)
                    if (pos.board[two] == null) out.add(Move(sq, two))
                }
            }
        }

        for (df in listOf(-1, 1)) {
            val nf = f + df
            if (!onBoard(nf, oneR)) continue
            val t = squareIndex(nf, oneR)
            val occ = pos.board[t]
            if (occ != null && occ.side != us) {
                addPawnMove(sq, t, promoRank, out)
            } else if (occ == null && pos.enPassantTarget == t) {
                out.add(Move(sq, t, isEnPassant = true))
            }
        }
    }

    private fun addPawnMove(from: Int, to: Int, promoRank: Int, out: MutableList<Move>) {
        if (rankOf(to) == promoRank) {
            PROMOTION_PIECES.forEach { out.add(Move(from, to, promotion = it)) }
        } else {
            out.add(Move(from, to))
        }
    }

    private fun castles(pos: Position, us: Side, out: MutableList<Move>) {
        val rank = if (us == Side.WHITE) 0 else 7
        val kingSq = squareIndex(4, rank)
        val king = pos.board[kingSq]
        if (king == null || king.type != PieceType.KING || king.side != us) return
        if (isInCheck(pos, us)) return

        val opp = us.opposite
        val rights = pos.castling
        val kingSideRight = if (us == Side.WHITE) rights.whiteKingSide else rights.blackKingSide
        val queenSideRight = if (us == Side.WHITE) rights.whiteQueenSide else rights.blackQueenSide

        if (kingSideRight &&
            isRook(pos, squareIndex(7, rank), us) &&
            isEmpty(pos, 5, rank) && isEmpty(pos, 6, rank) &&
            !isAttacked(pos, squareIndex(5, rank), opp) &&
            !isAttacked(pos, squareIndex(6, rank), opp)
        ) {
            out.add(Move(kingSq, squareIndex(6, rank), isCastle = true))
        }

        if (queenSideRight &&
            isRook(pos, squareIndex(0, rank), us) &&
            isEmpty(pos, 1, rank) && isEmpty(pos, 2, rank) && isEmpty(pos, 3, rank) &&
            !isAttacked(pos, squareIndex(3, rank), opp) &&
            !isAttacked(pos, squareIndex(2, rank), opp)
        ) {
            out.add(Move(kingSq, squareIndex(2, rank), isCastle = true))
        }
    }

    private fun isEmpty(pos: Position, f: Int, r: Int): Boolean = pos.board[squareIndex(f, r)] == null

    private fun isRook(pos: Position, sq: Int, us: Side): Boolean {
        val p = pos.board[sq] ?: return false
        return p.type == PieceType.ROOK && p.side == us
    }

    /** Apakah kotak [sq] diserang oleh bidak warna [by]. */
    fun isAttacked(pos: Position, sq: Int, by: Side): Boolean {
        val f = fileOf(sq)
        val r = rankOf(sq)

        // Pion penyerang berada satu baris di belakang sq, dari sudut pandang pihak penyerang.
        val pawnRank = if (by == Side.WHITE) r - 1 else r + 1
        for (df in listOf(-1, 1)) {
            val nf = f + df
            if (!onBoard(nf, pawnRank)) continue
            val p = pos.board[squareIndex(nf, pawnRank)]
            if (p != null && p.type == PieceType.PAWN && p.side == by) return true
        }

        for ((df, dr) in KNIGHT_STEPS) {
            val nf = f + df
            val nr = r + dr
            if (!onBoard(nf, nr)) continue
            val p = pos.board[squareIndex(nf, nr)]
            if (p != null && p.type == PieceType.KNIGHT && p.side == by) return true
        }

        for ((df, dr) in KING_STEPS) {
            val nf = f + df
            val nr = r + dr
            if (!onBoard(nf, nr)) continue
            val p = pos.board[squareIndex(nf, nr)]
            if (p != null && p.type == PieceType.KING && p.side == by) return true
        }

        for ((df, dr) in ROOK_DIRS) {
            val p = firstPieceOnRay(pos, f, r, df, dr) ?: continue
            if (p.side == by && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) return true
        }

        for ((df, dr) in BISHOP_DIRS) {
            val p = firstPieceOnRay(pos, f, r, df, dr) ?: continue
            if (p.side == by && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) return true
        }

        return false
    }

    private fun firstPieceOnRay(pos: Position, f: Int, r: Int, df: Int, dr: Int): Piece? {
        var nf = f + df
        var nr = r + dr
        while (onBoard(nf, nr)) {
            val p = pos.board[squareIndex(nf, nr)]
            if (p != null) return p
            nf += df
            nr += dr
        }
        return null
    }

    /** Menerapkan langkah yang sudah diverifikasi legal dan mengembalikan posisi baru. */
    fun apply(pos: Position, m: Move): Position {
        val b = pos.board.toMutableList()
        val mover = b[m.from] ?: error("Tidak ada bidak di ${squareName(m.from)}")
        val captured = b[m.to]
        b[m.from] = null

        if (m.isEnPassant) {
            b[squareIndex(fileOf(m.to), rankOf(m.from))] = null
        }

        if (m.isCastle) {
            val rank = rankOf(m.from)
            if (fileOf(m.to) == 6) {
                b[squareIndex(5, rank)] = b[squareIndex(7, rank)]
                b[squareIndex(7, rank)] = null
            } else {
                b[squareIndex(3, rank)] = b[squareIndex(0, rank)]
                b[squareIndex(0, rank)] = null
            }
        }

        b[m.to] = m.promotion?.let { Piece(it, mover.side) } ?: mover

        var c = pos.castling
        if (mover.type == PieceType.KING) {
            c = if (mover.side == Side.WHITE) {
                c.copy(whiteKingSide = false, whiteQueenSide = false)
            } else {
                c.copy(blackKingSide = false, blackQueenSide = false)
            }
        }
        fun revoke(sq: Int) {
            c = when (sq) {
                0 -> c.copy(whiteQueenSide = false)
                7 -> c.copy(whiteKingSide = false)
                56 -> c.copy(blackQueenSide = false)
                63 -> c.copy(blackKingSide = false)
                else -> c
            }
        }
        revoke(m.from)
        revoke(m.to)

        val ep = if (mover.type == PieceType.PAWN && abs(rankOf(m.to) - rankOf(m.from)) == 2) {
            squareIndex(fileOf(m.from), (rankOf(m.from) + rankOf(m.to)) / 2)
        } else {
            null
        }

        val halfmove = if (mover.type == PieceType.PAWN || captured != null || m.isEnPassant) {
            0
        } else {
            pos.halfmoveClock + 1
        }
        val full = if (pos.sideToMove == Side.BLACK) pos.fullmoveNumber + 1 else pos.fullmoveNumber

        return Position(b, pos.sideToMove.opposite, c, ep, halfmove, full)
    }

    /** Notasi SAN standar, termasuk disambiguasi, promosi, dan tanda + atau #. */
    fun toSan(pos: Position, m: Move): String {
        val mover = pos.board[m.from] ?: return m.uci()
        val after = apply(pos, m)
        val suffix = when (status(after)) {
            GameStatus.CHECKMATE -> "#"
            GameStatus.CHECK -> "+"
            else -> ""
        }

        if (m.isCastle) {
            return (if (fileOf(m.to) == 6) "O-O" else "O-O-O") + suffix
        }

        val capture = pos.board[m.to] != null || m.isEnPassant
        val body = if (mover.type == PieceType.PAWN) {
            val prefix = if (capture) "${'a' + fileOf(m.from)}x" else ""
            val promo = m.promotion?.let { "=${it.symbol}" } ?: ""
            prefix + squareName(m.to) + promo
        } else {
            val others = legalMoves(pos).filter { other ->
                other.from != m.from && other.to == m.to &&
                    pos.board[other.from]?.let { it.type == mover.type && it.side == mover.side } == true
            }
            val disamb = when {
                others.isEmpty() -> ""
                others.none { fileOf(it.from) == fileOf(m.from) } -> "${'a' + fileOf(m.from)}"
                others.none { rankOf(it.from) == rankOf(m.from) } -> "${'1' + rankOf(m.from)}"
                else -> squareName(m.from)
            }
            "${mover.type.symbol}$disamb${if (capture) "x" else ""}${squareName(m.to)}"
        }
        return body + suffix
    }

    fun toPgn(sans: List<String>, result: String, headers: Map<String, String>): String {
        val sb = StringBuilder()
        headers.forEach { (k, v) -> sb.append("[$k \"$v\"]\n") }
        sb.append("[Result \"$result\"]\n\n")
        sans.forEachIndexed { i, san ->
            if (i % 2 == 0) sb.append("${i / 2 + 1}. ")
            sb.append(san).append(' ')
        }
        sb.append(result)
        return sb.toString()
    }
}
