#!/bin/bash

rm -rf dist
mkdir dist
cp main.html dist/main.html

`npm bin`/ts-interface-builder src/model/*.ts
npm run build
