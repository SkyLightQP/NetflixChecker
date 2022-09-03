# NetflixChecker ![works badge](https://cdn.jsdelivr.net/gh/nikku/works-on-my-machine@v0.2.0/badge.svg)

> **정상적인 동작을 책임지지 않습니다. 민감한 개인정보를 config.json에 평문으로 저장하고 있습니다.**

넷플릭스 입금 확인을 대신 해줄거에요.

은행 크롤링 후 입금 확인 시 디스코드로 알림을 보내줍니다.

## 시작하기

- Python 3.10
- Chromedriver

```shell
pip install -r requirements.txt
```

```shell
cp config.json.example config.json
```

## 데이터베이스 구축하기 

중복으로 입금 확인을 하지 않기 위해 데이터베이스를 사용합니다.

```shell
cat > data.db
```

```sql
CREATE TABLE data (
    date TEXT,
    name TEXT
);
```

## TODO

- [x] 프로덕션 환경에서 크롤링 실패하는 경우가 매우 많음
  - 현재는 크롤링 Retry 기능을 이용해 보완 중
- [x] 매번 로그인을 안하고 데이터 크롤링이 가능하도록 변경
- [x] 매주 입금 내역 DM으로 리포팅
- [ ] 민감한 정보을 안전하게 저장하도록 변경
