#!/bin/bash

# Re-create interface checkers
find src/model -name "*-ti.ts" -type f -delete
`npm bin`/ts-interface-builder src/model/*.ts

npm run build
