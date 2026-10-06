package com.alvia.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.reflect.KClass

/** Read-only bridge. Never estimates a vital sign from the phone or missing data. */
internal object HealthBridge {
    private val types = listOf(HeartRateRecord::class, BloodPressureRecord::class,
        OxygenSaturationRecord::class, BodyTemperatureRecord::class,
        RespiratoryRateRecord::class, BloodGlucoseRecord::class, WeightRecord::class)
    @JvmStatic fun availability(c: Context) = HealthConnectClient.getSdkStatus(c)
    @JvmStatic fun available(c: Context) = availability(c) == HealthConnectClient.SDK_AVAILABLE
    @JvmStatic fun permissions(): Set<String> = types.map { HealthPermission.getReadPermission(it) }.toSet()
    /** Called off the UI thread, including when Android granted access in Settings. */
    @JvmStatic fun granted(c: Context): Set<String> = runBlocking { HealthConnectClient.getOrCreate(c).permissionController.getGrantedPermissions() }
    @JvmStatic fun backgroundAvailable(c: Context) = available(c) && HealthConnectClient.getOrCreate(c).features.getFeatureStatus(
        HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
    @JvmStatic fun sync(c: Context, background: Boolean): Int = runBlocking {
        if (!available(c)) return@runBlocking 0
        val vault = Vault(c)
        HealthProfile.migrateWeights(vault)
        val cloud = FirebaseCloud.create(c)
        val source = SourceSync.setting(vault, "health")
        if (!cloud.account() || !source.optBoolean("enabled") || cloud.owner() != source.optString("owner")) return@runBlocking 0
        val client = HealthConnectClient.getOrCreate(c)
        val granted = client.permissionController.getGrantedPermissions()
        if (background && !granted.contains(HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND)) return@runBlocking 0
        if (types.none { granted.contains(HealthPermission.getReadPermission(it)) })
            throw SecurityException("Autoriza al menos un tipo de medición en Health Connect.")
        val now = Instant.now()
        var count = 0
        suspend fun <T: Record> read(type: KClass<T>) {
            if (!granted.contains(HealthPermission.getReadPermission(type))) return
            var page: String? = null
            do {
                val response = client.readRecords(ReadRecordsRequest(type,
                    timeRangeFilter = TimeRangeFilter.between(now.minus(30, ChronoUnit.DAYS), now), pageToken = page))
                val batch = mutableListOf<JSONObject>()
                for (record in response.records) {
                    if (!cloud.account() || cloud.owner() != source.optString("owner") ||
                        !SourceSync.setting(vault,"health").optBoolean("enabled")) return
                    fun save(metric: Int, time: Instant, value: Double) {
                        if (!value.isFinite() || value <= 0) return
                        val key = SourceSync.hash((record.metadata.id+":"+metric+":"+time).toByteArray())
                        val item = JSONObject().put("id","hc-$key").put("created",time.toEpochMilli())
                            .put("metric",metric).put("value",value).put("origin","Health Connect")
                            .put("sourcePackage",record.metadata.dataOrigin.packageName)
                            .put("sourceRecord",record.metadata.id).put("owner",cloud.owner())
                        if (metric == 7) {
                            if (HealthProfile.saveWeight(vault,item)) count++
                        } else batch.add(item)
                    }
                    when(record) {
                        is HeartRateRecord -> record.samples.forEach { save(0,it.time,it.beatsPerMinute.toDouble()) }
                        is BloodPressureRecord -> {save(1,record.time,record.systolic.inMillimetersOfMercury);save(2,record.time,record.diastolic.inMillimetersOfMercury)}
                        is OxygenSaturationRecord -> save(3,record.time,record.percentage.value)
                        is BodyTemperatureRecord -> save(4,record.time,record.temperature.inCelsius)
                        is RespiratoryRateRecord -> save(5,record.time,record.rate)
                        is BloodGlucoseRecord -> save(6,record.time,record.level.inMilligramsPerDeciliter)
                        is WeightRecord -> save(7,record.time,record.weight.inKilograms)
                    }
                }
                count += SourceSync.saveVitals(vault,batch)
                page = response.pageToken
            } while(page != null)
        }
        for(type in types) read(type)
        if (cloud.account() && cloud.owner() == source.optString("owner")) {
            HealthProfile.syncWeight(vault,cloud.owner())
            try { cloud.syncProfile(vault) } catch (_: Exception) { /* Pending profile retries on the next sync. */ }
        }
        count
    }
}
