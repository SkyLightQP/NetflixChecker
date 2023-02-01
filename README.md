# NetflixChecker ![works badge](https://cdn.jsdelivr.net/gh/nikku/works-on-my-machine@v0.2.0/badge.svg)
<br/>
<div align="center">
넷플릭스 입금 확인을 대신 해줍니다

은행 크롤링 후 넷플릭스 금액 확인 시 디스코드로 알림을 보냅니다
</div>

## 시작하기 전에

- 작동여부를 보장하지 않습니다.
- 신한은행에서만 작동합니다.
- **은행 계좌번호, 비밀번호를 평문으로 저장하고 있습니다.**

## 시작하기

```shell
pip install -r requirements.txt
```

```shell
cp config.json.example config.json
```

## 데이터베이스 구축하기 

데이터베이스는 SQLite를 사용합니다. `.db` 파일을 만들면 최초 실행 시 자동으로 테이블을 생성합니다.

```shell
touch data.db
```

## TODO

- [ ] 민감한 정보을 안전하게 저장하도록 변경
- [ ] 자동배포
