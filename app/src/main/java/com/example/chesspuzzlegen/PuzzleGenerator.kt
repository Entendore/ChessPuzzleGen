package com.example.chesspuzzlegen

import chesslib.Board
import chesslib.Move
import chesslib.Piece
import chesslib.PieceType
import chesslib.Side
import chesslib.Square
import chesslib.move.MoveList
import kotlin.random.Random

class PuzzleGenerator {

    fun generatePuzzle(difficulty: Difficulty, type: PuzzleType): Puzzle {
        return when (type) {
            PuzzleType.GRECO_MATE -> generateGrecoMatePuzzle()
            PuzzleType.CLEARANCE_SAC -> generateClearanceSacPuzzle()
            PuzzleType.UNDERPROMOTION -> generateUnderpromotionPuzzle()
            PuzzleType.INTERFERENCE -> generateInterferencePuzzle()
            PuzzleType.QUEEN_SAC -> generateSacrificePuzzle(PieceType.QUEEN)
            PuzzleType.ROOK_SAC -> generateSacrificePuzzle(PieceType.ROOK)
            PuzzleType.MINOR_SAC -> generateSacrificePuzzle(listOf(PieceType.BISHOP, PieceType.KNIGHT).random())
            PuzzleType.STALEMATE_TRAP -> generateStalemateTrapPuzzle()
            PuzzleType.DISCOVERED_ATTACK -> generateDiscoveredAttackPuzzle()
            PuzzleType.DEFENSE -> generateDefensivePuzzle()
            PuzzleType.PROMOTION -> generatePromotionPuzzle()
            PuzzleType.FORK -> generateGuaranteedForkPuzzle()
            PuzzleType.WIN_MATERIAL, PuzzleType.PIN, PuzzleType.SKEWER -> generateTacticalPuzzle(type)
            else -> generateMatePuzzle(difficulty, type)
        }
    }

    // --- SACRIFICE GENERATORS ---
    private fun generateSacrificePuzzle(sacrificeType: PieceType): Puzzle {
        var attempts = 0
        while (attempts < 5000) {
            val board = Board()
            board.clear()
            board.pieceAdd(Piece.BLACK_KING, listOf(Square.G8, Square.H8).random())
            board.pieceAdd(Piece.BLACK_PAWN, Square.F7)
            board.pieceAdd(Piece.BLACK_PAWN, Square.G7)
            board.pieceAdd(Piece.BLACK_PAWN, Square.H7)
            board.pieceAdd(Piece.WHITE_KING, Square.A1)
            placeRandomPiece(board, sacrificeType)
            val whiteAttackers = listOf(Piece.WHITE_QUEEN, Piece.WHITE_ROOK, Piece.WHITE_BISHOP, Piece.WHITE_KNIGHT)
            for (i in 1..2) { placeRandomPiece(board, whiteAttackers.random()) }
            placeRandomPiece(board, listOf(Piece.BLACK_QUEEN, Piece.BLACK_ROOK, Piece.BLACK_KNIGHT, Piece.BLACK_BISHOP).random())
            board.setSideToMove(Side.WHITE)
            val mateSequence = findForcedMate(board, 2)
            if (mateSequence.isNotEmpty() && isSacrifice(board, mateSequence[0], sacrificeType)) {
                val (pType, theme) = when (sacrificeType) {
                    PieceType.QUEEN -> Pair(PuzzleType.QUEEN_SAC, Theme.QUEEN_SACRIFICE)
                    PieceType.ROOK -> Pair(PuzzleType.ROOK_SAC, Theme.ROOK_SACRIFICE)
                    else -> Pair(PuzzleType.MINOR_SAC, Theme.MINOR_SACRIFICE)
                }
                return Puzzle(board.fen, mateSequence.toMutableList(), pType, Difficulty.HARD, theme, 2)
            }
            attempts++
        }
        return generateMatePuzzle(Difficulty.EASY, PuzzleType.MATE_IN_ONE)
    }

