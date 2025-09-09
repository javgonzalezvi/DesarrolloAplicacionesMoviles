package com.example.triqui.game

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

data class GameState(
    val board: List<Mark> = List(9) { Mark.EMPTY },
    val currentTurn: Mark = Mark.X,        // Siempre empieza 'X'
    val playerMark: Mark = Mark.X,         // Se decide tras el cara/sello
    val aiMark: Mark = Mark.O,             // Se decide tras el cara/sello
    val isPlayerTurn: Boolean = true,      // Turno actual del humano
    val gameOver: Boolean = false,
    val winner: Winner? = null,
    val scores: Scores = Scores(),
    val showCoinDialog: Boolean = true,    // Mostrar diálogo "Cara o Sello"
    val lastCoinResultText: String = ""    // Texto informativo luego del sorteo
)

enum class CoinFace { CARA, SELLO }

class GameViewModel : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state

    private var aiAutoPlaying = false

    /** El usuario elige CARA o SELLO; hacemos el sorteo. */
    fun resolveCoinToss(userChoice: CoinFace) {
        val coinIsHeads = Random.nextBoolean()
        val playerStarts = (coinIsHeads && userChoice == CoinFace.CARA) || (!coinIsHeads && userChoice == CoinFace.SELLO)

        val playerMark = if (playerStarts) Mark.X else Mark.O
        val aiMark = if (playerStarts) Mark.O else Mark.X

        _state.update {
            it.copy(
                board = List(9) { Mark.EMPTY },
                currentTurn = Mark.X, // el que inicia siempre 'X'
                playerMark = playerMark,
                aiMark = aiMark,
                isPlayerTurn = playerStarts, // si inicia el jugador, juega primero
                gameOver = false,
                winner = null,
                showCoinDialog = false,
                lastCoinResultText = buildString {
                    append("Resultado: ${if (coinIsHeads) "Cara" else "Sello"}. ")
                    append(if (playerStarts) "Inicia el jugador (X)." else "Inicia la máquina (X).")
                }
            )
        }

        maybeAIMove()
    }

    fun onCellClicked(index: Int) {
        val s = _state.value
        if (s.gameOver || !s.isPlayerTurn) return
        if (!GameEngine.isMoveLegal(s.board, index)) return

        val newBoard = s.board.toMutableList()
        newBoard[index] = s.playerMark

        advanceGame(newBoard, nextTurn = toggle(s.currentTurn))
        maybeAIMove()
    }

    fun newGame() {
        _state.update {
            it.copy(
                board = List(9) { Mark.EMPTY },
                currentTurn = Mark.X,
                gameOver = false,
                winner = null,
                showCoinDialog = true,
                lastCoinResultText = ""
            )
        }
    }

    fun exitRequested(onExit: () -> Unit) {
        onExit()
    }

    // --- Helpers ---

    private fun maybeAIMove() {
        val s = _state.value
        if (s.gameOver || s.isPlayerTurn || aiAutoPlaying) return
        aiAutoPlaying = true

        val move = GameEngine.bestMove(s.board, s.aiMark, s.playerMark)
        if (move in 0..8 && GameEngine.isMoveLegal(s.board, move)) {
            val newBoard = s.board.toMutableList()
            newBoard[move] = s.aiMark
            advanceGame(newBoard, nextTurn = toggle(s.currentTurn))
        }

        aiAutoPlaying = false
    }

    private fun advanceGame(newBoard: MutableList<Mark>, nextTurn: Mark) {
        val s = _state.value
        val winnerMark = GameEngine.checkWinner(newBoard)
        val isDraw = GameEngine.isDraw(newBoard)

        if (winnerMark != null) {
            val winner = if (winnerMark == s.playerMark) Winner.PLAYER else Winner.AI
            _state.update {
                it.copy(
                    board = newBoard,
                    currentTurn = nextTurn,
                    isPlayerTurn = false,
                    gameOver = true,
                    winner = winner,
                    scores = when (winner) {
                        Winner.PLAYER -> it.scores.copy(playerWins = it.scores.playerWins + 1)
                        Winner.AI     -> it.scores.copy(aiWins = it.scores.aiWins + 1)
                        else -> it.scores
                    }
                )
            }
        } else if (isDraw) {
            _state.update {
                it.copy(
                    board = newBoard,
                    currentTurn = nextTurn,
                    isPlayerTurn = false,
                    gameOver = true,
                    winner = Winner.DRAW,
                    scores = it.scores.copy(draws = it.scores.draws + 1)
                )
            }
        } else {
            _state.update {
                it.copy(
                    board = newBoard,
                    currentTurn = nextTurn,
                    isPlayerTurn = (nextTurn == it.playerMark)
                )
            }
        }
    }

    private fun toggle(mark: Mark): Mark = if (mark == Mark.X) Mark.O else Mark.X
}
