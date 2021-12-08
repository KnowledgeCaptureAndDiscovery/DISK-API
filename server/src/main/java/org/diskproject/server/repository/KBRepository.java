package org.diskproject.server.repository;

import java.io.File;
import java.util.concurrent.Semaphore;
import java.util.HashMap;

import org.apache.commons.configuration.plist.PropertyListConfiguration;
import org.diskproject.server.util.Config;
import org.diskproject.shared.classes.util.KBConstants;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBObject;
import edu.isi.kcap.ontapi.OntFactory;
import edu.isi.kcap.ontapi.OntSpec;
import edu.isi.kcap.ontapi.jena.transactions.TransactionsJena;
import edu.isi.kcap.ontapi.transactions.TransactionsAPI;

public class KBRepository implements TransactionsAPI {
  protected String server;
  protected String tdbdir;
  protected String onturi;
  protected String ontns;
  protected OntFactory fac;
  protected transient TransactionsAPI transaction;
  
  protected String owlns, rdfns, rdfsns;
  protected KBAPI ontkb;
  protected HashMap<String, KBObject> pmap, cmap;

  private Semaphore mutex;
  
  protected void setConfiguration(String onturi, String ontns) {
    if(Config.get() == null)
      return;
    PropertyListConfiguration props = Config.get().getProperties();
    this.server = props.getString("server");
    tdbdir = props.getString("storage.tdb");
    File tdbdirf = new File(tdbdir);
    if(!tdbdirf.exists() && !tdbdirf.mkdirs()) {
      System.err.println("Cannot create tdb directory : "+tdbdirf.getAbsolutePath());
    }

    // TODO: Read execution engines
    owlns = KBConstants.OWLNS();
    rdfns = KBConstants.RDFNS();
    rdfsns = KBConstants.RDFSNS();
    
    this.onturi = onturi;
    this.ontns = ontns;
  }
  
  protected void initializeKB() {
    if (this.tdbdir == null)
      return;
    
    if (this.mutex == null)
        this.mutex = new Semaphore(1);
    
    this.fac = new OntFactory(OntFactory.JENA, tdbdir);
    this.transaction = new TransactionsJena(this.fac);
    this.pmap = new HashMap<String, KBObject>();
    this.cmap = new HashMap<String, KBObject>();
      
    try {
        this.ontkb = fac.getKB(this.onturi, OntSpec.PELLET, false, true);
    } catch (Exception e) {
        e.printStackTrace();
        System.out.println("Error reading KB: " + this.onturi);
        return;
    }
    
    if (this.ontkb != null) {
        this.start_write();
        this.cacheKBTerms(ontkb);
        this.end();
    } else {
        return;
    }

  }

  protected void cacheKBTerms(KBAPI kb) {
    for (KBObject obj : kb.getAllClasses()) {
      if(obj != null && obj.getName() != null) {
        cmap.put(obj.getName(), obj);
      }
    }
    for (KBObject obj : kb.getAllObjectProperties()) {
      if(obj != null) {
        pmap.put(obj.getName(), obj);
      }
    }
    for (KBObject obj : kb.getAllDatatypeProperties()) {
      if(obj != null) {
        pmap.put(obj.getName(), obj);
      }
    }
  }

  //TransactionsAPI functions
  private void acquire () {
	  if (is_in_transaction()) {
		  System.out.println("Waiting... " +  mutex.availablePermits());
		  //FIXME this is an error! check why you get here, double open probably.
	  }
	  try {
		  mutex.acquire();
	  } catch(InterruptedException ie) {
		  System.out.println("InterruptedException");
	  }
  }
  
  private void release () {
	  try {
		  mutex.release();
	  } catch (Exception e) {
		  System.out.println("Error on release");
	  }
  }
  
 @Override
 public boolean start_read() {
	 if (transaction != null) {
		 acquire();
		 return transaction.start_read();
	 }
	 return true; //true??? FIXME
 }

 @Override
 public boolean start_write() {
	 if (transaction != null) {
		 acquire();
		 return transaction.start_write();
	 }
	 return true;
 }

 @Override
 public boolean end () {
	 if (transaction != null) {
		 boolean b = transaction.end();
		 release();
		 return b;
	 }
	 return true;
 }
 
 @Override
 public boolean save(KBAPI kb) {
	 return transaction.save(kb);
 }
 
 @Override
 public boolean saveAll() {
	 return transaction.saveAll();
 }

 @Override
 public boolean start_batch_operation() {
   return transaction.start_batch_operation();
 }

 @Override
 public void stop_batch_operation() {
   transaction.stop_batch_operation();
 }
 
 @Override
 public boolean is_in_transaction() {
	 return transaction.is_in_transaction();
 }

}