    private fun isSacrifice(board: Board, moveStr: String, requiredType: PieceType): Boolean {
        val move = Move(moveStr.substring(0, 2), moveStr.substring(2, 4))
        val piece = board.getPiece(move.from)
        if (piece.pieceType != requiredType) return false
        board.doMove(move)
        val blackMoves = board.legalMoves()
        var canBeCaptured = false
        for (bMove in blackMoves) {
            if (bMove.capture == move.to) { canBeCaptured = true; break }
        }
        board.undoMove()
        return canBeCaptured
    }

    private fun generateGrecoMatePuzzle(): Puzzle {
        var attempts = 0
        while (attempts < 5000) {
            val board = Board()
            board.clear()
            board.pieceAdd(Piece.BLACK_KING, Square.G8)
            board.pieceAdd(Piece.BLACK_ROOK, Square.F8)
            board.pieceAdd(Piece.BLACK_PAWN, Square.F7)
            board.pieceAdd(Piece.BLACK_PAWN, Square.G7)
            board.pieceAdd(Piece.BLACK_PAWN, Square.H7)
            board.pieceAdd(Piece.WHITE_KING, Square.A1)
            placeRandomPiece(board, Piece.WHITE_QUEEN)
            placeRandomPiece(board, Piece.WHITE_BISHOP)
            board.pieceAdd(Piece.WHITE_KNIGHT, Square.E6)
            board.setSideToMove(Side.WHITE)
            val mateSequence = findForcedMate(board, 3)
            if (mateSequence.isNotEmpty() && isBishopSacOnF7(board, mateSequence[0])) {
                return Puzzle(board.fen, mateSequence.toMutableList(), PuzzleType.GRECO_MATE, Difficulty.HARD, Theme.CLASSIC_GRECO, 3)
            }
            attempts++
        }
        return generateMatePuzzle(Difficulty.MEDIUM, PuzzleType.MATE_IN_TWO)
    }

    private fun isBishopSacOnF7(board: Board, moveStr: String): Boolean {
        val move = Move(moveStr.substring(0, 2), moveStr.substring(2, 4))
        val piece = board.getPiece(move.from)
        return piece.pieceType == PieceType.BISHOP && move.to == Square.F7
    }

    private fun generateClearanceSacPuzzle(): Puzzle {
        var attempts = 0
        while (attempts < 5000) {
            val board = Board()
            board.clear()
            board.pieceAdd(Piece.BLACK_KING, listOf(Square.G8, Square.H8).random())
            board.pieceAdd(Piece.BLACK_PAWN, Square.F7)
            board.pieceAdd(Piece.BLACK_PAWN, Square.G7)
            board.pieceAdd(Piece.BLACK_PAWN, Square.H7)
            board.pieceAdd(Piece.WHITE_KING, Square.A1)
            placeRandomPiece(board, Piece.WHITE_ROOK)
            placeRandomPiece(board, Piece.WHITE_QUEEN)
            board.setSideToMove(Side.WHITE)
            val mateSequence = findForcedMate(board, 3)
            if (mateSequence.isNotEmpty() && isClearanceSacrifice(board, mateSequence[0])) {
                return Puzzle(board.fen, mateSequence.toMutableList(), PuzzleType.CLEARANCE_SAC, Difficulty.HARD, Theme.CLEARANCE_SACRIFICE, 3)
            }
            attempts++
        }
        return generateMatePuzzle(Difficulty.MEDIUM, PuzzleType.MATE_IN_TWO)
    }

    private fun isClearanceSacrifice(board: Board, moveStr: String): Boolean {
        val move = Move(moveStr.substring(0, 2), moveStr.substring(2, 4))
        val piece = board.getPiece(move.from)
        val isHeavyPiece = piece.pieceType == PieceType.ROOK || piece.pieceType == PieceType.BISHOP || piece.pieceType == PieceType.QUEEN
        val isShieldSquare = listOf(Square.F7, Square.G7, Square.H7).contains(move.to)
        return isHeavyPiece && isShieldSquare
    }

