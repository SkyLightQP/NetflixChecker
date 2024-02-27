import email
import poplib
from email.header import decode_header, make_header

from bs4 import BeautifulSoup

from common import Config


class CodeChecker:
    config = Config().get_config_model()

    def __init__(self):
        self.pop3 = poplib.POP3_SSL(self.config.pop_host, self.config.pop_port)
        self.pop3.user(self.config.pop_user)
        self.pop3.pass_(self.config.pop_password)
        self.netflix_from = 'Netflix <info@account.netflix.com>'

    def __get_email_data(self, raw_email):
        message = email.message_from_bytes(raw_email)
        fr = make_header(decode_header(message.get('From')))
        subject = make_header(decode_header(message.get('Subject')))
        body = ''
        if message.is_multipart():
            for part in message.walk():
                content_type = part.get_content_type()
                if content_type == 'text/html':
                    body = part.get_payload(decode=True).decode(part.get_content_charset())
                    break
        else:
            content_type = message.get_content_type()
            if content_type == 'text/html':
                body = message.get_payload(decode=True).decode(message.get_content_charset())
        return fr, subject, body

    def __parse_email_body(self, body):
        bs = BeautifulSoup(body, 'html.parser')
        url = bs.select_one("td > a").get_attribute_list("href")
        return url

    def get_netflix_code_url(self):
        recent_no = self.pop3.stat()[0]

        for i in range(recent_no, recent_no - 10, -1):
            raw_email = b'\n'.join(self.pop3.retr(i)[1])
            fr, subject, body = self.__get_email_data(raw_email)

            if fr == self.netflix_from:
                url = self.__parse_email_body(body)
                return url[0]

        return None
