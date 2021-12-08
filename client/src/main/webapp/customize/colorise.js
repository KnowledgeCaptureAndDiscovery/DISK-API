function colorise(colors) {
	var base = normaliseColor(colors.base);
	var ok = normaliseColor(colors.ok);
	var error = normaliseColor(colors.error);
	var text = normaliseColor(colors.link);
	var base_text = normaliseColor(colors.header);

	var base_lighter = shadeColor(base, 0.2);
	var base_lightest = shadeColor(base, 0.3);

	var styles = STYLES;
	
	styles = styles.replace(/__BASE__/g, base);
	styles = styles.replace(/__BASE_LIGHTER__/g, base_lighter);
	styles = styles.replace(/__BASE_LIGHTEST__/g, base_lightest);
	
	styles = styles.replace(/__BASE_RGB__/g, toRGB(base));
	styles = styles.replace(/__BASE_LIGHTER_RGB__/g, toRGB(base_lighter));
	styles = styles.replace(/__BASE_LIGHTEST_RGB__/g, toRGB(base_lightest));
	
	styles = styles.replace(/__OK__/g, ok);
	styles = styles.replace(/__OK_RGB__/g, toRGB(ok));

	styles = styles.replace(/__ERROR__/g, error);
	styles = styles.replace(/__ERROR_RGB__/g, toRGB(error));
	
	styles = styles.replace(/__LINK__/g, text);
	styles = styles.replace(/__LINK_RGB__/g, toRGB(text));
	
	styles = styles.replace(/__BASE_TEXT__/g, base_text);
	styles = styles.replace(/__BASE_TEXT_RGB__/g, toRGB(base_text));
	
	var style = document.createElement("style");
	style.appendChild(document.createTextNode(""));
	document.head.appendChild(style);
    var sheet = style.sheet;

	var stylearr =  styles.split("}");
	for(var i=0; i<stylearr.length; i++) {
		if(stylearr[i]) {
			var style = stylearr[i] + " }";
    		sheet.insertRule(style, i);
		}
	}
}

function shadeColor(color, percent) {   
    var f=parseInt(color.slice(1),16),t=percent<0?0:255,p=percent<0?percent*-1:percent,R=f>>16,G=f>>8&0x00FF,B=f&0x0000FF;
    return "#"+(0x1000000+(Math.round((t-R)*p)+R)*0x10000+(Math.round((t-G)*p)+G)*0x100+(Math.round((t-B)*p)+B)).toString(16).slice(1);
}

function toRGB(hex) {
    var shorthandRegex = /^#?([a-f\d])([a-f\d])([a-f\d])$/i;
    hex = hex.replace(shorthandRegex, function(m, r, g, b) {
        return r + r + g + g + b + b;
    });
    var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? 
        parseInt(result[1], 16) + "," +
        parseInt(result[2], 16) + "," +
        parseInt(result[3], 16)
    : "0,0,0";
}

