package me.daegyeo.netflixchecker.batch

import me.daegyeo.netflixchecker.config.SeleniumConfiguration
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.slf4j.LoggerFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.stereotype.Component
import java.net.URI
import java.time.Duration

@Component
@JobScope
class BankBrowserSession(
    private val seleniumConfiguration: SeleniumConfiguration
) {
    private val logger = LoggerFactory.getLogger(BankBrowserSession::class.java)

    private var driver: WebDriver? = null

    fun open() {
        close()

        val createdDriver = createDriver()
        try {
            createdDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(WAIT_SECONDS))
            driver = createdDriver
        } catch (e: Exception) {
            runCatching {
                createdDriver.quit()
            }.onFailure {
                logger.warn("브라우저 초기화 실패 후 정리 중 오류가 발생했습니다.", it)
            }
            throw e
        }
    }

    fun driver(): WebDriver = driver ?: error("Browser is not open.")

    fun close() {
        val currentDriver = driver ?: return
        driver = null

        runCatching {
            currentDriver.quit()
        }.onFailure {
            logger.warn("브라우저 종료 중 오류가 발생했습니다.", it)
        }
    }

    private fun createDriver(): WebDriver {
        val option = ChromeOptions()
        if (seleniumConfiguration.useHeadless) {
            option.addArguments("--headless=new")
            option.addArguments("--disable-gpu")
        }
        option.setExperimentalOption("mobileEmulation", hashMapOf("deviceName" to "Samsung Galaxy S20 Ultra"))

        if (seleniumConfiguration.useRemote) {
            return RemoteWebDriver(URI(seleniumConfiguration.host).toURL(), option)
        }

        System.setProperty(DRIVER_NAME, DRIVER_PATH)
        return ChromeDriver(option)
    }

    companion object {
        private const val DRIVER_NAME = "webdriver.chrome.driver"
        private const val DRIVER_PATH = "chromedriver.exe"
        private const val WAIT_SECONDS: Long = 8
    }
}
