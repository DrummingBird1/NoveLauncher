package com.ailauncher.app.data.api

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Contract test against Open-Meteo's actual response shape — catches a parsing
 * break (field renamed, nesting changed) that a pure-logic test never would.
 * Always passes explicit lat/lon so getCurrentLocation() (which touches
 * Context/LocationManager) is never reached — a relaxed mock Context is enough,
 * no Robolectric needed.
 */
class WeatherServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: WeatherService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        service = WeatherService(mockk(relaxed = true)).apply {
            baseUrl = server.url("/").toString().trimEnd('/')
        }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `parses a real-shape Open-Meteo response`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "latitude": 32.08, "longitude": 34.78,
                  "current": {
                    "temperature_2m": 27.3,
                    "weather_code": 2,
                    "wind_speed_10m": 11.4,
                    "is_day": 1
                  }
                }
                """.trimIndent()
            )
        )

        val result = service.fetchWeather(lat = 32.0853, lon = 34.7818)

        assertEquals(27.3, result?.temperature ?: 0.0, 0.001)
        assertEquals(2, result?.weatherCode)
        assertEquals(11.4, result?.windSpeed ?: 0.0, 0.001)
        assertEquals(1, result?.isDay)
        assertEquals("⛅", result?.icon)
    }

    @Test
    fun `missing current object returns null instead of throwing`() = runTest {
        server.enqueue(MockResponse().setBody("""{"latitude": 32.08, "longitude": 34.78}"""))

        val result = service.fetchWeather(lat = 32.0853, lon = 34.7818)

        assertNull(result)
    }

    @Test
    fun `non-2xx response returns null instead of throwing`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = service.fetchWeather(lat = 32.0853, lon = 34.7818)

        assertNull(result)
    }

    @Test
    fun `malformed json returns null instead of throwing`() = runTest {
        server.enqueue(MockResponse().setBody("not json"))

        val result = service.fetchWeather(lat = 32.0853, lon = 34.7818)

        assertNull(result)
    }
}
