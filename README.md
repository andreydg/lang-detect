lang-detect
===========

Language detection with language boundary recognition.

Live systems:

* http://lang.andreyg.com (Google App Engine)
* http://andreyg-lang-detect.herokuapp.com (Heroku)
* 

To deploy to app-engine

* Open checked in eclipe project
* See <code> WEB-INF/appengine-web.xml </code> and <code> WEB-INF/web.xml </code>
* Use app-engine plugin


To deploy to heroku

1. <code> git clone https://github.com/andreydg/lang-detect.git </code>
2. <code> cd heroku </code>
3. <code> ./prepare.sh </code>
4. <code> mvn compile package </code>
5. <code> sh target/bin/webapp </code> - test it out with, should start at http://localhost:8080
6. <code> git init </code>
7. <code> git add . </code>
8. <code> git commit -m "Ready to deploy" </code>
9. <code> heroku create </code>
10. <code> git push heroku master </code>
