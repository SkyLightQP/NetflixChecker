package me.daegyeo.netflixchecker.api.service

import me.daegyeo.netflixchecker.api.exception.ServiceException
import me.daegyeo.netflixchecker.config.FeatureFlagConfiguration
import me.daegyeo.netflixchecker.enum.FeatureFlagKey
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FeatureFlagService(
    private val featureFlagConfiguration: FeatureFlagConfiguration
) {
    private val logger = LoggerFactory.getLogger(FeatureFlagService::class.java)

    fun getFeatureFlags(): Map<String, String> {
        return featureFlagConfiguration.getAllFlags()
    }

    fun setFeatureFlag(key: String, value: String) {
        val featureFlagKey = runCatching {
            enumValueOf<FeatureFlagKey>(key)
        }.getOrElse {
            throw ServiceException("올바르지 않은 FeatureFlag 입니다.", 400)
        }

        logger.info("FeatureFlag를 설정했습니다. key=$key, value=$value")

        val normalizedValue = value.lowercase()
        if (normalizedValue == "true" || normalizedValue == "false") {
            featureFlagConfiguration.setBoolean(featureFlagKey, normalizedValue.toBoolean())
            return
        }

        if (value.toIntOrNull() != null) {
            featureFlagConfiguration.setInt(featureFlagKey, value.toInt())
            return
        }

        featureFlagConfiguration.setString(featureFlagKey, value)
    }
}