#!/bin/bash

echo "Running DB migration"

java -jar /opt/corda/bin/corda.jar run-migration-scripts --core-schemas --app-schemas --allow-hibernate-to-manage-app-schema


echo "Starting corda node"
run-corda
