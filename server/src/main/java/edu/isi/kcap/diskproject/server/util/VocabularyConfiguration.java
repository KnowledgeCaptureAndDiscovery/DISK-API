package edu.isi.kcap.diskproject.server.util;

import edu.diskproject.shared.classes.vocabulary.Vocabulary;
import edu.isi.kcap.ontapi.KBAPI;

public class VocabularyConfiguration {
    private String prefix, url, namespace, description, title;
    private KBAPI kb;
    private Vocabulary vocabulary;

    public VocabularyConfiguration(String prefix, String url, String namespace, String title) {
        this.prefix = prefix;
        this.url = url;
        this.namespace = namespace;
        this.title = title;
    }

    public void setKB(KBAPI kb) {
        this.kb = kb;
    }

    public void setVocabulary(Vocabulary vocab) {
        this.vocabulary = vocab;
    }

    public void setDescription(String desc) {
        this.description = desc;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getURL() {
        return this.url;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String getDescription() {
        return this.description;
    }

    public KBAPI getKB() {
        return this.kb;
    }

    public Vocabulary getVocabulary() {
        return this.vocabulary;
    }
}
