#!/usr/bin/env bash

docker-compose -f docker-compose-1.yaml -f docker-compose-2.yaml up -d

docker-compose -f docker-compose-1.yaml -f docker-compose-2.yaml logs --tail=0 --follow
