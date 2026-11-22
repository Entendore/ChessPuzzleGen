package com.example.chesspuzzlegen

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import chesslib.Board
import chesslib.Square
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var boardContainer: FrameLayout
    private lateinit var boardGrid: GridLayout
    private lateinit var statusText: TextView
    private val chessLogic = Board()
    private val generator = PuzzleGenerator()

    private var currentPuzzle: Puzzle? = null
    private var selectedSquare: Square? = null
    private var lastMoveFrom: Square? = null
    private var lastMoveTo: Square? = null
    private var cellSize: Int = 0

    private val boardSquares = arrayOfNulls<ImageView>(64)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        boardContainer = findViewById(R.id.boardContainer)
        statusText = findViewById(R.id.statusTextView)

        setupResponsiveBoardUI()

        findViewById<Button>(R.id.btnEasy).setOnClickListener {
            generateNewPuzzle(Difficulty.EASY, PuzzleType.MATE_IN_ONE)
        }
        findViewById<Button>(R.id.btnMedium).setOnClickListener {
            generateNewPuzzle(Difficulty.MEDIUM, PuzzleType.MATE_IN_TWO)
        }
        findViewById<Button>(R.id.btnFork).setOnClickListener {
            generateNewPuzzle(Difficulty.TACTICAL, PuzzleType.FORK)
        }
        findViewById<Button>(R.id.btnSacrifice).setOnClickListener {
            generateNewPuzzle(Difficulty.HARD, PuzzleType.QUEEN_SAC)
        }

        generateNewPuzzle(Difficulty.EASY, PuzzleType.MATE_IN_ONE)
    }

    private fun setupResponsiveBoardUI() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        cellSize = (screenWidth / 8) - 4

        boardGrid = GridLayout(this)
        val params = FrameLayout.LayoutParams(cellSize * 8, cellSize * 8)
        params.gravity = Gravity.CENTER
        boardGrid.layoutParams = params
        boardContainer.addView(boardGrid)

        for (i in 0 until 64) {
            val img = ImageView(this)
            val param = GridLayout.LayoutParams()
            param.width = cellSize
            param.height = cellSize
            param.setMargins(2, 2, 2, 2)
            img.layoutParams = param
            img.scaleType = ImageView.ScaleType.FIT_CENTER

            val row = i / 8
            val col = i % 8
            if ((row + col) % 2 == 0) img.setBackgroundColor(Color.parseColor("#EEEED2")) 
            else img.setBackgroundColor(Color.parseColor("#769656"))

            val square = Square.values()[i]
            img.setOnClickListener { onSquareClicked(square) }

            boardGrid.addView(img)
            boardSquares[i] = img
        }
    }

    private fun generateNewPuzzle(difficulty: Difficulty, type: PuzzleType) {
        statusText.text = "Generating ${type.name.replace("_", " ")}..."
        CoroutineScope(Dispatchers.Default).launch {
            val puzzle = generator.generatePuzzle(difficulty, type)
            withContext(Dispatchers.Main) {
                currentPuzzle = puzzle
                chessLogic.loadFromFen(puzzle.fen)
                selectedSquare = null
                lastMoveFrom = null
                lastMoveTo = null
                updateBoardUI()
                
                val typeName = puzzle.type.name.replace("_", " ")
                statusText.text = "$typeName\nMoves to win: ${puzzle.movesToCompletion}"
            }
        }
    }

    private fun onSquareClicked(square: Square) {
        if (currentPuzzle == null || chessLogic.isMated || chessLogic.isStaleMate) return

        val piece = chessLogic.getPiece(square)

        if (selectedSquare == null) {
            if (piece != chesslib.Piece.NONE && piece.pieceSide == chesslib.Side.WHITE) {
                selectedSquare = square
                updateBoardUI()
            }
        } else {
            val from = selectedSquare!!
            val move = chesslib.Move(from, square)

            if (chessLogic.legalMoves().contains(move)) {
                val expectedMove = currentPuzzle?.correctMoveSequence?.firstOrNull()
                
                if (move.toString() == expectedMove) {
                    chessLogic.doMove(move)
                    lastMoveFrom = from
                    lastMoveTo = square
                    currentPuzzle?.correctMoveSequence?.removeAt(0)
                    selectedSquare = null

                    if (currentPuzzle?.correctMoveSequence?.isEmpty() == true) {
                        statusText.text = "Puzzle Solved! Checkmate."
                        updateBoardUI()
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(2000)
                            generateNewPuzzle(currentPuzzle!!.difficulty, currentPuzzle!!.type)
                        }
                    } else {
                        statusText.text = "Correct! Opponent replying..."
                        updateBoardUI()
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(800)
                            playOpponentMove()
                        }
                    }
                } else {
                    statusText.text = "Wrong move. Try again."
                    selectedSquare = null
                    updateBoardUI()
                }
            } else {
                if (piece != chesslib.Piece.NONE && piece.pieceSide == chesslib.Side.WHITE) {
                    selectedSquare = square
                } else {
                    selectedSquare = null
                }
                updateBoardUI()
            }
        }
    }

    private fun playOpponentMove() {
        val expectedBlackMove = currentPuzzle?.correctMoveSequence?.firstOrNull()
        if (expectedBlackMove != null) {
            val move = chesslib.Move(
                Square.fromValue(expectedBlackMove.substring(0, 2)), 
                Square.fromValue(expectedBlackMove.substring(2, 4))
            )
            chessLogic.doMove(move)
            lastMoveFrom = move.from
            lastMoveTo = move.to
            currentPuzzle?.correctMoveSequence?.removeAt(0)
            updateBoardUI()
            statusText.text = "Your move. Find the solution."
        }
    }

    private fun updateBoardUI() {
        val kingInCheck = chessLogic.isKingAttacked(chesslib.Side.BLACK) || chessLogic.isKingAttacked(chesslib.Side.WHITE)
        val checkedSide = if (chessLogic.isKingAttacked(chesslib.Side.BLACK)) chesslib.Side.BLACK else chesslib.Side.WHITE
        val checkedKingSquare = chessLogic.getKingSquare(checkedSide)

        for (i in 0 until 64) {
            val square = Square.values()[i]
            val img = boardSquares[i] ?: continue
            val piece = chessLogic.getPiece(square)

            img.setImageResource(when (piece) {
                chesslib.Piece.WHITE_KING -> R.drawable.wk
                chesslib.Piece.WHITE_QUEEN -> R.drawable.wq
                chesslib.Piece.WHITE_ROOK -> R.drawable.wr
                chesslib.Piece.WHITE_BISHOP -> R.drawable.wb
                chesslib.Piece.WHITE_KNIGHT -> R.drawable.wn
                chesslib.Piece.WHITE_PAWN -> R.drawable.wp
                chesslib.Piece.BLACK_KING -> R.drawable.bk
                chesslib.Piece.BLACK_QUEEN -> R.drawable.bq
                chesslib.Piece.BLACK_ROOK -> R.drawable.br
                chesslib.Piece.BLACK_BISHOP -> R.drawable.bb
                chesslib.Piece.BLACK_KNIGHT -> R.drawable.bn
                chesslib.Piece.BLACK_PAWN -> R.drawable.bp
                else -> 0
            })

            val row = i / 8
            val col = i % 8
            if ((row + col) % 2 == 0) img.setBackgroundColor(Color.parseColor("#EEEED2")) 
            else img.setBackgroundColor(Color.parseColor("#769656"))

            img.clearColorFilter()

            if (square == lastMoveFrom || square == lastMoveTo) {
                img.setColorFilter(Color.parseColor("#80FFD700"), PorterDuff.Mode.SRC_ATOP)
            }

            if (square == selectedSquare) {
                img.setColorFilter(Color.parseColor("#800000FF"), PorterDuff.Mode.SRC_ATOP)
            }

            if (kingInCheck && square == checkedKingSquare) {
                img.setColorFilter(Color.parseColor("#80FF0000"), PorterDuff.Mode.SRC_ATOP)
            }

            if (selectedSquare != null && piece == chesslib.Piece.NONE) {
                val move = chesslib.Move(selectedSquare!!, square)
                if (chessLogic.legalMoves().contains(move)) {
                    img.setImageResource(R.drawable.dot)
                }
            }
        }
    }
}
