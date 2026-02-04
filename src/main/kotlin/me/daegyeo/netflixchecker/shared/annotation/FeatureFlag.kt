package me.daegyeo.netflixchecker.shared.annotation

import me.daegyeo.netflixchecker.enum.FeatureFlagKey

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class FeatureFlag(
    val key: FeatureFlagKey
)
