#!/bin/bash

cd frontend
./build.sh
cd ..

rm -f modules/backend/src/main/resources/static/bundle.js
cp frontend/dist/bundle.js modules/backend/src/main/resources/static/bundle.js

sbt backend/run