    // --- ADVANCED TACTICS ---
    private fun generateInterferencePuzzle(): Puzzle {
        val board = Board()
        board.clear()
        board.pieceAdd(Piece.BLACK_KING, Square.H8)
        board.pieceAdd(Piece.BLACK_ROOK, Square.F8)
        board.pieceAdd(Piece.BLACK_PAWN, Square.G7)
        board.pieceAdd(Piece.BLACK_PAWN, Square.H7)
        board.pieceAdd(Piece.WHITE_KING, Square.E1)
        board.pieceAdd(Piece.WHITE_ROOK, Square.A1)
        board.pieceAdd(Piece.WHITE_KNIGHT, Square.D6)
        board.setSideToMove(Side.WHITE)
        val mateSequence = findForcedMate(board, 2)
        return if (mateSequence.isNotEmpty() && isInterferenceMove(board, mateSequence[0])) {
            Puzzle(board.fen, mateSequence.toMutableList(), PuzzleType.INTERFERENCE, Difficulty.HARD, Theme.INTERFERENCE_SAC, 2)
        } else {
            generateMatePuzzle(Difficulty.MEDIUM, PuzzleType.MATE_IN_TWO)
        }
    }

    private fun isInterferenceMove(board: Board, moveStr: String): Boolean {
        val move = Move(moveStr.substring(0, 2), moveStr.substring(2, 4))
        val piece = board.getPiece(move.from)
        if (piece.pieceType != PieceType.KNIGHT && piece.pieceType != PieceType.BISHOP) return false
        return move.to == Square.F8
    }

    private fun generateUnderpromotionPuzzle(): Puzzle {
        val board = Board()
        board.clear()
        board.pieceAdd(Piece.BLACK_KING, Square.A8)
        board.pieceAdd(Piece.BLACK_PAWN, Square.A7)
        board.pieceAdd(Piece.BLACK_PAWN, Square.B7)
        board.pieceAdd(Piece.WHITE_KING, Square.B5)
        board.pieceAdd(Piece.WHITE_PAWN, Square.C7)
        board.setSideToMove(Side.WHITE)
        val mateSequence = findForcedMate(board, 1)
        return if (mateSequence.isNotEmpty() && mateSequence[0].contains("=N")) {
            Puzzle(board.fen, mateSequence.toMutableList(), PuzzleType.UNDERPROMOTION, Difficulty.HARD, Theme.KNIGHT_UNDERPROMOTION, 1)
        } else {
            generateMatePuzzle(Difficulty.EASY, PuzzleType.MATE_IN_ONE)
        }
    }

    private fun generateStalemateTrapPuzzle(): Puzzle {
        val board = Board()
        board.clear()
        board.pieceAdd(Piece.BLACK_KING, Square.H8)
        board.pieceAdd(Piece.WHITE_KING, Square.F7)
        board.pieceAdd(Piece.WHITE_QUEEN, Square.A1)
        board.setSideToMove(Side.WHITE)
        val mateIn1 = findForcedMate(board, 1)
        return if (mateIn1.isNotEmpty()) {
            Puzzle(board.fen, mateIn1.toMutableList(), PuzzleType.STALEMATE_TRAP, Difficulty.HARD, Theme.AVOID_STALEMATE, 2)
        } else {
            generateMatePuzzle(Difficulty.EASY, PuzzleType.MATE_IN_ONE)
        }
    }

    private fun generateDiscoveredAttackPuzzle(): Puzzle {
        val board = Board()
        board.clear()
        board.pieceAdd(Piece.WHITE_KING, Square.A1)
        board.pieceAdd(Piece.BLACK_KING, Square.H8)
        board.pieceAdd(Piece.WHITE_BISHOP, Square.A1)
        board.pieceAdd(Piece.BLACK_QUEEN, Square.E5)
        board.pieceAdd(Piece.WHITE_KNIGHT, Square.B2)
        board.setSideToMove(Side.WHITE)
        val tacticMoves = findForcedTactic(board, depth = 3, threshold = 9)
        return if (tacticMoves.isNotEmpty()) {
            Puzzle(board.fen, tacticMoves.toMutableList(), PuzzleType.DISCOVERED_ATTACK, Difficulty.TACTICAL, Theme.DISCOVERED_BISHOP, 2)
        } else {
            generateTacticalPuzzle(PuzzleType.WIN_MATERIAL)
        }
    }

