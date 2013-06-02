#!/bin/bash

cp -R ../lang-detect/war/ .
mv war/languagemodels .

# remove model sources and test sets
rm -rf languagemodels/modelSource
rm -rf languagemodels/multiLangTestSet
rm -rf languagemodels/trainingAndTestSet

# copy main source
cp -R ../lang-detect/src/language/classifier ./src/main/java/language
cp -R ../lang-detect/src/language/model ./src/main/java/language
cp -R ../lang-detect/src/language/util ./src/main/java/language
