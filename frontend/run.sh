#!/bin/bash

./build.sh

npx http-server ./dist/ -p 8082 -o
