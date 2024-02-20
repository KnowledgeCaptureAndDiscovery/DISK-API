package org.diskproject.server.managers;

import java.util.HashMap;
import java.util.Map;

import org.diskproject.server.repository.DiskRDF;
import org.diskproject.server.util.Config;
import org.diskproject.server.util.KBUtils;
import org.diskproject.server.util.Config.VocabularyConfig;
import org.diskproject.shared.classes.util.KBConstants;
import org.diskproject.shared.classes.vocabulary.Vocabulary;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.OntSpec;

public class VocabularyManager {
    private DiskRDF rdf;
    private Map<String, Vocabulary> vocabularies;
    private Map<String, String> URLs;
    private Map<String, KBAPI> KBs;
    private KBAPI hypothesisVocabulary;

    public VocabularyManager (DiskRDF rdf) {
        if (rdf == null) {
            System.err.println("Could not load main ontology");
            return;
        }
        this.rdf = rdf;
        this.loadKB();
    }

    public void loadKB () {
        this.vocabularies = new HashMap<String, Vocabulary>();
        this.URLs = new HashMap<String, String>();
        this.KBs = new HashMap<String, KBAPI>();

        this.rdf.startWrite();
        for (VocabularyConfig v: Config.get().vocabularies) {
            KBAPI curKB = null;
            try {
                curKB = this.rdf.getFactory().getKB(v.url, OntSpec.PLAIN, false, true);
                this.URLs.put(v.prefix, v.url);
                this.KBs.put(v.prefix, curKB);
            } catch (Exception e) {
                System.out.println("Could not load " + v.url);
            }
            if (curKB != null) {
                this.vocabularies.put(v.prefix, this.initializeVocabularyFromKB(curKB, v.namespace, v.prefix, v.title, v.description));
                System.out.println("Vocabulary loaded: " + v.url);
            }
        }
        this.rdf.end();
        this.loadDefaultHypothesisVocabulary();

        if (this.vocabularies.size() == 0)
            System.err.println("WARNING: No external vocabularies found on the configuration file.");
    }

    // This vocabulary 
    private void loadDefaultHypothesisVocabulary() {
        try {
            this.hypothesisVocabulary = this.rdf.getFactory().getKB(KBConstants.HYP_URI, OntSpec.PLAIN, false, true);
            this.addVocabularyFromKB(this.hypothesisVocabulary, KBConstants.HYP_URI, KBConstants.HYP_NS,
                    "DISK Hypothesis Ontology", "hyp",
                    "DISK Hypothesis Ontology: Defines general terms to express hypotheses.");
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    public void reloadKB () {
        for (KBAPI kb: this.KBs.values()) {
            KBUtils.clearKB(kb, this.rdf);
        }
        if (this.hypothesisVocabulary != null) {
            KBUtils.clearKB(this.hypothesisVocabulary, this.rdf);
        }
        this.loadKB();
    }

    public void addVocabularyFromKB (KBAPI KB, String URI, String NS, String title, String prefix, String description) {
        this.rdf.startWrite();
        this.vocabularies.put(URI, this.initializeVocabularyFromKB(KB, NS, prefix, title, description));
        this.rdf.end();
    }

    public Vocabulary getVocabulary(String uri) {
        return this.vocabularies.get(uri);
    }

    public Map<String, Vocabulary> getVocabularies() {
        return this.vocabularies;
    }

    private Vocabulary initializeVocabularyFromKB(KBAPI kb, String ns, String prefix, String title, String description) {
        Vocabulary vocabulary = new Vocabulary(ns);
        vocabulary.setPrefix(prefix);
        vocabulary.setTitle(title);
        if (description != null)
            vocabulary.setDescription(description);
        KBUtils.fetchTypesAndIndividualsFromKB(kb, vocabulary);
        KBUtils.fetchPropertiesFromKB(kb, vocabulary);
        return vocabulary;
    }

    public String getPrefixes() {
        String txt = "";
        for (Vocabulary v: this.vocabularies.values()) {
            txt += String.format("PREFIX %s: <%s>\n", v.getPrefix(), v.getNamespace());
        }
        return txt;
    }
}