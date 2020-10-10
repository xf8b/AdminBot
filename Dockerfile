# note: i am not using the gradle image since it doesnt support 6.7-rc4 and java 15
FROM openjdk:15-jdk-slim as BUILD
COPY . .
RUN chmod +x gradlew
RUN ./gradlew -v
RUN ./gradlew build

FROM openjdk:15-jdk-slim
COPY --from=BUILD /home/gradle/build/libs/*.jar /usr/app/bot.jar
WORKDIR /usr/app/
ENTRYPOINT ["java", "-jar", "bot.jar"]