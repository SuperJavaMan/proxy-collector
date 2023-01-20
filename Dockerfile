FROM openjdk:17-alpine
MAINTAINER com.oleg.pavliukov
COPY build/libs/proxy-collector-0.0.5.jar proxy-collector.jar
ENTRYPOINT ["java","-jar","/proxy-collector.jar"]