    // --- DEFENSE & ENDGAMES ---
    private fun generateDefensivePuzzle(): Puzzle {
        var attempts = 0
        while (attempts < 1000) {
            val board = Board()
            board.clear()
            board.pieceAdd(Piece.WHITE_KING, Square.H1)
            board.pieceAdd(Piece.WHITE_PAWN, Square.G2)
            board.pieceAdd(Piece.BLACK_KING, Square.A8)
            board.pieceAdd(Piece.BLACK_QUEEN, Square.D8)
            board.pieceAdd(Piece.BLACK_ROOK, Square.A1)
            board.setSideToMove(Side.WHITE)
            val moves = board.legalMoves()
            var safeMoves = 0
            var theSafeMove = ""
            for (move in moves) {
                board.doMove(move)
                if (!board.isMated) {
                    val blackReplies = board.legalMoves()
                    var blackCanMate = false
                    for (bMove in blackReplies) {
                        board.doMove(bMove)
                        if (board.isMated) blackCanMate = true
                        board.undoMove()
                        if (blackCanMate) break
                    }
                    if (!blackCanMate) { safeMoves++; theSafeMove = move.toString() }
                }
                board.undoMove()
            }
            if (safeMoves == 1) {
                return Puzzle(board.fen, mutableListOf(theSafeMove), PuzzleType.DEFENSE, Difficulty.HARD, Theme.SURVIVAL, 1)
            }
            attempts++
        }
        return generateMatePuzzle(Difficulty.EASY, PuzzleType.MATE_IN_ONE)
    }

    private fun generatePromotionPuzzle(): Puzzle {
        val board = Board()
        board.clear()
        board.pieceAdd(Piece.WHITE_KING, Square.E6)
        board.pieceAdd(Piece.BLACK_KING, Square.E8)
        board.pieceAdd(Piece.WHITE_PAWN, Square.E7)
        board.setSideToMove(Side.WHITE)
        val winSequence = findForcedMate(board, 3)
        return if (winSequence.isNotEmpty()) {
            Puzzle(board.fen, winSequence.toMutableList(), PuzzleType.PROMOTION, Difficulty.ENDGAME, Theme.PAWN_ENDGAME, 2)
        } else {
            generateMatePuzzle(Difficulty.EASY, PuzzleType.MATE_IN_ONE)
        }
    }

    // --- FORKS & TACTICS ---
    private fun generateGuaranteedForkPuzzle(): Puzzle {
        val board = Board()
        board.clear()
        board.pieceAdd(Piece.WHITE_KING, Square.A1)
        board.pieceAdd(Piece.BLACK_KING, Square.H8)
        board.pieceAdd(Piece.WHITE_KNIGHT, Square.F7)
        board.pieceAdd(Piece.BLACK_QUEEN, Square.E5)
        board.setSideToMove(Side.WHITE)
        val tacticMoves = findForcedTactic(board, depth = 3, threshold = 9)
        return if (tacticMoves.isNotEmpty()) {
            Puzzle(board.fen, tacticMoves.toMutableList(), PuzzleType.FORK, Difficulty.TACTICAL, Theme.GUARANTEED_FORK, 2)
        } else {
            generateTacticalPuzzle(PuzzleType.WIN_MATERIAL)
        }
    }

