# NetflixChecker [![Made with Supabase](https://supabase.com/badge-made-with-supabase.svg)](https://supabase.com) ![works badge](https://cdn.jsdelivr.net/gh/nikku/works-on-my-machine@v0.2.0/badge.svg) ![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?logo=kotlin&logoColor=white) ![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?logo=spring&logoColor=white)

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
- 보안키보드 문제로 인해 100% 작동을 보장하지 않습니다.

## 시작하기

- NetflixChecker는 인증 및 인가, 데이터베이스, 설정 관리를 위해 **Supabase**를 사용하고 있습니다.
- 시작 전 Supabase 프로젝트를 생성해주세요.
- 크롤링을 위해 `ChromeDriver` 또는 Remote Selenium 서버가 필요합니다.
- 오류 수집을 위해 **Sentry**가 필요합니다.

```shell
cp .env.example .env
```

```shell
docker build -t netflixchecker .

docker run --env-file=.env -v ./data:/workspace/data --name netflixchecker -d netflixchecker 
```

## 환경변수

```dotenv
# 디스코드 봇 설정
DISCORD_BOT_TOKEN=
DISCORD_CHANNEL_ID=
DISCORD_ADMIN_ID=

# 은행 계정 및 계좌 정보
BANK_COST=

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

# Supabase API
SUPABASE_URL=
# NOT ANON Key, Use Service Role Key
SUPABASE_SECRET_KEY=
```

## 민감한 정보 관리하기

- `입급 대상자 이름`, `은행 정보`, `계좌 정보`는 [Supabase Vault](https://supabase.com/docs/guides/database/vault)를 사용해서 관리합니다.
- 아래 값을 Supabase Vault에 추가해주세요.

| Name                     | Value                                      |
|-------------------------|--------------------------------------------|
| `deposit_target_names`  | 입금 대상자 이름 (JSON-like, ex: ["홍길동", "김길동"])  |
| `bank_site_id`          | 은행 홈페이지 아이디                                |
| `bank_site_password`    | 은행 홈페이지 비밀번호                               |
| `bank_account_password` | 은행 계좌 비밀번호                                 |