function normaliseColor(color) {
	if(color[0] == '#')
		return color;
    var colors = {"aliceblue":"#f0f8ff","antiquewhite":"#faebd7","aqua":"#00ffff","aquamarine":"#7fffd4","azure":"#f0ffff",
    "beige":"#f5f5dc","bisque":"#ffe4c4","black":"#000000","blanchedalmond":"#ffebcd","blue":"#0000ff","blueviolet":"#8a2be2","brown":"#a52a2a","burlywood":"#deb887",
    "cadetblue":"#5f9ea0","chartreuse":"#7fff00","chocolate":"#d2691e","coral":"#ff7f50","cornflowerblue":"#6495ed","cornsilk":"#fff8dc","crimson":"#dc143c","cyan":"#00ffff",
    "darkblue":"#00008b","darkcyan":"#008b8b","darkgoldenrod":"#b8860b","darkgray":"#a9a9a9","darkgreen":"#006400","darkkhaki":"#bdb76b","darkmagenta":"#8b008b","darkolivegreen":"#556b2f",
    "darkorange":"#ff8c00","darkorchid":"#9932cc","darkred":"#8b0000","darksalmon":"#e9967a","darkseagreen":"#8fbc8f","darkslateblue":"#483d8b","darkslategray":"#2f4f4f","darkturquoise":"#00ced1",
    "darkviolet":"#9400d3","deeppink":"#ff1493","deepskyblue":"#00bfff","dimgray":"#696969","dodgerblue":"#1e90ff",
    "firebrick":"#b22222","floralwhite":"#fffaf0","forestgreen":"#228b22","fuchsia":"#ff00ff",
    "gainsboro":"#dcdcdc","ghostwhite":"#f8f8ff","gold":"#ffd700","goldenrod":"#daa520","gray":"#808080","green":"#008000","greenyellow":"#adff2f",
    "honeydew":"#f0fff0","hotpink":"#ff69b4",
    "indianred ":"#cd5c5c","indigo":"#4b0082","ivory":"#fffff0","khaki":"#f0e68c",
    "lavender":"#e6e6fa","lavenderblush":"#fff0f5","lawngreen":"#7cfc00","lemonchiffon":"#fffacd","lightblue":"#add8e6","lightcoral":"#f08080","lightcyan":"#e0ffff","lightgoldenrodyellow":"#fafad2",
    "lightgrey":"#d3d3d3","lightgreen":"#90ee90","lightpink":"#ffb6c1","lightsalmon":"#ffa07a","lightseagreen":"#20b2aa","lightskyblue":"#87cefa","lightslategray":"#778899","lightsteelblue":"#b0c4de",
    "lightyellow":"#ffffe0","lime":"#00ff00","limegreen":"#32cd32","linen":"#faf0e6",
    "magenta":"#ff00ff","maroon":"#800000","mediumaquamarine":"#66cdaa","mediumblue":"#0000cd","mediumorchid":"#ba55d3","mediumpurple":"#9370d8","mediumseagreen":"#3cb371","mediumslateblue":"#7b68ee",
    "mediumspringgreen":"#00fa9a","mediumturquoise":"#48d1cc","mediumvioletred":"#c71585","midnightblue":"#191970","mintcream":"#f5fffa","mistyrose":"#ffe4e1","moccasin":"#ffe4b5",
    "navajowhite":"#ffdead","navy":"#000080",
    "oldlace":"#fdf5e6","olive":"#808000","olivedrab":"#6b8e23","orange":"#ffa500","orangered":"#ff4500","orchid":"#da70d6",
    "palegoldenrod":"#eee8aa","palegreen":"#98fb98","paleturquoise":"#afeeee","palevioletred":"#d87093","papayawhip":"#ffefd5","peachpuff":"#ffdab9","peru":"#cd853f","pink":"#ffc0cb","plum":"#dda0dd","powderblue":"#b0e0e6","purple":"#800080",
    "red":"#ff0000","rosybrown":"#bc8f8f","royalblue":"#4169e1",
    "saddlebrown":"#8b4513","salmon":"#fa8072","sandybrown":"#f4a460","seagreen":"#2e8b57","seashell":"#fff5ee","sienna":"#a0522d","silver":"#c0c0c0","skyblue":"#87ceeb","slateblue":"#6a5acd","slategray":"#708090","snow":"#fffafa","springgreen":"#00ff7f","steelblue":"#4682b4",
    "tan":"#d2b48c","teal":"#008080","thistle":"#d8bfd8","tomato":"#ff6347","turquoise":"#40e0d0",
    "violet":"#ee82ee",
    "wheat":"#f5deb3","white":"#ffffff","whitesmoke":"#f5f5f5",
    "yellow":"#ffff00","yellowgreen":"#9acd32"};

    if (typeof colors[color.toLowerCase()] != 'undefined')
        return colors[color.toLowerCase()];

    return "#000000";
}

