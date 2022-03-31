# NetflixChecker (넷플릭스가웨이브를왓챠로티빙)

- 넷플릭스 입금 확인을 대신 해줄거에요.

## 시작하기

- Python 3.10

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