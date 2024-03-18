package me.daegyeo.netflixchecker.crawler

import me.daegyeo.netflixchecker.config.BankConfiguration
import me.daegyeo.netflixchecker.config.SeleniumConfiguration
import me.daegyeo.netflixchecker.data.AccountData
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

enum class ButtonType {
    SHIFT, CHAR, NORMAL
}

@Component
class BankCrawler(
    private val seleniumConfiguration: SeleniumConfiguration, private val bankConfiguration: BankConfiguration
) {
    private val DRIVER_NAME = "webdriver.chrome.driver"
    private val DRIVER_PATH = "chromedriver.exe"
    private val BANK_URL = "https://bank.shinhan.com/rib/easy/index.jsp#210000000000"
    private val WAIT_SECONDS: Long = 10
    private val BUTTON_WAIT_SECONDS: Long = 2

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

    private fun delayUntilClickable(seconds: Long, locator: By) =
        WebDriverWait(driver, Duration.ofSeconds(seconds)).until(ExpectedConditions.elementToBeClickable(locator))

    private fun clickSecureButton(buttonType: ButtonType, value: String) {
        val keyXPath = By.xpath("//img[@alt='$value']")
        val uppercaseKeyXPath = By.xpath("//img[@alt='대문자$value']")
        val specialKeyXPath = By.xpath("//img[@alt='특수키']")
        val shiftKeyXPath = By.xpath("//img[@alt='쉬프트']")
        val prevXPath = By.xpath("./../..")

        when (buttonType) {
            ButtonType.SHIFT -> {
                delayUntilClickable(BUTTON_WAIT_SECONDS, shiftKeyXPath)
                driver.findElement(shiftKeyXPath).findElement(prevXPath).click()
                delayUntilClickable(BUTTON_WAIT_SECONDS, uppercaseKeyXPath)
                driver.findElement(uppercaseKeyXPath).findElement(prevXPath).click()
                delayUntilClickable(BUTTON_WAIT_SECONDS, shiftKeyXPath)
                driver.findElement(shiftKeyXPath).findElement(prevXPath).click()
            }

            ButtonType.CHAR -> {
                delayUntilClickable(BUTTON_WAIT_SECONDS, specialKeyXPath)
                driver.findElement(specialKeyXPath).findElement(prevXPath).click()
                delayUntilClickable(BUTTON_WAIT_SECONDS, keyXPath)
                driver.findElement(keyXPath).findElement(prevXPath).click()
                delayUntilClickable(BUTTON_WAIT_SECONDS, specialKeyXPath)
                driver.findElement(specialKeyXPath).findElement(prevXPath).click()
            }

            ButtonType.NORMAL -> {
                delayUntilClickable(BUTTON_WAIT_SECONDS, keyXPath)
                driver.findElement(keyXPath).findElement(prevXPath).click()
            }
        }
    }

    private fun loginBank() {
        WebDriverWait(driver, Duration.ofSeconds(WAIT_SECONDS)).until(
            ExpectedConditions.elementToBeClickable(By.id("btn_idLogin"))
        )

        driver.findElement(By.xpath("//*[@id=\"ibx_loginId\"]")).sendKeys(bankConfiguration.siteId)
        driver.findElement(By.xpath("//*[@id=\"비밀번호\"]")).click()

        WebDriverWait(
            driver, Duration.ofSeconds(WAIT_SECONDS)
        ).until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"mtk_비밀번호\"]")))
        bankConfiguration.sitePassword.forEach {
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

        driver.findElement(By.xpath("//*[@id=\"mtk_done\"]")).click()
        driver.findElement(By.id("btn_idLogin")).click()

        delayUntilClickable(BUTTON_WAIT_SECONDS, By.xpath("//*[@id=\"btn_alertLayer_yes\"]"))

        driver.findElement(By.xpath("//*[@id=\"btn_alertLayer_yes\"]")).click()

        logger.info("은행 로그인을 완료했습니다.")
    }

    private fun selectBankAccount() {
        driver.findElement(By.xpath("//*[@class=\"w2textbox mt5\"]")).click()
        driver.findElement(By.xpath("//*[@id=\"sbx_accno_input_0\"]/option[2]")).click()
        driver.findElement(By.xpath("//*[@id=\"계좌비밀번호\"]")).click()

        WebDriverWait(
            driver, Duration.ofSeconds(WAIT_SECONDS)
        ).until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"mtk_계좌비밀번호\"]")))
        bankConfiguration.accountPassword.forEach {
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
            val normalizeCost = cost?.replace(",", "")?.toInt()
            val who = accountDetailHtml.selectXpath("span[12]").text()

            if (normalizeCost != 0) {
                val bankCostX2 = bankConfiguration.cost * 2
                val bankCostX3 = bankConfiguration.cost * 3

                when {
                    normalizeCost == bankConfiguration.cost -> result.add(
                        AccountData(
                            time, date, normalizeCost.toString(), who, "1"
                        )
                    )

                    normalizeCost == bankCostX2 -> result.add(AccountData(time, date, normalizeCost.toString(), who, "2"))
                    normalizeCost == bankCostX3 -> result.add(AccountData(time, date, normalizeCost.toString(), who, "3"))
                }
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
        driver.get(BANK_URL)
        loginBank()
        selectBankAccount()
        return getAccountData()
    }

    fun closeBrowser() = driver.quit()
}
