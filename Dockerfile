FROM gradle:6.8.2-jdk15-openj9 as BUILD
COPY . .
RUN gradle -v
RUN gradle build

FROM adoptopenjdk:15-jdk-openj9
COPY --from=BUILD home/gradle/build/libs/xf8bot-*-all.jar /opt/bot/bot.jar
WORKDIR /opt/bot/
ENTRYPOINT ["java", "-jar", "bot.jar"]