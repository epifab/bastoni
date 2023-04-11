!#/bin/bash

rm dist/main.html 2> /dev/null
cp src/main.html dist/main.html

npm run build