// package org.diskproject.server.adapters.Reana;

// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.Map;

// public class ReanaSpecification {
//     Map<String, String> parameters = new HashMap<String, String>();
//     Specification specification;

//     public void setSpecification(Specification specificationObject) {
//         this.specification = specificationObject;
//     }

//     public class Specification {
//         Inputs inputs;
//         Outputs outputs;
//         private String version;
//         Workflow workflow;

//         // Getter Methods

//         public Inputs getInputs() {
//             return inputs;
//         }

//         public Outputs getOutputs() {
//             return outputs;
//         }

//         public String getVersion() {
//             return version;
//         }

//         public Workflow getWorkflow() {
//             return workflow;
//         }

//     }

//     public class Workflow {
//         private String file;
//         private String type;

//         // Getter Methods

//         public String getFile() {
//             return file;
//         }

//         public String getType() {
//             return type;
//         }

//         // Setter Methods

//         public void setFile(String file) {
//             this.file = file;
//         }

//         public void setType(String type) {
//             this.type = type;
//         }
//     }

//     public class Outputs {
//         ArrayList<Object> files = new ArrayList<Object>();

//         // Getter Methods

//         // Setter Methods

//     }

//     public class Inputs {
//         ArrayList<Object> directories = new ArrayList<Object>();
//         ArrayList<String> files = new ArrayList<String>();
//         Map<String, String> parameters = new HashMap<String, String>();

//         public Inputs(ArrayList<Object> directories, ArrayList<String> files, Map<String, String> parameters) {
//             this.directories = directories;
//             this.files = files;
//             this.parameters = parameters;
//         }

//         public ArrayList<Object> getDirectories() {
//             return directories;
//         }

//         public ArrayList<String> getFiles() {
//             return files;
//         }

//         public Map<String, String> getParameters() {
//             return parameters;
//         }

//     }

//     public class Parameters {
//         private String input;

//         // Getter Methods

//         public String getInput() {
//             return input;
//         }

//         // Setter Methods

//         public void setInput(String input) {
//             this.input = input;
//         }
//     }
// } 