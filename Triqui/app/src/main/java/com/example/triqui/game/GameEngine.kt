package com.example.triqui.game

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class Mark { X, O, EMPTY }
enum class Winner { PLAYER, AI, DRAW }

data class Scores(
    val playerWins: Int = 0,
    val draws: Int = 0,
    val aiWins: Int = 0
)

object GameEngine {

    private val wins = listOf(
        listOf(0,1,2), listOf(3,4,5), listOf(6,7,8), // filas
        listOf(0,3,6), listOf(1,4,7), listOf(2,5,8), // columnas
        listOf(0,4,8), listOf(2,4,6)                 // diagonales
    )

    fun isMoveLegal(board: List<Mark>, index: Int): Boolean =
        index in 0..8 && board[index] == Mark.EMPTY

    fun checkWinner(board: List<Mark>): Mark? {
        for (combo in wins) {
            val (a, b, c) = combo
            if (board[a] != Mark.EMPTY && board[a] == board[b] && board[b] == board[c]) {
                return board[a]
            }
        }
        return null
    }

    fun isDraw(board: List<Mark>): Boolean =
        checkWinner(board) == null && board.none { it == Mark.EMPTY }

    /**
     * Devuelve el mejor movimiento para la IA usando Minimax con poda alfa-beta.
     * La IA es 'aiMark'. El humano es 'playerMark'.
     */
    fun bestMove(board: List<Mark>, aiMark: Mark, playerMark: Mark): Int {
        var bestScore = Int.MIN_VALUE
        var move = -1

        for (i in board.indices) {
            if (board[i] == Mark.EMPTY) {
                val newBoard = board.toMutableList()
                newBoard[i] = aiMark
                val score = minimax(newBoard, depth = 0, maximizing = false, aiMark, playerMark, alpha = Int.MIN_VALUE, beta = Int.MAX_VALUE)
                if (score > bestScore) {
                    bestScore = score
                    move = i
                }
            }
        }

        // Si hay varios equivalentes, desempata aleatoriamente para que no sea monótono
        if (move == -1) {
            val options = board.withIndex().filter { it.value == Mark.EMPTY }.map { it.index }
            if (options.isNotEmpty()) move = options[Random.nextInt(options.size)]
        }
        return move
    }

    /**
     * Minimax con poda alfa-beta.
     * Puntuación: +10 (IA gana), -10 (Jugador gana), 0 (empate). Ajuste por profundidad
     * para preferir victorias rápidas y demorar derrotas.
     */
    private fun minimax(
        board: MutableList<Mark>,
        depth: Int,
        maximizing: Boolean,
        aiMark: Mark,
        playerMark: Mark,
        alpha: Int,
        beta: Int
    ): Int {
        val winner = checkWinner(board)
        if (winner == aiMark) return 10 - depth
        if (winner == playerMark) return depth - 10
        if (board.none { it == Mark.EMPTY }) return 0 // empate

        var a = alpha
        var b = beta

        if (maximizing) {
            var best = Int.MIN_VALUE
            for (i in board.indices) {
                if (board[i] == Mark.EMPTY) {
                    board[i] = aiMark
                    val score = minimax(board, depth + 1, false, aiMark, playerMark, a, b)
                    board[i] = Mark.EMPTY
                    best = max(best, score)
                    a = max(a, best)
                    if (b <= a) break // poda
                }
            }
            return best
        } else {
            var best = Int.MAX_VALUE
            for (i in board.indices) {
                if (board[i] == Mark.EMPTY) {
                    board[i] = playerMark
                    val score = minimax(board, depth + 1, true, aiMark, playerMark, a, b)
                    board[i] = Mark.EMPTY
                    best = min(best, score)
                    b = min(b, best)
                    if (b <= a) break // poda
                }
            }
            return best
        }
    }
}
