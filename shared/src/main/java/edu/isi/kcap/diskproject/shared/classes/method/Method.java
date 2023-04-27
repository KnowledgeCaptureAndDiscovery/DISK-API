package edu.isi.kcap.diskproject.shared.classes.method;

import java.util.List;

public class Method {
    private String name;
    private String link;
    private String providerUrl;
    private List<MethodInput> inputParameters;
    private List<MethodOutput> outputParameters;

    public Method(String name, String link, String providerUrl) {
        this.name = name;
        this.link = link;
        this.providerUrl = providerUrl;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setProviderUrl(String url) {
        this.providerUrl = url;
    }

    public void setInputParameters(List<MethodInput> params) {
        this.inputParameters = params;
    }

    public void setOutputParameters(List<MethodOutput> params) {
        this.outputParameters = params;
    }

    public String getName() {
        return this.name;
    }

    public String getLink() {
        return this.link;
    }

    public String getProviderUrl() {
        return this.providerUrl;
    }

    public List<MethodInput> getInputParameters() {
        return this.inputParameters;
    }

    public List<MethodOutput> getOutputParamters() {
        return this.outputParameters;
    }
}
