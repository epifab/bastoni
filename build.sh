#!/bin/bash

sbt test sdk/fullLinkJS
cp modules/sdk/target/scala-3.3.1/sdk-opt/main.js modules/sdk/src/main/typescript/index.js
cd modules/sdk/src/main/typescript
npm install
find model -name "*-ti.ts" -type f -delete
`npm bin`/ts-interface-builder model/*.ts
npm link
cd ../../../../../frontend
npm install
npm link bastoni
npm run build
