package com.jdw.skillstestapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val currentScreen = remember { mutableStateOf(BottomNaviBarScreen.GoogleMaps) }

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
    NavigationBar(
        tonalElevation = 4.dp,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        items.forEach { screen ->
            val isSelected = currentScreen.value == screen
            val contentColor = if (isSelected) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
            val indicatorColor = MaterialTheme.colorScheme.secondaryContainer

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .clickable { onItemSelected(screen) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) indicatorColor else Color.Transparent,
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.label,
                            tint = contentColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = screen.label,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = contentColor
                    )
                }
            }
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