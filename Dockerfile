FROM gradle:6.7.0-jdk15-openj9 as BUILD
COPY . .
RUN pwd
RUN gradle -v
RUN gradle build

FROM adoptopenjdk:15-jdk-openj9
COPY --from=BUILD /build/libs/xf8bot-*-all.jar /usr/app/bot.jar
WORKDIR /usr/app/
ENTRYPOINT ["java", "-jar", "bot.jar"]