FROM gradle:6.6.1 as BUILD
COPY . .
RUN gradle build

FROM openjdk:15-jdk-slim
COPY --from=BUILD /home/gradle/build/libs/*.jar /usr/app/bot.jar
WORKDIR /usr/app/
ENTRYPOINT ["java", "-jar", "bot.jar"]
