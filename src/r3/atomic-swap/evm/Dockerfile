# syntax=docker/dockerfile:1.4
FROM ghcr.io/foundry-rs/foundry:latest
 
# Install Node.js 16, npm, and netcat using apk
RUN apk --no-cache add nodejs npm netcat-openbsd
 
# Set the working directory in the container
WORKDIR /app
 
# Copy your project into the container
COPY . /app
 
# Install project dependencies
RUN forge install --no-git
RUN npm install

# Create a custom entry point script
RUN chmod 777 /app/entrypoint.sh
 
EXPOSE 8545

HEALTHCHECK --interval=5s --timeout=5s --start-period=5s --retries=36 \
  CMD [ "sh", "-c", "test -f /app/deployment_complete" ]
 
# Start the Foundry Forge (in the background) and Hardhat Node
ENTRYPOINT ["/app/entrypoint.sh"]
