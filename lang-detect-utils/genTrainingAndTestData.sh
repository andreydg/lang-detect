#!/bin/bash

DATA_PATH=../lang-detect/war/

java -server -mx500m -cp bin:../lang-detect/war/WEB-INF/classes \
  language.tools.LanguageDetectorTester \
  -dataPath $DATA_PATH \
  -genTrainTest \
  -verbose
