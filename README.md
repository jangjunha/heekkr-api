# heekkr api

## Run Dependencies

```console
$ cp .env.dist .env
$ vim .env  # and edits

$ docker-compose up -d
```

## Run

```console
$ CORS_ORIGINS_0="http://localhost:5173" NL_API_KEY="<API-KEY>" ./gradlew :app:bootRun
```
