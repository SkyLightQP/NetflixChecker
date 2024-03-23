# NetflixChecker ![works badge](https://cdn.jsdelivr.net/gh/nikku/works-on-my-machine@v0.2.0/badge.svg) ![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?logo=kotlin&logoColor=white) ![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?logo=spring&logoColor=white)

<br/>
<p align="center">
<img src="./docs/logo.png" width="200" />
</p>
<div align="center">
넷플릭스 입금 확인을 대신 해줍니다
  
은행 크롤링 후 넷플릭스 입금 및 인증코드 알림을 보냅니다
</div>

## 시작하기 전에

- 신한은행에서만 작동합니다.
- 작동여부를 보장하지 않습니다. 은행 상태에 따라 크롤링에 실패할 수 있습니다.
- **은행 계좌번호 등 민감한 정보를 평문으로 저장하고 있습니다.**

## 시작하기

- `ChromeDriver`가 필요합니다.

```shell
cp .env.example .env
```

```shell
docker build -t netflixchecker .

docker run --env-file=.env -v ./data:/workspace/data --name netflixchecker -d netflixchecker 
```

## 데이터베이스 구축하기 

H2 데이터베이스를 사용합니다.

데이터베이스 테이블이 없다면 봇 시작 시 자동으로 생성합니다.

## 환경변수

```dotenv
# 디스코드 봇 설정
DISCORD_BOT_TOKEN=
DISCORD_CHANNEL_ID=
DISCORD_ADMIN_ID=

# 은행 계정 및 계좌 정보
BANK_COST=
BANK_SITE_ID=
BANK_SITE_PASSWORD=
BANK_ACCOUNT_PASSWORD=

# Selenium 정보
SELENIUM_USE_REMOTE=false
SELENIUM_REMOTE_HOST=
SELENIUM_USE_HEADLESS=false

# POP3 정보
POP3_HOST=
POP3_PORT=995
POP3_USERNAME=
POP3_PASSWORD=

# Sentry
SENTRY_DSN=
```
