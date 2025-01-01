FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy source files to the container
COPY ./src /app/src

# Create a directory for compiled classes
RUN mkdir /app/bin

# Compile all Java source files
RUN javac /app/src/*.java -d /app/bin

# Set the default command to run ServerGUI
CMD ["java", "-cp", "/app/bin", "ServerGUI"]