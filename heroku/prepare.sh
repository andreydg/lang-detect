#!/bin/bash

cp -R ../lang-detect/war/languagemodels/ .
cp -R ../lang-detect/src/language/classifier ./src/main/java/language
cp -R ../lang-detect/src/language/model ./src/main/java/language
cp -R ../lang-detect/src/language/util ./src/main/java/language
