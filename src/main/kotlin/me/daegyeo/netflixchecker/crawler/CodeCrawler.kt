package me.daegyeo.netflixchecker.crawler

import me.daegyeo.netflixchecker.config.Pop3Configuration
import me.daegyeo.netflixchecker.enum.MetricsKey
import me.daegyeo.netflixchecker.table.Metrics
import org.jetbrains.exposed.sql.IntegerColumnType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.castTo
import org.jetbrains.exposed.sql.upsert
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*
import javax.mail.*
import javax.mail.internet.MimeBodyPart


data class VerificationCode(val text: String, val link: String)

@Component
class CodeCrawler(private val pop3Configuration: Pop3Configuration) {
    private val logger = LoggerFactory.getLogger(CodeCrawler::class.java)

    private val EMAIL_SUBJECT = "회원님의 넷플릭스 임시 접속 코드"
    private val EMAIL_FROM = "Netflix <info@account.netflix.com>"

    private fun createMailClient(): Store {
        val properties = Properties()
        properties["mail.pop3.host"] = pop3Configuration.host
        properties["mail.pop3.port"] = pop3Configuration.port
        properties["mail.pop3.starttls.enable"] = "true"
        properties["mail.pop3.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
        val emailSession = Session.getDefaultInstance(properties, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(pop3Configuration.user, pop3Configuration.password)
            }
        })
        val store = emailSession.getStore("pop3")
        return store
    }

    fun getVerificationCode(): VerificationCode {
        val store = createMailClient()
        store.connect(pop3Configuration.host, pop3Configuration.user, pop3Configuration.password)
        val inbox = store.getFolder("INBOX")
        inbox.open(Folder.READ_ONLY)
        val messages = inbox.messages


        for (i in messages.size - 1 downTo (messages.size - 10).coerceAtLeast(0)) {
            val message = messages[i]
            val content = message.content
            if (message.subject != EMAIL_SUBJECT || message.from[0].toString() != EMAIL_FROM) {
                continue
            }
            if (!content.toString().contains("Multipart")) {
                continue
            }

            val convertedContent = convertMultipartToString(content as Multipart)
            val html = Jsoup.parse(convertedContent).body()
            val text =
                html.selectXpath("table/tbody/tr/td/table/tbody/tr[2]/td/table[5]/tbody/tr/td/table/tbody/tr/td/table[1]/tbody/tr[2]/td[3]/table/tbody/tr/td")
                    .text()
            val link =
                html.selectXpath("table/tbody/tr/td/table/tbody/tr[2]/td/table[5]/tbody/tr/td/table/tbody/tr/td/table[2]/tbody/tr/td/table/tbody/tr/td/a")
                    .attr("href")

            logger.info("넷플릭스 인증코드를 발견했습니다.")

            inbox.close(false)
            store.close()

            Metrics.upsert(
                Metrics.key,
                onUpdate = listOf(Metrics.value to Metrics.value.castTo(IntegerColumnType()).plus(1)),
                where = { Metrics.key eq MetricsKey.CODE_GENERATED_COUNT.name }
            ) {
                it[key] = MetricsKey.CODE_GENERATED_COUNT.name
                it[value] = "1"
            }

            return VerificationCode(text, link)
        }

        inbox.close(false)
        store.close()

        return VerificationCode("", "")
    }

    private fun convertMultipartToString(data: Multipart): String {
        var content = ""
        for (i in 0 until data.count) {
            val bp = data.getBodyPart(i) as MimeBodyPart
            if (bp.contentType.contains("text/html")) {
                content = bp.content.toString()
            }
        }
        return content
    }
}