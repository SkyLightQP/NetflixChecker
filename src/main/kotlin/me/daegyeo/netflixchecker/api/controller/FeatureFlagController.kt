package me.daegyeo.netflixchecker.api.controller

import me.daegyeo.netflixchecker.api.controller.request.SetFeatureFlagRequest
import me.daegyeo.netflixchecker.api.service.FeatureFlagService
import me.daegyeo.netflixchecker.shared.annotation.CheckAuth
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/feature-flag")
class FeatureFlagController(
    private val featureFlagService: FeatureFlagService
) {

    @CheckAuth
    @GetMapping
    fun getFeatureFlags(): Map<String, Map<String, String>> {
        return mapOf("result" to featureFlagService.getFeatureFlags())
    }

    @CheckAuth
    @PostMapping
    fun setFeatureFlag(@RequestBody request: SetFeatureFlagRequest): ResponseEntity<Unit> {
        featureFlagService.setFeatureFlag(request.key, request.value)
        return ResponseEntity.ok().build()
    }
}