var STYLES =  
".form-control {" + 
"  background-image: linear-gradient(__OK__, __OK__),linear-gradient(#d2d2d2,#d2d2d2) !important;" + 
"}" + 
".form-group .form-control:focus, " + 
".form-group-default .form-control:focus {" + 
"  background-image: linear-gradient(__OK__, __OK__),linear-gradient(#d2d2d2,#d2d2d2) !important;" + 
"}" + 
".form-group.has-error .material-input:focus, " + 
".form-group.has-error .form-control:focus, " + 
".form-group.has-error .form-control.focus {" + 
"  background-image: linear-gradient(__ERROR__, __ERROR__),linear-gradient(#d2d2d2,#d2d2d2) !important;" + 
"}" + 
"#optional .form-group.has-error .material-input:focus, " + 
"#optional .form-group.has-error .form-control:focus, " + 
"#optional .form-group.has-error .form-control.focus {" + 
"  background-image: linear-gradient(#999999,#999999),linear-gradient(#d2d2d2,#d2d2d2) !important;" + 
"}" + 
".nav-tabs {" + 
"  background: __BASE__;" + 
"}" + 
".navbar.navbar, .navbar-default.navbar {" + 
"  background-color: __BASE__;" + 
"}" + 
"a, a:hover, a:focus {" + 
"  color: __LINK__;" + 
"}" + 
".dropdown-menu .item-selected {" + 
"  background-color: #eee;" + 
"}" + 
".btn-group-header .btn-info {" + 
"  background-color: __BASE_LIGHTER__ !important;" + 
"  border-color: __BASE_LIGHTER__ !important;" + 
"}" + 
".btn-group-header .btn-info:hover {" + 
"  background-color: __BASE_LIGHTEST__ !important;" + 
"}" + 
".pagination .active a, " + 
".pagination .active a:hover," + 
".pagination .active a:active" + 
"{" + 
"	background-color: __BASE__ !important;" + 
"	border-color: __BASE__ !important;" + 
"}" + 
".selected-row, .selected-row td {" + 
"	background-color: rgba(__BASE_RGB__, 0.4) !important;" +
"}" + 
".selected-row.even, .selected-row.even td {" + 
"	background-color: rgba(__BASE_RGB__, 0.45) !important;" + 
"}" + 
".selected-row.active, .selected-row.active td {" + 
"	background-color: rgba(__BASE_RGB__, 0.5) !important;" + 
"}" + 
".selected-row a {" + 
"	color: __BASE_TEXT__;" + 
"}" + 
".comparison-table td.header-cell {" + 
"	background: rgba(__BASE_RGB__, 0.8);" + 
"	border-color: rgba(__BASE_RGB__, 0.9) !important;" + 
"	color: rgba(__BASE_TEXT_RGB__, 0.84);" + 
"}" + 
".btn-info, .panel-info > .panel-heading {" + 
"  background-color: __BASE__ !important;" + 
"  color: rgba(__BASE_TEXT_RGB__,.84);" + 
"  border-color: __BASE__ !important;" + 
"}" + 
".panel-info, .panel-info>.panel-heading+.panel-collapse>.panel-body {" + 
"	border-color: __BASE__ !important;" + 
"}" + 
".btn-info:hover {" + 
"  background-color: __BASE_LIGHTER__ !important;" + 
"}" + 
".form-group .control-label," + 
".browse-label {" + 
"	color: __OK__ !important;" + 
"	background-color: rgba(__OK_RGB__,0.05) !important;" + 
"	border: 1px solid rgba(__OK_RGB__,0.08) !important;" + 
"}" + 
"#required .form-group.has-error .control-label," + 
".browse-label.error-label {" + 
"	background-color: rgba(__ERROR_RGB__,0.05) !important;" + 
"	border: 1px solid rgba(__ERROR_RGB__,0.08) !important;	" + 
"	color: __ERROR__ !important;" + 
"}" + 
".breadcrumb > li.active-crumb > a {" + 
"  color: __BASE__;" + 
"}";
 
 