FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY . .
RUN mkdir -p bin && javac -encoding UTF-8 -cp "lib/*" -d bin $(find src -name "*.java")
EXPOSE 8080
CMD ["java", "-cp", "bin:lib/*", "com.vault.Main"]
