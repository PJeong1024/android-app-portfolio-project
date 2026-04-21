package com.jdw.skillstestapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.jdw.skillstestapp.R
import com.jdw.skillstestapp.components.SkillsTestAppBar
import com.jdw.skillstestapp.utils.BottomNaviBarScreen
import com.jdw.skillstestapp.utils.Constants
import com.jdw.skillstestapp.utils.NetworkConnectionType
import com.jdw.skillstestapp.utils.getConnectionType

@Composable
fun MainScreen(
    navController: NavController = NavController(LocalContext.current),
) {
    val context = LocalContext.current
    val currentScreen = remember { mutableStateOf(BottomNaviBarScreen.WeatherApiScreen) }

    Scaffold(
        topBar = {
            SkillsTestAppBar(
                title = stringResource(R.string.app_name),
                navController = navController,
                showProfile = false
            )
        },
        bottomBar = {
            BottomNavigationBar(
                items = Constants.items,
                currentScreen = currentScreen,
                onItemSelected = { screen ->
                    currentScreen.value = screen
//                    navController.navigate(screen.route)
                }
            )
        }
    ) { paddingValues ->
        if (getConnectionType(context) != NetworkConnectionType.NoConnection) {
            CurrentScreen(
                screen = currentScreen.value,
                paddingValues = paddingValues,
                navController,
            )
        } else {
            Text(text = NetworkConnectionType.NoConnection.label)
        }
    }
}


@Composable
fun BottomNavigationBar(
    items: List<BottomNaviBarScreen>,
    currentScreen: MutableState<BottomNaviBarScreen>,
    onItemSelected: (BottomNaviBarScreen) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFB3E5FC))
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        items.forEach { screen ->
            BottomNavigationItem(
                screen = screen,
                isSelected = currentScreen.value == screen,
                onItemSelected = { onItemSelected(screen) }
            )
        }
    }
}

@Composable
fun BottomNavigationItem(
    screen: BottomNaviBarScreen,
    isSelected: Boolean,
    onItemSelected: () -> Unit
) {
    val iconColor = if (isSelected) Color(0xFF01579B) else Color(0xFF9DB3B6)
    val textColor = if (isSelected) Color(0xFF01579B) else Color(0xFF9DB3B6)

    Button(
        onClick = onItemSelected,
        modifier = Modifier
            .width(120.dp)
            .height(80.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = screen.icon, tint = iconColor, contentDescription = "Icon")
            Text(
                text = screen.label,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CurrentScreen(
    screen: BottomNaviBarScreen,
    paddingValues: PaddingValues,
    navController: NavController,
) {
    when (screen) {
        BottomNaviBarScreen.GoogleMaps -> GoogleMapsScreen(
            navController = navController,
            viewModel = hiltViewModel(),
            paddingValues = paddingValues
        )

        BottomNaviBarScreen.GeminiCharRoom -> GeminiCharRoomScreen(
            navController = navController,
            viewModel = hiltViewModel(),
            paddingValues = paddingValues
        )

        BottomNaviBarScreen.FirebaseAuthScreen -> FirebaseAuthScreen(
            navController = navController,
            viewModel = hiltViewModel(),
            paddingValues = paddingValues
        )

        BottomNaviBarScreen.WeatherApiScreen -> WeatherApiScreen(
            navController = navController,
            viewModel = hiltViewModel(),
            paddingValues = paddingValues
        )
    }
}