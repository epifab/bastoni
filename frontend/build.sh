#!/bin/bash

rm -rf dist
mkdir dist
cp main.html dist/main.html

npm run build
