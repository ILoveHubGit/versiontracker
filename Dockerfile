FROM openjdk:8-alpine

COPY target/uberjar/versiontracker.jar /versiontracker/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/versiontracker/app.jar"]
