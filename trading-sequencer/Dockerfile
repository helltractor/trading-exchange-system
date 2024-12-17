FROM openjdk:21
ENV APP_HOME=/apps
WORKDIR $APP_HOME
ADD ./target/trading-sequencer.jar trading-sequencer.jar
EXPOSE 8003
ENTRYPOINT ["java", "-jar"]
CMD ["trading-sequencer.jar"]