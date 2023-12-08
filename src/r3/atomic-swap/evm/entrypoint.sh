#!/bin/sh

# Function to check if a specific port is open
wait_for_port() {
    local port=$1
    while ! nc -z localhost $port; do   
        sleep 1
    done
}

# Background task to deploy contracts once the node is ready
(
    echo "Waiting for Hardhat node to start on port 8545..."
    wait_for_port 8545

    echo "Deploying contracts..."
    npx hardhat run deploy.js --network localhost
    touch /app/deployment_complete
) &

# Start Hardhat node in the foreground
npx hardhat node
