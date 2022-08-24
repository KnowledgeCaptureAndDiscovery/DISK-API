package org.diskproject.server.adapters.Reana;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ApiSchema {


    public class ReanaSpecification {
        Map<String, String> parameters = new HashMap<String, String>();
        Specification specification;

        public Specification getSpecification() {
            return specification;
        }

        public void setSpecification(Specification specificationObject) {
            this.specification = specificationObject;
        }

        public class Specification {
            Inputs inputs;
            Outputs outputs;
            private String version;
            Workflow workflow;

            // Getter Methods

            public Inputs getInputs() {
                return inputs;
            }

            public Outputs getOutputs() {
                return outputs;
            }

            public String getVersion() {
                return version;
            }

            public Workflow getWorkflow() {
                return workflow;
            }

        }

        public class Workflow {
            private String file;
            private String type;

            // Getter Methods

            public String getFile() {
                return file;
            }

            public String getType() {
                return type;
            }

            // Setter Methods

            public void setFile(String file) {
                this.file = file;
            }

            public void setType(String type) {
                this.type = type;
            }
        }

        public class Outputs {
            ArrayList<String> files = new ArrayList<String>();

            public ArrayList<String> getFiles() {
                return files;
            }

            // Getter Methods

            // Setter Methods

        }

        public class Inputs {
            ArrayList<Object> directories = new ArrayList<Object>();
            ArrayList<String> files = new ArrayList<String>();
            Map<String, String> parameters = new HashMap<String, String>();

            public Inputs(ArrayList<Object> directories, ArrayList<String> files, Map<String, String> parameters) {
                this.directories = directories;
                this.files = files;
                this.parameters = parameters;
            }

            public ArrayList<Object> getDirectories() {
                return directories;
            }

            public ArrayList<String> getFiles() {
                return files;
            }

            public Map<String, String> getParameters() {
                return parameters;
            }

        }

        public class Parameters {
            private String input;

            // Getter Methods

            public String getInput() {
                return input;
            }

            // Setter Methods

            public void setInput(String input) {
                this.input = input;
            }
        }
    }

    public class RequestApiLaunch {
        String url;
        String name;
        String specification;
        String parameters;

        public RequestApiLaunch(String url, String name, String specification, String parameters) {
            this.url = url;
            this.name = name;
            this.specification = specification;
            this.parameters = parameters;
        }
    }

    public class ReanaWorkflow {
        public String name;
        public String created;
        public String id;
        public String launcher_url;
        public Object progress;
        public Object size;
        public String status;
        public String user;

        public ReanaWorkflow(String name, String created, String id, String launcher_url, Object progress, Object size,
                String status, String user) {
            this.name = name;
            this.created = created;
            this.id = id;
            this.launcher_url = launcher_url;
            this.progress = progress;
            this.size = size;
            this.status = status;
            this.user = user;
        }
    }

    public class ResponseRunStatus {
        public class Progress {
            String current_command;
            String current_step_name;
            Object failed;
            Object finished;
            String run_finished_at;
            String run_started_at;
            Object running;
            Object total;
            public String getCurrent_command() {
                return current_command;
            }
            public String getCurrent_step_name() {
                return current_step_name;
            }
            public Object getFailed() {
                return failed;
            }
            public Object getFinished() {
                return finished;
            }
            public String getRun_finished_at() {
                return run_finished_at;
            }
            public String getRun_started_at() {
                return run_started_at;
            }
            public Object getRunning() {
                return running;
            }
            public Object getTotal() {
                return total;
            }
        }
        public String created;
        public String id;
        public String logs;
        public String name;
        public Progress progress;
        public String status;
        public String user;
        public String getCreated() {
            return created;
        }
        public String getId() {
            return id;
        }
        public String getLogs() {
            return logs;
        }
        public String getName() {
            return name;
        }
        public Object getProgress() {
            return progress;
        }
        public String getStatus() {
            return status;
        }
        public String getUser() {
            return user;
        }
    }

    public class WorkflowInventory {
        String url;
        String name;
        String specification;
        String rawUrl;
        String id;

        public WorkflowInventory(String url, String name, String specification, String rawUrl, String id) {
            this.url = url;
            this.name = name;
            this.specification = specification;
            this.rawUrl = rawUrl;
            this.id = id;
        }

    }
}
