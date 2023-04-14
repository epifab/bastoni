#!/bin/bash

rm -rf dist
mkdir dist
cp main.html dist/main.html

find src/model -name "*-ti.ts" -type f -delete
`npm bin`/ts-interface-builder src/model/*.ts
npm run build

rm ../modules/backend/src/main/resources/static/bundle.js
cp dist/bundle.js ../modules/backend/src/main/resources/static/bundle.js
