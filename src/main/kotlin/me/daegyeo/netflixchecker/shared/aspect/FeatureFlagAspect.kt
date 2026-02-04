package me.daegyeo.netflixchecker.shared.aspect

import me.daegyeo.netflixchecker.api.exception.ServiceException
import me.daegyeo.netflixchecker.config.FeatureFlagConfiguration
import me.daegyeo.netflixchecker.shared.annotation.FeatureFlag
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.stereotype.Component

@Aspect
@Component
class FeatureFlagAspect(private val featureFlagConfiguration: FeatureFlagConfiguration) {

    @Before(value = "@annotation(featureFlag)")
    fun checkFeatureFlag(featureFlag: FeatureFlag) {
        val isEnabled = featureFlagConfiguration.getBoolean(featureFlag.key)
        if (!isEnabled) {
            throw ServiceException("기능을 사용할 수 없습니다.", 403)
        }
    }
}
