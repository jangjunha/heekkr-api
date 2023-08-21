FROM bellsoft/liberica-openjre-alpine:17 AS app-base
WORKDIR /app

FROM bellsoft/liberica-openjdk-alpine:17 AS build-base
WORKDIR /app
COPY . /app

FROM build-base AS builder
RUN --mount=type=cache,target=/root/.gradle ./gradlew clean build -p app
RUN mkdir -p app/build/dependency && (cd app/build/dependency; jar -xf ../libs/*-SNAPSHOT.jar)

FROM app-base AS production
ARG SPRING_PROFILES_ACTIVE=default
ENV SPRING_PROFILES_ACTIVE $SPRING_PROFILES_ACTIVE
ARG DEPENDENCY=/app/app/build/dependency
COPY --from=builder ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=builder ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=builder ${DEPENDENCY}/BOOT-INF/classes /app
EXPOSE 8080
ENTRYPOINT ["java","-cp",".:./lib/*","kr.heek.api.ApiApplicationKt"]
