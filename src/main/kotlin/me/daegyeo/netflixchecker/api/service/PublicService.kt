package me.daegyeo.netflixchecker.api.service

import me.daegyeo.netflixchecker.crawler.CodeCrawler
import me.daegyeo.netflixchecker.crawler.VerificationCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PublicService(private val codeCrawler: CodeCrawler) {

    private val logger = LoggerFactory.getLogger(PublicService::class.java)

    fun getCode(): VerificationCode {
        logger.info("인증코드를 공개 API를 통해 가져옵니다.")
        return codeCrawler.getVerificationCode()
    }
}