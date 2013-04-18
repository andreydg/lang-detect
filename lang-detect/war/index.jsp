<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="language.model.multiling.*" %>
<%@ page import="language.model.*" %>
<%@ page import="language.model.NgramLanguageDetector.ClassificationAlgorithm" %>
<%@ page import="java.util.*" %>
<%@ page import="language.util.*" %>
<%@ page import="java.io.File" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN"            
"http://www.w3.org/TR/html4/strict.dtd">
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
		<title>Language Detection</title>
		<link rel="stylesheet" type="text/css" media="screen, projection" href="/general.css">
		<link rel="shortcut icon" href="favicon.ico">
		<script type="text/javascript" src="main.js"></script
	</head>
	<%
	
		final int MIN_LENGTH = 25;
		final int MAX_LENGTH = 100000;
		// TODO: get base directory with properties
		String basePath = "";
		File baseFilePath = new File(basePath);
		// allow both windows and linux compatibility
				
		NgramLanguageDetector detector = new NgramLanguageDetector(baseFilePath);
		LanguageBoundaryDetector boundaryDetector = new SlidingWindowWithBigramLanguageBoundaryDetector(
				ClassificationAlgorithm.LINEAR_WEIGHTS, detector, 4);
		String q = request.getParameter("q");
		boolean isMulti = request.getParameter("m") != null;
		List<Pair<String, Locale>> multiStringTag = null;
		Locale singleLanguage = null;
		String errorMsg = "";
		if(q != null && q.length() >= MIN_LENGTH && q.length() <= MAX_LENGTH){
			if (isMulti){
				multiStringTag = boundaryDetector.tagStringWithLanguages(q);
			} else {
				singleLanguage = detector.getMostLikelyLanguage(q);
			}
			detector.logQuery(q);
		} else if (q != null && q.length() > 0 && q.length() < MIN_LENGTH ){
			errorMsg =  "Minimum length for language detection is " + MIN_LENGTH + " characters";
		} else if (q != null && q.length() > MAX_LENGTH ){
			errorMsg =  "Maximum length for language detection is " + MAX_LENGTH + " characters";
		} else {
			q = "";
		}
		
	%>
	<body class='bodyTag' style="background-image:url('background.jpg')">
		<div class="container">
			<h1 class='title'> Language Detection</h1>
			<div id='error' class='errorDiv'><%=errorMsg%></div>
			<form id='f' action='/' name='f' method='POST'>
				<center>
					<div>
						<textarea onkeydown="updateAndClear(this,'error', 'inputCounter');"  onkeyup="updateAndClear(this,'error', 'inputCounter');" id= 'inputArea' value ='' name='q' rows='4' cols='100' title='Language Detection'><%=q%></textarea>
					</div>
				
					<span class="inputCounter" id="inputCounter"><%=q.length()%> characters</span>
					<div>
						<input onclick="checkLength('inputArea', document.f, 'error', 'inputCounter',  <%=MIN_LENGTH%>, <%=MAX_LENGTH%>);" class='btn' type='button' value='Detect Language'>
						<input onclick="checkLength('inputArea', document.f, 'error', 'inputCounter',  <%=MIN_LENGTH%>, <%=MAX_LENGTH%>, true);" class='btn' type='button' value='Multi-Detect Language'>		
					</div>
				</center>
			</form>
			<% if(multiStringTag != null) {%>
				<table class='output'>
					<%for (Pair<String, Locale> pair : multiStringTag) {%>
					<tr>
						<td class='stringMark'><%=pair.getFirst()%></td>
						<td class='languageMark'><%=pair.getSecond().getDisplayLanguage()%></td>
					</tr>
					<%}%>
				
				</table>
			<%} else if (singleLanguage != null) {%>
				<div class="detectedLanguage">
					<span><%=singleLanguage.getDisplayLanguage()%></span>
				</div>
			<%}%>
			<br/>
			<div id="footer" class="footer">
				<span class='desc'> Supported languages: en,es,fr,de,it,pt. Experimental support for multilingual strings.</h4>
			</div>
		</div>

  </body>
</html>