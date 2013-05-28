#!/bin/bash

DATA_PATH=../lang-detect/war/

java -server -Xmx2G -cp bin:../lang-detect/war/WEB-INF/classes \
  language.tools.LanguageDetectorTester \
  -dataPath $DATA_PATH \
  -runMultiTestSet \
  -useClassifier 1 \
  -boundaryDetector FOUR_WORD_BIGRAM \
  -verbose
