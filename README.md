lang-detect
===========

Language detection with language boundary recognition.

Live systems:

* http://lang.andreyg.com (Google App Engine)
* http://andreyg-lang-detect.herokuapp.com (Heroku)


To deploy to heroku

1. <code> git clone https://github.com/andreydg/lang-detect.git </code>
2. <code> cd heorku </code>
3. <code> ./prepare.sh </code>
4. <code> mvn compile package </code>
5. <code> sh target/bin/webapp </code> - test it out with, should start at http://localhost:8080

