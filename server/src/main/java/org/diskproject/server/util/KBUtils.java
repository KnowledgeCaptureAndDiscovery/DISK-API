package org.diskproject.server.util;

import org.diskproject.shared.classes.util.KBConstants;
import org.diskproject.shared.classes.vocabulary.Individual;
import org.diskproject.shared.classes.vocabulary.Property;
import org.diskproject.shared.classes.vocabulary.Type;
import org.diskproject.shared.classes.vocabulary.Vocabulary;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBObject;
import edu.isi.kcap.ontapi.KBTriple;

public class KBUtils {
    public static String createPropertyLabel(String pName) {
        // Remove starting "has"
        pName = pName.replaceAll("^has", "");
        // Convert camel case to spaced human readable string
        pName = pName.replaceAll(String.format("%s|%s|%s", "(?<=[A-Z])(?=[A-Z][a-z])", "(?<=[^A-Z])(?=[A-Z])",
                "(?<=[A-Za-z])(?=[^A-Za-z])"), " ");
        // Make first letter upper case
        return pName.substring(0, 1).toUpperCase() + pName.substring(1);
    }

    public static void fetchPropertiesFromKB(KBAPI kb, Vocabulary vocabulary) {
        for (KBObject prop : kb.getAllProperties()) {
            if (!prop.getID().startsWith(vocabulary.getNamespace()))
                continue;

            KBObject domCls = kb.getPropertyDomain(prop);
            KBObject rangeCls = kb.getPropertyRange(prop);
            String desc = kb.getComment(prop);

            Property mProp = new Property();
            mProp.setId(prop.getID());
            mProp.setName(prop.getName());
            if (desc != null)
                mProp.setDescription(desc);

            String label = KBUtils.createPropertyLabel(prop.getName());
            mProp.setLabel(label);
            if (domCls != null)
                mProp.setDomain(domCls.getID());
            if (rangeCls != null)
                mProp.setRange(rangeCls.getID());

            vocabulary.addProperty(mProp);
        }
    }

    public static void fetchTypesAndIndividualsFromKB(KBAPI kb, Vocabulary vocabulary) {
        KBObject typeprop = kb.getProperty(KBConstants.RDF_NS + "type");
        for (KBTriple t : kb.genericTripleQuery(null, typeprop, null)) {
            KBObject inst = t.getSubject();
            KBObject typeobj = t.getObject();
            String instId = inst.getID();

            if (instId == null || instId.startsWith(vocabulary.getNamespace())
                    || typeobj.getNamespace().equals(KBConstants.OWL_NS)) {
                continue;
            }

            // Add individual, this does not ADD individuals without type.
            Individual ind = new Individual();
            ind.setId(inst.getID());
            ind.setName(inst.getName());
            ind.setType(typeobj.getID());
            String label = kb.getLabel(inst);
            if (label == null)
                label = inst.getName();
            ind.setLabel(label);
            vocabulary.addIndividual(ind);

            // Add asserted types
            if (!typeobj.getID().startsWith(vocabulary.getNamespace()))
                continue;
            String clsId = typeobj.getID();
            Type type = new Type();
            type.setId(clsId);
            type.setName(typeobj.getName());
            type.setLabel(kb.getLabel(typeobj));
            vocabulary.addType(type);
        }

        // Add types not asserted
        KBObject clsObj = kb.getProperty(KBConstants.OWL_NS + "Class");
        for (KBTriple t : kb.genericTripleQuery(null, typeprop, clsObj)) {
            KBObject cls = t.getSubject();
            String clsId = cls.getID();
            if (clsId == null || !clsId.startsWith(vocabulary.getNamespace())
                    || cls.getNamespace().equals(KBConstants.OWL_NS)) {
                continue;
            }

            String desc = kb.getComment(cls);
            Type type = vocabulary.getType(clsId);
            if (type == null) {
                type = new Type();
                type.setId(clsId);
                type.setName(cls.getName());
                type.setLabel(kb.getLabel(cls));
                if (desc != null)
                    type.setDescription(desc);
                vocabulary.addType(type);
            }
        }

        // Add type hierarchy
        KBObject subClsProp = kb.getProperty(KBConstants.RDFS_NS + "subClassOf");
        for (KBTriple t : kb.genericTripleQuery(null, subClsProp, null)) {
            KBObject subCls = t.getSubject();
            KBObject cls = t.getObject();
            String clsId = cls.getID();

            Type subtype = vocabulary.getType(subCls.getID());
            if (subtype == null)
                continue;

            if (!clsId.startsWith(KBConstants.OWL_NS))
                subtype.setParent(clsId);

            Type type = vocabulary.getType(cls.getID());
            if (type != null && subtype.getId().startsWith(vocabulary.getNamespace())) {
                type.addChild(subtype.getId());
            }
        }
    }

    public static String SHAsum(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        return byteArray2Hex(md.digest(data));
    }

    private static String byteArray2Hex(final byte[] hash) {
        try (Formatter formatter = new Formatter()) {
            for (byte b : hash) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }
}