    private fun generateTacticalPuzzle(type: PuzzleType): Puzzle {
        var attempts = 0
        while (attempts < 2000) {
            val board = Board()
            board.clear()
            board.pieceAdd(Piece.WHITE_KING, Square.A1)
            board.pieceAdd(Piece.BLACK_KING, Square.H8)
            placeRandomPiece(board, Piece.WHITE_QUEEN)
            placeRandomPiece(board, Piece.WHITE_ROOK)
            val blackTargets = listOf(Piece.BLACK_QUEEN, Piece.BLACK_ROOK, Piece.BLACK_BISHOP, Piece.BLACK_KNIGHT)
            for (i in 1..2) { placeRandomPiece(board, blackTargets.random()) }
            board.setSideToMove(Side.WHITE)
            val tacticMoves = findForcedTactic(board, depth = 2, threshold = 3)
            if (tacticMoves.isNotEmpty()) {
                return Puzzle(board.fen, tacticMoves.toMutableList(), type, Difficulty.TACTICAL, Theme.TACTICAL_SCRAMBLE, 1)
            }
            attempts++
        }
        return generateMatePuzzle(Difficulty.EASY, PuzzleType.MATE_IN_ONE)
    }

    // --- MATE GENERATION ---
    private fun generateMatePuzzle(difficulty: Difficulty, type: PuzzleType): Puzzle {
        val targetDepth = when (type) {
            PuzzleType.MATE_IN_ONE -> 1
            PuzzleType.MATE_IN_TWO -> 2
            PuzzleType.MATE_IN_THREE -> 3
            else -> 1
        }
        val (board, theme) = when (difficulty) {
            Difficulty.EASY -> generateEdgeMateSeed()
            Difficulty.MEDIUM -> generateBackRankSeed()
            Difficulty.HARD -> generateSmotheredSeed()
            else -> generateEdgeMateSeed()
        }
        val mateSequence = findForcedMate(board, targetDepth)
        return if (mateSequence.isNotEmpty()) {
            Puzzle(board.fen, mateSequence.toMutableList(), type, difficulty, theme, targetDepth)
        } else {
            generateMatePuzzle(difficulty, PuzzleType.MATE_IN_ONE)
        }
    }

    // --- HELPER FUNCTIONS ---
    private fun placeRandomPiece(board: Board, piece: Piece) {
        var sq: Square
        do { sq = Square.values().random() } while (board.getPiece(sq) != Piece.NONE)
        board.pieceAdd(piece, sq)
    }

    private fun findForcedTactic(board: Board, depth: Int, threshold: Int): List<String> {
        val currentMaterial = evaluateMaterial(board)
        val moves: MoveList = board.legalMoves()
        if (board.sideToMove == Side.WHITE) {
            for (move in moves) {
                board.doMove(move)
                if (depth == 1) {
                    if (evaluateMaterial(board) >= currentMaterial + threshold) {
                        board.undoMove(); return listOf(move.toString())
                    }
                } else {
                    val blackBestMaterial = minimaxMaterial(board, depth - 1, isMaximizing = false)
                    if (blackBestMaterial >= currentMaterial + threshold) {
                        board.undoMove(); return listOf(move.toString())
                    }
                }
                board.undoMove()
            }
        }
        return emptyList()
    }

