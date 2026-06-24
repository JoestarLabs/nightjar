package com.bl4ckswordsman.nightjar.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class TimerPreferencesDataSourceTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    private lateinit var dataSource: TimerPreferencesDataSource

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { temporaryFolder.newFile("test_timer_prefs.preferences_pb") }
        )
        dataSource = TimerPreferencesDataSource(dataStore)
    }

    @Test
    fun testInitialDefaultPreferences() = runTest(testDispatcher) {
        val prefs = dataSource.preferences.first()
        assertEquals(300L, prefs.lastDurationSeconds)
        assertEquals(0L, prefs.startedAtMillis)
        assertFalse(prefs.commitmentMode)
        assertEquals(listOf(300L, 900L, 1800L, 3600L), prefs.customPresets)
        assertTrue(prefs.sunsetModeEnabled)
        assertEquals(30L, prefs.sunsetDurationSeconds)
    }

    @Test
    fun testSaveLastDuration() = runTest(testDispatcher) {
        dataSource.saveLastDuration(600L)
        val prefs = dataSource.preferences.first()
        assertEquals(600L, prefs.lastDurationSeconds)
    }

    @Test
    fun testSaveTimerStarted() = runTest(testDispatcher) {
        dataSource.saveTimerStarted(1200L, 987654321L)
        val prefs = dataSource.preferences.first()
        assertEquals(1200L, prefs.lastDurationSeconds)
        assertEquals(987654321L, prefs.startedAtMillis)
    }

    @Test
    fun testClearActiveTimer() = runTest(testDispatcher) {
        dataSource.saveTimerStarted(1200L, 987654321L)
        dataSource.clearActiveTimer()
        val prefs = dataSource.preferences.first()
        assertEquals(0L, prefs.startedAtMillis)
    }

    @Test
    fun testSaveCommitmentMode() = runTest(testDispatcher) {
        dataSource.saveCommitmentMode(true)
        var prefs = dataSource.preferences.first()
        assertTrue(prefs.commitmentMode)

        dataSource.saveCommitmentMode(false)
        prefs = dataSource.preferences.first()
        assertFalse(prefs.commitmentMode)
    }

    @Test
    fun testSaveCustomPresets() = runTest(testDispatcher) {
        dataSource.saveCustomPresets(listOf(10L, 20L, 45L, 90L))
        val prefs = dataSource.preferences.first()
        // Save format maps minutes list to comma-separated string,
        // which the flow parses by multiplying minutes by 60 to get seconds:
        assertEquals(listOf(600L, 1200L, 2700L, 5400L), prefs.customPresets)
    }

    @Test
    fun testSaveSunsetMode() = runTest(testDispatcher) {
        dataSource.saveSunsetMode(false)
        var prefs = dataSource.preferences.first()
        assertFalse(prefs.sunsetModeEnabled)

        dataSource.saveSunsetMode(true)
        prefs = dataSource.preferences.first()
        assertTrue(prefs.sunsetModeEnabled)
    }

    @Test
    fun testSaveSunsetDuration() = runTest(testDispatcher) {
        dataSource.saveSunsetDuration(45L)
        val prefs = dataSource.preferences.first()
        assertEquals(45L, prefs.sunsetDurationSeconds)
    }

    @Test
    fun testMalformedCustomPresetsFallback() = runTest(testDispatcher) {
        // Manually write a malformed custom presets string directly to the DataStore
        dataStore.edit { prefs ->
            prefs[TimerPreferenceKeys.CUSTOM_PRESETS] = "invalid,presets,values"
        }
        val prefs = dataSource.preferences.first()
        // Should fallback to default custom presets: listOf(300L, 900L, 1800L, 3600L)
        assertEquals(listOf(300L, 900L, 1800L, 3600L), prefs.customPresets)
    }
}
