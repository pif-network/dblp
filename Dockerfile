FROM gradle:7.5-jdk11 AS builder

COPY --chown=gradle:gradle . /home/gradle/app
WORKDIR /home/gradle/app

RUN gradle build


FROM openjdk:19 AS runner

RUN mkdir /app
COPY --from=builder /home/gradle/app/build/libs/*.jar /app/

EXPOSE 8080:8080

ENTRYPOINT java -jar /app/dblp-0.0.1.jar -XX:+UseContainerSupport