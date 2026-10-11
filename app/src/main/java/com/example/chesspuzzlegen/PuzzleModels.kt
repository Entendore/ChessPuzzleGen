package com.example.chesspuzzlegen

enum class PuzzleType {
    MATE_IN_ONE, MATE_IN_TWO, MATE_IN_THREE, 
    WIN_MATERIAL, FORK, PIN, SKEWER, DISCOVERED_ATTACK, 
    DEFENSE, PROMOTION, STALEMATE_TRAP,
    QUEEN_SAC, ROOK_SAC, MINOR_SAC,
    UNDERPROMOTION, INTERFERENCE,
    GRECO_MATE, CLEARANCE_SAC
}

enum class Difficulty { EASY, MEDIUM, HARD, TACTICAL, ENDGAME }

enum class Theme {
    BACK_RANK_MATE, SMOTHERED_MATE, EDGE_MATE, TACTICAL_SCRAMBLE,
    SURVIVAL, PAWN_ENDGAME, GUARANTEED_FORK, 
    DISCOVERED_BISHOP, AVOID_STALEMATE,
    QUEEN_SACRIFICE, ROOK_SACRIFICE, MINOR_SACRIFICE,
    KNIGHT_UNDERPROMOTION, INTERFERENCE_SAC,
    CLASSIC_GRECO, CLEARANCE_SACRIFICE
}

data class Puzzle(
    val fen: String,
    val correctMoveSequence: MutableList<String>,
    val type: PuzzleType,
    val difficulty: Difficulty,
    val theme: Theme,
    val movesToCompletion: Int
)
