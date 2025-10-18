package me.daegyeo.netflixchecker.crawler

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.daegyeo.netflixchecker.config.BankConfiguration
import me.daegyeo.netflixchecker.config.SeleniumConfiguration
import me.daegyeo.netflixchecker.config.VaultConfiguration
import me.daegyeo.netflixchecker.data.AccountData
import me.daegyeo.netflixchecker.enum.MetricsKey
import me.daegyeo.netflixchecker.table.Metrics
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URL
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.round


enum class ButtonType {
    SHIFT, CHAR, NORMAL
}

@Component
class BankCrawler(
    private val seleniumConfiguration: SeleniumConfiguration,
    private val bankConfiguration: BankConfiguration
) {
    private val DRIVER_NAME = "webdriver.chrome.driver"
    private val DRIVER_PATH = "chromedriver.exe"
    private val BANK_URL = "https://bank.shinhan.com/rib/easy/index.jsp#210000000000"
    private val WAIT_SECONDS: Long = 8
    private val BUTTON_WAIT_SECONDS: Long = 1

    lateinit var driver: WebDriver

    private val logger = LoggerFactory.getLogger(BankCrawler::class.java)

    fun openBrowser() {
        val isRemote = seleniumConfiguration.useRemote
        val option = ChromeOptions()
        if (seleniumConfiguration.useHeadless) {
            option.addArguments("--headless=new")
            option.addArguments("--disable-gpu")
        }

        val mobileEmulation = hashMapOf("deviceName" to "Samsung Galaxy S20 Ultra")
        option.setExperimentalOption("mobileEmulation", mobileEmulation)

        driver = if (!isRemote) {
            System.setProperty(DRIVER_NAME, DRIVER_PATH)
            ChromeDriver(option)
        } else {
            RemoteWebDriver(URL(seleniumConfiguration.host), option)
        }

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(WAIT_SECONDS))
    }

    fun closeBrowser() = driver.quit()

    private fun delayUntilClickable(seconds: Long, locator: By) =
        WebDriverWait(driver, Duration.ofSeconds(seconds)).until(ExpectedConditions.elementToBeClickable(locator))

    private fun clickSecureButton(buttonType: ButtonType, value: String) {
        val keyXPath = By.xpath("//a[@aria-label='$value']")
        val uppercaseKeyXPath = By.xpath("//a[@aria-label='대문자${value.uppercase()}']")
        val specialKeyXPath = By.xpath("//a[@aria-label='특수키']")
        val shiftKeyXPath = By.xpath("//a[@aria-label='쉬프트']")

        when (buttonType) {
            ButtonType.SHIFT -> {
                delayUntilClickable(BUTTON_WAIT_SECONDS, shiftKeyXPath)
                driver.findElement(shiftKeyXPath).click()
                delayUntilClickable(BUTTON_WAIT_SECONDS, uppercaseKeyXPath)
                driver.findElement(uppercaseKeyXPath).click()
                delayUntilClickable(BUTTON_WAIT_SECONDS, shiftKeyXPath)
                driver.findElement(shiftKeyXPath).click()
            }

            ButtonType.CHAR -> {
                delayUntilClickable(BUTTON_WAIT_SECONDS, specialKeyXPath)
                driver.findElement(specialKeyXPath).click()
                delayUntilClickable(BUTTON_WAIT_SECONDS, keyXPath)
                driver.findElement(keyXPath).click()
                delayUntilClickable(BUTTON_WAIT_SECONDS, specialKeyXPath)
                driver.findElement(specialKeyXPath).click()
            }

            ButtonType.NORMAL -> {
                delayUntilClickable(BUTTON_WAIT_SECONDS, keyXPath)
                driver.findElement(keyXPath).click()
            }
        }
    }

    private fun loginBank() {
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
                delay(BUTTON_WAIT_SECONDS * 1000)
            }
            when {
                it.isUpperCase() -> clickSecureButton(ButtonType.SHIFT, it.toString())
                it == '*' -> clickSecureButton(ButtonType.CHAR, "별표")
                it == '!' -> clickSecureButton(ButtonType.CHAR, "느낌표")
                it == '?' -> clickSecureButton(ButtonType.CHAR, "물음표")
                it == '~' -> clickSecureButton(ButtonType.CHAR, "물결표시")
                it == '#' -> clickSecureButton(ButtonType.CHAR, "우물정")
                it == '@' -> clickSecureButton(ButtonType.CHAR, "골뱅이")
                else -> clickSecureButton(ButtonType.NORMAL, it.toString())
            }
        }

        driver.findElement(By.xpath("/html/body/div[5]/div[6]/a[3]")).click() // 보안키보드 입력 완료
        driver.findElement(By.id("btn_idLogin")).click()

        delayUntilClickable(BUTTON_WAIT_SECONDS, By.xpath("//*[@id=\"btn_alertLayer_yes\"]"))

        driver.findElement(By.xpath("//*[@id=\"btn_alertLayer_yes\"]")).click()

        logger.info("은행 로그인을 완료했습니다.")
    }

    private fun selectBankAccount() {
        driver.findElement(By.xpath("//*[@class=\"w2textbox mt5\"]")).click()
        driver.findElement(By.xpath("//*[@id=\"sbx_accno_input_0\"]/option[2]")).click()

        runBlocking { delay(BUTTON_WAIT_SECONDS * 1000) }
        driver.findElement(By.xpath("//*[@id=\"계좌비밀번호\"]")).click()

        WebDriverWait(
            driver, Duration.ofSeconds(WAIT_SECONDS)
        ).until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"mtk_계좌비밀번호\"]")))
        VaultConfiguration.BANK_ACCOUNT_PASSWORD.forEach {
            runBlocking {
                delay(BUTTON_WAIT_SECONDS * 1000)
            }
            clickSecureButton(ButtonType.NORMAL, it.toString())
        }

        driver.findElement(By.xpath("//*[@id=\"wfr_searchCalendar_rad_gigan\"]/div[3]")).click()
        driver.findElement(By.xpath("//*[@id=\"btn_조회\"]")).click()

        logger.info("은행 계좌를 찾았습니다.")
    }

    private fun getAccountData(): List<AccountData> {
        val result = arrayListOf<AccountData>()
        val original = driver.findElements(By.cssSelector("#F01_grd_list_body_tbody > tr"))

        for (i in 0 until original.size) {
            val elementXPath = By.xpath("//*[@id=\"F01_grd_list_cell_${i}_11\"]/nobr/a")
            delayUntilClickable(BUTTON_WAIT_SECONDS, elementXPath)
            driver.findElement(elementXPath).click()

            driver.switchTo().frame(driver.findElement(By.xpath("/html/body/div[7]/div[2]/div[1]/iframe")))
            val accountDetailXPath = By.xpath("//*[@id=\"M01_gen0\"]")
            delayUntilClickable(WAIT_SECONDS, accountDetailXPath)
            val accountDetail = driver.findElement(accountDetailXPath)
            val accountDetailHtml = Jsoup.parse(accountDetail.getAttribute("innerHTML")).body()
            val date = accountDetailHtml.selectXpath("span[2]").text()
            val time = accountDetailHtml.selectXpath("span[4]").text()
            val cost = accountDetailHtml.selectXpath("span[10]").text()
            val normalizeCost = cost.replace(",", "").toInt()
            val who = accountDetailHtml.selectXpath("span[12]").text()
            val targetNames = VaultConfiguration.DEPOSIT_TARGET_NAMES
                .replace("[", "").replace("]", "").replace("\"", "")
                .split(", ")

            if (who in targetNames && normalizeCost != 0) {
                val costMonth = round(normalizeCost / bankConfiguration.cost.toDouble()).toInt().toString()
                result.add(AccountData(time, date, normalizeCost.toString(), who, costMonth))
            }

            driver.findElement(By.xpath("//*[@id=\"btn_목록보기\"]")).click()
            driver.switchTo().defaultContent()
        }

        if (result.isEmpty()) {
            logger.info("입금 정보가 없습니다.")
        } else {
            logger.info("입금 정보 ${result.size}건를 찾았습니다.")
        }

        return result
    }

    fun crawl(): List<AccountData> {
        val formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Seoul"))

        try {
            driver.get(BANK_URL)
            loginBank()
            selectBankAccount()
            val result = getAccountData()
            transaction {
                Metrics.upsert(Metrics.key, where = { Metrics.key eq MetricsKey.LATEST_CRAWLING_STATUS.name }) {
                    it[key] = MetricsKey.LATEST_CRAWLING_STATUS.name
                    it[value] = "O"
                }
                Metrics.upsert(Metrics.key, where = { Metrics.key eq MetricsKey.LATEST_CRAWLING_TIME.name }) {
                    it[key] = MetricsKey.LATEST_CRAWLING_TIME.name
                    it[value] = LocalDateTime.now().format(formatter)
                }
            }
            return result
        } catch (e: Exception) {
            logger.error("은행 크롤링 중 오류가 발생했습니다. 브라우저를 닫습니다.", e)
            closeBrowser()
            transaction {
                Metrics.upsert(Metrics.key, where = { Metrics.key eq MetricsKey.LATEST_CRAWLING_STATUS.name }) {
                    it[key] = MetricsKey.LATEST_CRAWLING_STATUS.name
                    it[value] = "X"
                }
            }
            return emptyList()
        }
    }
}
