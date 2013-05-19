#!/bin/bash

DATA_PATH=../lang-detect/war/

java -server -Xmx2G -cp bin:../lang-detect/war/WEB-INF/classes \
  language.tools.LanguageDetectorTester \
  -dataPath $DATA_PATH \
  -runTestSet \
  -useClassifier 1 \
  -verbose
