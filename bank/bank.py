import re
import time
from typing import List, Literal

from bs4 import BeautifulSoup
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

from common import logger, Config
from .model import BankModel

BANK_URL = "https://bank.shinhan.com/rib/easy/index.jsp#210000000000"
ONLY_UPPERCASE = re.compile("\b[A-Z]\b")
ButtonType = Literal["shift", "char", "normal"]
MAX_RETRY = 3
BUTTON_DELAY = 0.5


class Bank:
    __data: List[BankModel] = []
    config = Config().get_config_model()

    def __init__(self):
        chrome_options = webdriver.ChromeOptions()
        # chrome_options.add_argument("headless")
        # chrome_options.add_argument("disable-gpu")
        chrome_options.add_experimental_option("mobileEmulation", {"deviceName": "Galaxy S5"})

        if self.config.remotedriver_enable:
            driver = webdriver.Remote(self.config.remotedriver_host, chrome_options.to_capabilities())
        else:
            driver = webdriver.Chrome(executable_path="chromedriver", options=chrome_options)
        driver.get(BANK_URL)
        driver.implicitly_wait(20)

        self.driver = driver
        self.bank_id = self.config.bank_id
        self.bank_password = self.config.bank_password
        self.account_password = self.config.account_password
        self.bank_cost = self.config.bank_cost

        self.retry = 0

    def __click_button(self, type: ButtonType, value):
        if type == "shift":
            self.driver.find_element(By.XPATH, "//img[@alt='쉬프트']").find_element(By.XPATH, "./../..").click()
            time.sleep(0.5)
            self.driver.find_element(By.XPATH, f"//img[@alt='대문자{value}']").find_element(By.XPATH, "./../..").click()
            time.sleep(0.5)
            self.driver.find_element(By.XPATH, "//img[@alt='쉬프트']").find_element(By.XPATH, "./../..").click()
        if type == "char":
            self.driver.find_element(By.XPATH, "//img[@alt='특수키']").find_element(By.XPATH, "./../..").click()
            time.sleep(0.5)
            self.driver.find_element(By.XPATH, f"//img[@alt='{value}']").find_element(By.XPATH, "./../..").click()
            time.sleep(0.5)
            self.driver.find_element(By.XPATH, "//img[@alt='특수키']").find_element(By.XPATH, "./../..").click()
        if type == "normal":
            self.driver.find_element(By.XPATH, f"//img[@alt='{value}']").find_element(By.XPATH, "./../..").click()

    def login(self):
        logger.info("[BANK] Start bank login.")

        WebDriverWait(self.driver, 20).until(EC.element_to_be_clickable((By.ID, 'btn_idLogin')))

        self.driver.find_element(By.XPATH, '//*[@id="ibx_loginId"]').send_keys(self.bank_id)
        self.driver.find_element(By.XPATH, '//*[@id="비밀번호"]').click()

        for i in self.bank_password:
            if i.isupper():
                self.__click_button("shift", i)
            elif i == '*':
                self.__click_button("char", "별표")
            elif i == '!':
                self.__click_button("char", "느낌표")
            elif i == '?':
                self.__click_button("char", "물음표")
            elif i == '~':
                self.__click_button("char", "물결표시")
            elif i == '#':
                self.__click_button("char", "우물정")
            elif i == '@':
                self.__click_button("char", "골뱅이")
            else:
                self.__click_button("normal", i)
            time.sleep(BUTTON_DELAY)

        self.driver.find_element(By.XPATH, '//*[@id="mtk_done"]').click()
        self.driver.find_element(By.ID, 'btn_idLogin').click()

        WebDriverWait(self.driver, 20).until(EC.element_to_be_clickable((By.XPATH, '//*[@id="btn_alertLayer_yes"]')))
        self.driver.find_element(By.XPATH, '//*[@id="btn_alertLayer_yes"]').click()

        logger.info("[BANK] Completed login successfully.")

    def select_account(self):
        logger.info("[BANK] Start select bank account.")

        self.driver.find_element(By.XPATH, '//*[@id="wq_uuid_852"]').click()
        self.driver.find_element(By.XPATH, '//*[@id="sbx_accno_input_0"]/option[2]').click()

        self.driver.find_element(By.XPATH, '//*[@id="계좌비밀번호"]').click()

        for i in self.account_password:
            self.__click_button("normal", i)
            time.sleep(BUTTON_DELAY)

        self.driver.find_element(By.XPATH, '//*[@id="wfr_searchCalendar_rad_gigan"]/div[3]').click()  # 1주일
        self.driver.find_element(By.XPATH, '//*[@id="btn_조회"]').click()

        logger.info("[BANK] Selected bank account successfully.")

    def refresh(self):
        try:
            logger.info(f"[BANK] Fetch bank data of account.")

            self.__data = []
            original = self.driver.find_elements(By.CSS_SELECTOR, '#F01_grd_list_body_tbody > tr')
            for i in range(0, len(original)):
                self.driver.find_element(By.XPATH, f'//*[@id="F01_grd_list_cell_{i}_11"]/nobr/a').click()
                self.driver.switch_to.frame(
                    self.driver.find_element(By.XPATH, '/html/body/div[7]/div[2]/div[1]/iframe'))
                html = self.driver.find_element(By.XPATH, '//*[@id="M01_gen0"]').get_attribute('innerHTML')

                bs = BeautifulSoup(html, 'html.parser')
                date = bs.select_one('#wq_uuid_41 > span').get_text()
                ttime = bs.select_one('#wq_uuid_46 > span').get_text()
                cost = bs.select_one('#wq_uuid_61 > span').get_text()
                who = bs.select_one('#wq_uuid_66 > span').get_text()

                if cost != 0 and cost == self.bank_cost:
                    self.__data.append(BankModel(ttime, date, cost, who))

                self.driver.find_element(By.XPATH, '//*[@id="btn_목록보기"]').click()
                self.driver.switch_to.default_content()

            logger.info(f"[BANK] Fetched bank data successfully. length={len(self.__data)}")
        except Exception as ex:
            if self.retry == MAX_RETRY:
                logger.error(f"[BANK] Failed during bank crawling. do not try anymore. - {ex.msg}")
                return
            self.retry += 1
            logger.error(f"[BANK] Failed during bank crawling. retry ({self.retry}/{MAX_RETRY})")
            self.refresh()

    def renewal(self):
        logger.info(f"[BANK] Renewal login.")
        self.driver.find_element(By.XPATH, '//*[@id="wq_uuid_1118"]').click()
        self.driver.find_element(By.XPATH, '//*[@id="wq_uuid_1139"]').click()
        self.driver.find_element(By.XPATH, '//*[@id="wq_uuid_1126"]').click()
        logger.info(f"[BANK] Renewed login successfully.")

    def get_data(self) -> List[BankModel]:
        return self.__data