    private fun minimaxMaterial(board: Board, depth: Int, isMaximizing: Boolean): Int {
        if (depth == 0 || board.isMated || board.isStaleMate) return evaluateMaterial(board)
        val moves = board.legalMoves()
        if (moves.isEmpty()) return evaluateMaterial(board)
        return if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (move in moves) {
                board.doMove(move)
                val eval = minimaxMaterial(board, depth - 1, false)
                board.undoMove()
                if (eval > maxEval) maxEval = eval
            }
            maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in moves) {
                board.doMove(move)
                val eval = minimaxMaterial(board, depth - 1, true)
                board.undoMove()
                if (eval < minEval) minEval = eval
            }
            minEval
        }
    }

    private fun evaluateMaterial(board: Board): Int {
        var score = 0
        for (sq in Square.values()) {
            when (board.getPiece(sq)) {
                Piece.WHITE_PAWN -> score += 1
                Piece.WHITE_KNIGHT, Piece.WHITE_BISHOP -> score += 3
                Piece.WHITE_ROOK -> score += 5
                Piece.WHITE_QUEEN -> score += 9
                Piece.BLACK_PAWN -> score -= 1
                Piece.BLACK_KNIGHT, Piece.BLACK_BISHOP -> score -= 3
                Piece.BLACK_ROOK -> score -= 5
                Piece.BLACK_QUEEN -> score -= 9
                else -> {}
            }
        }
        return score
    }

    private fun findForcedMate(board: Board, depth: Int): List<String> {
        if (board.isMated) return emptyList()
        val moves: MoveList = board.legalMoves()
        if (board.sideToMove == Side.WHITE) {
            for (move in moves) {
                board.doMove(move)
                if (depth == 1) {
                    if (board.isMated) { board.undoMove(); return listOf(move.toString()) }
                } else {
                    val blackResponses = findForcedMate(board, depth - 1)
                    if (blackResponses.isNotEmpty()) { board.undoMove(); return listOf(move.toString()) + blackResponses }
                }
                board.undoMove()
            }
        } else {
            if (moves.isEmpty()) return emptyList()
            var bestSequence = emptyList<String>()
            for (blackMove in moves) {
                board.doMove(blackMove)
                val whiteFollowUp = findForcedMate(board, depth - 1)
                if (whiteFollowUp.isEmpty()) { board.undoMove(); return emptyList() }
                board.undoMove()
                if (bestSequence.isEmpty()) bestSequence = listOf(blackMove.toString()) + whiteFollowUp
            }
            return bestSequence
        }
        return emptyList()
    }

    // --- SEEDS ---
    private fun generateEdgeMateSeed(): Pair<Board, Theme> {
        val board = Board()
        board.clear()
        val cornerFiles = listOf(Square.A8, Square.H8)
        val kingSquare = cornerFiles.random()
        board.pieceAdd(Piece.BLACK_KING, kingSquare)
        val pawnSquare = if (kingSquare.file == 0) Square.B7 else Square.G7
        board.pieceAdd(Piece.BLACK_PAWN, pawnSquare)
        board.pieceAdd(Piece.WHITE_KING, if (kingSquare.file == 0) Square.C6 else Square.F6)
        val queenSquare = Square.values().filter { it != kingSquare && it != pawnSquare && it != Square.C6 && it != Square.F6 }.random()
        board.pieceAdd(Piece.WHITE_QUEEN, queenSquare)
        board.setSideToMove(Side.WHITE)
        return Pair(board, Theme.EDGE_MATE)
    }

    private fun generateBackRankSeed(): Pair<Board, Theme> {
        val board = Board()
        board.clear()
        board.pieceAdd(Piece.BLACK_KING, Square.G8)
        board.pieceAdd(Piece.BLACK_PAWN, Square.F7)
        board.pieceAdd(Piece.BLACK_PAWN, Square.G7)
        board.pieceAdd(Piece.BLACK_PAWN, Square.H7)
        board.pieceAdd(Piece.WHITE_KING, Square.E4)
        val rookSquare = listOf(Square.A1, Square.B1, Square.C1, Square.D1).random()
        board.pieceAdd(Piece.WHITE_ROOK, rookSquare)
        val defenderSquare = Square.values().filter { board.getPiece(it) == Piece.NONE && it.rank != 0 }.random()
        board.pieceAdd(Piece.BLACK_KNIGHT, defenderSquare)
        board.setSideToMove(Side.WHITE)
        return Pair(board, Theme.BACK_RANK_MATE)
    }

    private fun generateSmotheredSeed(): Pair<Board, Theme> {
        val board = Board()
        board.clear()
        board.pieceAdd(Piece.BLACK_KING, Square.H8)
        board.pieceAdd(Piece.BLACK_ROOK, Square.G8)
        board.pieceAdd(Piece.BLACK_PAWN, Square.G7)
        board.pieceAdd(Piece.BLACK_PAWN, Square.H7)
        board.pieceAdd(Piece.WHITE_KING, Square.F6)
        board.pieceAdd(Piece.WHITE_KNIGHT, Square.E5)
        board.pieceAdd(Piece.WHITE_QUEEN, Square.A2)
        board.setSideToMove(Side.WHITE)
        return Pair(board, Theme.SMOTHERED_MATE)
    }
}
