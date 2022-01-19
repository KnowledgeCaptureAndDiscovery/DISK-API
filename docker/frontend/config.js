var CONFIG = {
        SERVER : "http://localhost:8080/disk-server",
        TITLE : "NeuroDISK",
        COLORS : {
                base : "#5D7BA0", // Background color for Top Banner, and Headers
                link : "#5D7BA0", // Text color for Links
                header : "white", // Text color for Headers
                ok : "#5D7BA0", // Color indicating form items that are filled out
                error : "#D42041" // Color indicating required form items that are not filled out 
        }
}

CONFIG.HOME = "<h4>What is DISK?</h4>" +
"<p>DISK is a general AI discovery system that automatically tests questions and hypotheses based on the data that it has access to.</p>" +
"<p>NeuroDISK is the DISK site devoted to neuroscience, and in particular to the <a href=\"http://enigma.ini.usc.edu/\" target=\"_blank\">ENIGMA consortium</a>.</p>" +
"<p>You can specify a question, and DISK will then figure out a way to answer it.</p>" +
"<h4>How Does DISK Work?</h4>" +
"<p>DISK has access all the data available on " +
"<a href=\"http://organicdatapublishing.org/enigma_new/index.php/Main_Page\" target=\"_blank\">the ENIGMA consortium wiki.</a>" +
"<p>DISK draws from a library of general lines of inquiry. Each line of inquiry expresses a common method to answer a type of question. " +
"For example, there is a general line of inquiry to answer the question of whether a gene is correlated with a brain characteristic, " +
"which would implement a common method which is to find genomic data from patients that have that type of cancer and does a regression with the data found. " +
"There is a different line of inquiry for the question of whether a protein is correlated with a type of cancer, which would look for proteomic " +
"data and do apply proteomics tools to the data found.</p>" +
"<p>When you  specify a question, DISK retrieves a line of inquiry that specifies what data and method would be appropriate for it. " +
"DISK will then execute the line of inquiry, and will show you the results.</p>" +
"<iframe width=\"640\" hexight=\"480\" src=\"https://www.youtube.com/embed/LZJ-A3RyQcY\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture\" allowfullscreen></iframe>";