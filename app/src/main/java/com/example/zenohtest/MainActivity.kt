package com.example.zenohtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zenohtest.ui.theme.ZenohTestTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var role by mutableStateOf(Role.NONE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZenohTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (role) {
                        Role.NONE -> RoleSelectionScreen(
                            onRoleSelected = { selectedRole ->
                                role = selectedRole
                                viewModel.startSession(selectedRole)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )

                        Role.DRONE -> DroneScreen(viewModel, Modifier.padding(innerPadding))
                        Role.JOYSTICK -> JoystickScreen(viewModel, Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}


@Composable
fun RoleSelectionScreen(onRoleSelected: (Role) -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Text("Choose your role")
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { onRoleSelected(Role.JOYSTICK) }) {
            Text("Joystick")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onRoleSelected(Role.DRONE) }) {
            Text("Drone")
        }
    }
}

@Composable
fun JoystickScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val gyro = viewModel.gyroValues.value

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Gyroscope", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("X: ${gyro[0]}")
            Text("Y: ${gyro[1]}")
            Text("Z: ${gyro[2]}")
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(200.dp)) {
                OperationButton(
                    onClick = { viewModel.publishOperation(Operation.UP) },
                    modifier = Modifier.align(Alignment.TopCenter),
                    text = "↑"
                )
                OperationButton(
                    onClick = { viewModel.publishOperation(Operation.DOWN) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                    text = "↓"
                )
                OperationButton(
                    onClick = { viewModel.publishOperation(Operation.LEFT) },
                    modifier = Modifier.align(Alignment.CenterStart),
                    text = "←"
                )
                OperationButton(
                    onClick = { viewModel.publishOperation(Operation.RIGHT) },
                    modifier = Modifier.align(Alignment.CenterEnd),
                    text = "→"
                )
            }
        }
    }
}

@Composable
fun DroneScreen(viewModel: MainViewModel, modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = viewModel.operation.value,
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun OperationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(60.dp)
    ) {
        Text(
            text,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}