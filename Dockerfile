FROM bellsoft/liberica-openjre-alpine:17 AS app-base
WORKDIR /app

FROM bellsoft/liberica-openjdk-alpine:17 AS build-base
WORKDIR /app
COPY . /app

FROM build-base AS builder
RUN --mount=type=cache,target=/root/.gradle ./gradlew clean build
RUN mkdir -p build/dependency && (cd build/dependency; jar -xf ../libs/*-SNAPSHOT.jar)

FROM app-base AS production
ARG PROFILE
ARG DEPENDENCY=/app/build/dependency
COPY --from=builder ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=builder ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=builder ${DEPENDENCY}/BOOT-INF/classes /app
EXPOSE 8080
ENTRYPOINT ["java","-cp",".:./lib/*","kr.heek.api.ApiApplicationKt","--spring.profiles.active","$PROFILE"]
