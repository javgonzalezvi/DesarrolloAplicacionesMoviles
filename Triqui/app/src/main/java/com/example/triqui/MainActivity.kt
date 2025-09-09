package com.example.triqui

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.triqui.game.CoinFace
import com.example.triqui.game.GameViewModel
import com.example.triqui.game.Mark
import com.example.triqui.game.Scores
import com.example.triqui.game.Winner

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TriquiApp()
        }
    }
}

@Composable
fun TriquiApp(vm: GameViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val activity = LocalContext.current as Activity

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // --- Sección de Puntajes ---
                ScoreBoard(
                    scores = state.scores,
                    playerMark = state.playerMark,
                    aiMark = state.aiMark,
                    lastCoinText = state.lastCoinResultText,
                    currentTurn = state.currentTurn,
                    gameOver = state.gameOver,
                    winner = state.winner
                )

                // --- Tablero ---
                BoardGrid(
                    board = state.board,
                    onCellClick = { vm.onCellClicked(it) },
                    enabled = !state.gameOver && state.isPlayerTurn
                )

                // --- Botones de control ---
                ControlButtons(
                    onNewGame = { vm.newGame() },
                    onExit = { vm.exitRequested { activity.finish() } }
                )
            }

            // Diálogo de Cara o Sello al iniciar cada juego
            if (state.showCoinDialog) {
                CoinDialog(
                    onChoose = { vm.resolveCoinToss(it) }
                )
            }
        }
    }
}

@Composable
fun ScoreBoard(
    scores: Scores,
    playerMark: Mark,
    aiMark: Mark,
    lastCoinText: String,
    currentTurn: Mark,
    gameOver: Boolean,
    winner: Winner?
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "Triqui (Tic-Tac-Toe) — Minimax",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(title = "Jugador (wins)", value = scores.playerWins.toString())
            StatCard(title = "Empates", value = scores.draws.toString())
            StatCard(title = "Máquina (wins)", value = scores.aiWins.toString())
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Marcas → Jugador: ${playerMark.name}  •  Máquina: ${aiMark.name}",
            style = MaterialTheme.typography.bodyMedium
        )

        val statusText = when {
            gameOver && winner == Winner.PLAYER -> "¡Ganaste! 🎉"
            gameOver && winner == Winner.AI -> "La máquina ganó."
            gameOver && winner == Winner.DRAW -> "Empate."
            else -> "Turno: ${currentTurn.name}"
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )

        if (lastCoinText.isNotBlank()) {
            Text(
                text = lastCoinText,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun StatCard(title: String, value: String) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.sizeIn(minWidth = 100.dp).padding(4.dp)
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 12.sp, textAlign = TextAlign.Center)
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BoardGrid(
    board: List<Mark>,
    onCellClick: (Int) -> Unit,
    enabled: Boolean
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val size = 3
        for (r in 0 until size) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                for (c in 0 until size) {
                    val index = r * size + c
                    Cell(
                        mark = board[index],
                        onClick = { onCellClick(index) },
                        enabled = enabled && board[index] == Mark.EMPTY
                    )
                }
            }
        }
    }
}

@Composable
fun Cell(
    mark: Mark,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val text = when (mark) {
        Mark.X -> "X"
        Mark.O -> "O"
        Mark.EMPTY -> ""
    }

    val color = when (mark) {
        Mark.X -> Color.Green
        Mark.O -> Color.Red
        else -> Color.Unspecified
    }

    OutlinedCard(
        modifier = Modifier
            .size(96.dp)
            .padding(6.dp)
            .let { if (enabled) it.clickable { onClick() } else it },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = color
            )
        }
    }
}

@Composable
fun ControlButtons(
    onNewGame: () -> Unit,
    onExit: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        Button(onClick = onNewGame) {
            Text("Nuevo juego")
        }
        OutlinedButton(onClick = onExit) {
            Text("Salir")
        }
    }
}

@Composable
fun CoinDialog(onChoose: (CoinFace) -> Unit) {
    AlertDialog(
        onDismissRequest = { /* obligatorio elegir */ },
        title = { Text("Cara o Sello") },
        text = { Text("Elige tu opción para determinar quién inicia. Quien inicia juega con 'X'.") },
        confirmButton = {
            Button(onClick = { onChoose(CoinFace.CARA) }) { Text("Cara") }
        },
        dismissButton = {
            OutlinedButton(onClick = { onChoose(CoinFace.SELLO) }) { Text("Sello") }
        }
    )
}



//package com.example.triqui
//
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.tooling.preview.Preview
//import com.example.triqui.ui.theme.TriquiTheme
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContent {
//            TriquiTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Greeting(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    TriquiTheme {
//        Greeting("Android")
//    }
//}