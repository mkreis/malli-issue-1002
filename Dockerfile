FROM openjdk:8-alpine

COPY target/uberjar/malli-issue-1002.jar /malli-issue-1002/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/malli-issue-1002/app.jar"]
