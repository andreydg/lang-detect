// checks that minimum length is reached
function checkLength(inputId, formElement, errorId, counterId, minLength, maxLength, isMulti) {

	var inputElement = document.getElementById(inputId);
	var errorElement = document.getElementById(errorId);
	
	if(inputElement){
		inputElement.value = trim(inputElement.value);
		var counterElement = document.getElementById(counterId);
		updateCounter(counterElement,inputElement);
		var inputLength = inputElement.value.length;
		if(inputLength >= minLength && inputLength <= maxLength){
			updateActionForMultiAndSubmit(formElement,isMulti);
		} else if(inputLength < minLength ){
			errorElement.innerHTML = 'Minimum length for language detection is ' + minLength + ' characters';
		} else if(inputLength > maxLength ) {
			errorElement.innerHTML = 'Maximum length for language detection is ' + maxLength + ' characters';
		}		
	} else {
		updateActionForMultiAndSubmit(formElement,isMulti);
	}	
}

function updateAndClear(inputElement, errorId, counterId) {
	var errorElement = document.getElementById(errorId);
	errorElement.innerHTML = '';	
	var counterElement = document.getElementById(counterId);
	updateCounter(counterElement,inputElement);
}

// trims the string
function trim (str) {
	var	str = str.replace(/^\s\s*/, ''),
		ws = /\s/,
		i = str.length;
	while (ws.test(str.charAt(--i)));
	return str.slice(0, i + 1);
}

function updateActionForMultiAndSubmit(formElement, isMulti){
	if(isMulti){
		formElement.action = formElement.action + '?m=1';
	}
	formElement.submit();
}

function updateCounter(counterElement,inputElement){
	var charString = (inputElement.value.length == 1) ? ' character' : ' characters';
	counterElement.innerHTML = inputElement.value.length + charString;
}



