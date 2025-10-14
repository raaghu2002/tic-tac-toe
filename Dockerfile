FROM openjdk:17-jdk-slim
WORKDIR /app
COPY ./build/tictactoe-0.0.1-SNAPSHOT.jar ./tictactoe.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "tictactoe.jar"]
