package com.jdw.skillstestapp.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.jdw.skillstestapp.R
import com.jdw.skillstestapp.components.LineWithSpacer
import com.jdw.skillstestapp.data.model.weather.WeatherData
import com.jdw.skillstestapp.screens.viewmodel.WeatherApiViewModel
import com.jdw.skillstestapp.utils.formatDateTime


@Composable
fun WeatherApiScreen(
    navController: NavController,
    viewModel: WeatherApiViewModel,
    paddingValues: PaddingValues
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        MainScreen(viewModel = viewModel)
    }
}

@Composable
fun MainScreen(viewModel: WeatherApiViewModel) {
    var bottomBarVisible by remember { mutableStateOf(false) }
    var bottomBarState by remember { mutableStateOf(FBottomBarState.EmptyState) }

    val weatherData = viewModel.weatherData.collectAsState().value
    val isLoading = viewModel.loading.collectAsState().value

    Scaffold(
        bottomBar = {
            when (bottomBarState) {
                FBottomBarState.EmptyState -> {
                    bottomBarVisible = true
                }

                else -> {

                }
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WeatherScreenHeader {
                    viewModel.getWeather()
                }
                LineWithSpacer(3.dp)

                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    TodayWeatherCompose(weatherData)
                    LineWithSpacer(1.dp)
                    DetailWeatherInfoListCompose(weatherData)
                }
            }
        }
    }
}

@Composable
private fun DetailWeatherInfoListCompose(weatherData: WeatherData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.humidity),
            contentDescription = "Humidity",
            modifier = Modifier
                .size(40.dp)
                .padding(4.dp)
        )
        Text(
            text = "Humidity",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(4.dp),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = weatherData.main.humidity.toString(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(4.dp),
            fontWeight = FontWeight.Bold
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Thermostat,
            contentDescription = "Temperature",
            modifier = Modifier
                .size(40.dp)
                .padding(4.dp)
        )
        Text(
            text = "Temperature",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(8.dp),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = weatherData.main.temp.toString(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(8.dp),
            fontWeight = FontWeight.Bold
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.ArrowDownward,
            contentDescription = "Low Temperature",
            modifier = Modifier
                .size(40.dp)
                .padding(4.dp)
        )
        Text(
            text = "Low Temperature",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(8.dp),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = weatherData.main.temp_min.toString(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(8.dp),
            fontWeight = FontWeight.Bold
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.ArrowUpward,
            contentDescription = "High Temperature",
            modifier = Modifier
                .size(40.dp)
                .padding(4.dp)
        )
        Text(
            text = "High Temperature",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(8.dp),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = weatherData.main.temp_max.toString(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(8.dp),
            fontWeight = FontWeight.Bold
        )
    }

    LineWithSpacer(1.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.sunrise),
            contentDescription = "Sunrise",
            modifier = Modifier
                .size(40.dp)
                .padding(4.dp)
        )
        Text(
            text = "Sunrise",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(8.dp),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = formatDateTime(weatherData.sys.sunrise),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(8.dp),
            fontWeight = FontWeight.Bold
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.sunset),
            contentDescription = "Sunset",
            modifier = Modifier
                .size(40.dp)
                .padding(4.dp),
        )
        Text(
            text = "Sunset",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(8.dp),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = formatDateTime(weatherData.sys.sunset),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(8.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TodayWeatherCompose(weatherData: WeatherData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val todayWeather = weatherData.weather[0]
        val imageUrl = "https://openweathermap.org/img/wn/${todayWeather.icon}.png"

        Icon(
            painter = rememberAsyncImagePainter(model = imageUrl),
            contentDescription = "Weather Icon",
            modifier = Modifier
                .size(100.dp)
        )

        Text(
            text = weatherData.name,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )

        Text(
            text = todayWeather.description.uppercase(),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
        )
    }
}

@Composable
private fun WeatherScreenHeader(refreshWeather: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Weather Api Screen",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh",
            modifier = Modifier
                .size(40.dp)
                .align(alignment = Alignment.CenterVertically)
                .clickable {
                    refreshWeather()
                }
        )
    }
}

enum class FBottomBarState {
    EmptyState,
}


