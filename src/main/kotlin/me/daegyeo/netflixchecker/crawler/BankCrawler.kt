package me.daegyeo.netflixchecker.crawler

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.daegyeo.netflixchecker.config.FeatureFlagConfiguration
import me.daegyeo.netflixchecker.config.VaultConfiguration
import me.daegyeo.netflixchecker.data.AccountData
import me.daegyeo.netflixchecker.enum.FeatureFlagKey
import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import kotlin.math.round
import kotlin.time.Duration.Companion.milliseconds


enum class ButtonType {
    SHIFT, CHAR, NORMAL
}

@Component
class BankCrawler(
    private val featureFlagConfiguration: FeatureFlagConfiguration
) {
    private val BANK_URL = "https://bank.shinhan.com/rib/easy/index.jsp#210000000000"
    private val WAIT_SECONDS: Long = 8
    private val BUTTON_WAIT_SECONDS: Long = 1
    private val DEPOSIT_DETAIL_FRAME = By.xpath("/html/body/div[7]/div[2]/div[1]/iframe")
    private val DEPOSIT_LIST_BUTTON = By.xpath("//*[@id=\"btn_목록보기\"]")

    private val logger = LoggerFactory.getLogger(BankCrawler::class.java)

    private fun delayUntilClickable(driver: WebDriver, seconds: Long, locator: By) =
        WebDriverWait(driver, Duration.ofSeconds(seconds)).until(ExpectedConditions.elementToBeClickable(locator))

    private fun clickSecureButton(driver: WebDriver, buttonType: ButtonType, value: String) {
        val keyXPath = By.xpath("//a[@aria-label='$value']")
        val uppercaseKeyXPath = By.xpath("//a[@aria-label='대문자${value.uppercase()}']")
        val specialKeyXPath = By.xpath("//a[@aria-label='특수키']")
        val shiftKeyXPath = By.xpath("//a[@aria-label='쉬프트']")

        when (buttonType) {
            ButtonType.SHIFT -> {
                delayUntilClickable(driver, BUTTON_WAIT_SECONDS, shiftKeyXPath)
                driver.findElement(shiftKeyXPath).click()
                delayUntilClickable(driver, BUTTON_WAIT_SECONDS, uppercaseKeyXPath)
                driver.findElement(uppercaseKeyXPath).click()
                delayUntilClickable(driver, BUTTON_WAIT_SECONDS, shiftKeyXPath)
                driver.findElement(shiftKeyXPath).click()
            }

            ButtonType.CHAR -> {
                delayUntilClickable(driver, BUTTON_WAIT_SECONDS, specialKeyXPath)
                driver.findElement(specialKeyXPath).click()
                delayUntilClickable(driver, BUTTON_WAIT_SECONDS, keyXPath)
                driver.findElement(keyXPath).click()
                delayUntilClickable(driver, BUTTON_WAIT_SECONDS, specialKeyXPath)
                driver.findElement(specialKeyXPath).click()
            }

            ButtonType.NORMAL -> {
                delayUntilClickable(driver, BUTTON_WAIT_SECONDS, keyXPath)
                driver.findElement(keyXPath).click()
            }
        }
    }

    fun login(driver: WebDriver) {
        driver.get(BANK_URL)

        WebDriverWait(driver, Duration.ofSeconds(WAIT_SECONDS)).until(
            ExpectedConditions.elementToBeClickable(By.id("btn_idLogin"))
        )

        driver.findElement(By.xpath("//*[@id=\"ibx_loginId\"]")).sendKeys(VaultConfiguration.BANK_SITE_ID)
        driver.findElement(By.xpath("//*[@id=\"비밀번호\"]")).click()

        WebDriverWait(
            driver, Duration.ofSeconds(WAIT_SECONDS)
        ).until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"mtk_비밀번호\"]")))
        driver.findElement(By.xpath("//a[@aria-label='재배열']")).click()
        VaultConfiguration.BANK_SITE_PASSWORD.forEach {
            runBlocking {
                delay((BUTTON_WAIT_SECONDS * 1000).milliseconds)
            }
            when {
                it.isUpperCase() -> clickSecureButton(driver, ButtonType.SHIFT, it.toString())
                it == '*' -> clickSecureButton(driver, ButtonType.CHAR, "별표")
                it == '!' -> clickSecureButton(driver, ButtonType.CHAR, "느낌표")
                it == '?' -> clickSecureButton(driver, ButtonType.CHAR, "물음표")
                it == '~' -> clickSecureButton(driver, ButtonType.CHAR, "물결표시")
                it == '#' -> clickSecureButton(driver, ButtonType.CHAR, "우물정")
                it == '@' -> clickSecureButton(driver, ButtonType.CHAR, "골뱅이")
                else -> clickSecureButton(driver, ButtonType.NORMAL, it.toString())
            }
        }

        driver.findElement(By.xpath("/html/body/div[5]/div[6]/a[3]")).click() // 보안키보드 입력 완료
        driver.findElement(By.id("btn_idLogin")).click()

        delayUntilClickable(driver, BUTTON_WAIT_SECONDS, By.xpath("//*[@id=\"btn_alertLayer_yes\"]"))

        driver.findElement(By.xpath("//*[@id=\"btn_alertLayer_yes\"]")).click()

        logger.info("은행 로그인을 완료했습니다.")
    }

    fun selectAccount(driver: WebDriver, isRetry: Boolean) {
        if (isRetry) {
            driver.switchTo().defaultContent()
            driver.get(BANK_URL)
        }

        driver.findElement(By.xpath("//*[@class=\"w2textbox mt5\"]")).click()
        driver.findElement(By.xpath("//*[@id=\"sbx_accno_input_0\"]/option[2]")).click()

        runBlocking { delay((BUTTON_WAIT_SECONDS * 1000).milliseconds) }
        driver.findElement(By.xpath("//*[@id=\"계좌비밀번호\"]")).click()

        WebDriverWait(
            driver, Duration.ofSeconds(WAIT_SECONDS)
        ).until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"mtk_계좌비밀번호\"]")))
        VaultConfiguration.BANK_ACCOUNT_PASSWORD.forEach {
            runBlocking {
                delay((BUTTON_WAIT_SECONDS * 1000).milliseconds)
            }
            clickSecureButton(driver, ButtonType.NORMAL, it.toString())
        }

        driver.findElement(By.xpath("//*[@id=\"wfr_searchCalendar_rad_gigan\"]/div[3]")).click()
        driver.findElement(By.xpath("//*[@id=\"btn_조회\"]")).click()

        logger.info("은행 계좌를 찾았습니다.")
    }

    private fun closeDepositDetailIfOpen(driver: WebDriver) {
        driver.switchTo().defaultContent()
        val detailFrame = driver.findElements(DEPOSIT_DETAIL_FRAME).firstOrNull { it.isDisplayed } ?: return

        driver.switchTo().frame(detailFrame)
        driver.findElements(DEPOSIT_LIST_BUTTON).firstOrNull()?.click()
        driver.switchTo().defaultContent()
    }

    fun readDeposits(driver: WebDriver, isRetry: Boolean): List<AccountData> {
        if (isRetry) {
            closeDepositDetailIfOpen(driver)
        }

        val result = arrayListOf<AccountData>()
        val original = driver.findElements(By.cssSelector("#F01_grd_list_body_tbody > tr"))

        for (i in 0 until original.size) {
            val elementXPath = By.xpath("//*[@id=\"F01_grd_list_cell_${i}_11\"]/nobr/a")
            delayUntilClickable(driver, BUTTON_WAIT_SECONDS, elementXPath)
            driver.findElement(elementXPath).click()

            driver.switchTo().frame(driver.findElement(DEPOSIT_DETAIL_FRAME))
            val accountDetailXPath = By.xpath("//*[@id=\"M01_gen0\"]")
            delayUntilClickable(driver, WAIT_SECONDS, accountDetailXPath)
            val accountDetail = driver.findElement(accountDetailXPath)
            val accountDetailHtml = Jsoup.parse(accountDetail.getDomProperty("innerHTML").orEmpty()).body()
            val date = accountDetailHtml.selectXpath("span[2]").text()
            val time = accountDetailHtml.selectXpath("span[4]").text()
            val cost = accountDetailHtml.selectXpath("span[10]").text()
            val normalizeCost = cost.replace(",", "").toInt()
            val who = accountDetailHtml.selectXpath("span[12]").text()
            val targetNames = VaultConfiguration.DEPOSIT_TARGET_NAMES
                .replace("[", "").replace("]", "").replace("\"", "")
                .split(", ")

            if (who in targetNames && normalizeCost != 0) {
                val costMonth =
                    round(normalizeCost / featureFlagConfiguration.getInt(FeatureFlagKey.BANK_COST).toDouble()).toInt()
                        .toString()
                result.add(AccountData(time, date, normalizeCost.toString(), who, costMonth))
            }

            driver.findElement(DEPOSIT_LIST_BUTTON).click()
            driver.switchTo().defaultContent()
        }

        if (result.isEmpty()) {
            logger.info("입금 정보가 없습니다.")
        } else {
            logger.info("입금 정보 ${result.size}건를 찾았습니다.")
        }

        return result
    }
}
