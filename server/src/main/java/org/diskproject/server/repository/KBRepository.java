package org.diskproject.server.repository;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.apache.commons.configuration.plist.PropertyListConfiguration;
import org.diskproject.server.adapters.DataAdapter;
import org.diskproject.server.adapters.MethodAdapter;
import org.diskproject.server.util.Config;
import org.diskproject.server.util.KBCache;
import org.diskproject.shared.classes.util.KBConstants;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.OntFactory;
import edu.isi.kcap.ontapi.OntSpec;
import edu.isi.kcap.ontapi.jena.transactions.TransactionsJena;
import edu.isi.kcap.ontapi.transactions.TransactionsAPI;

public class KBRepository implements TransactionsAPI {
  protected String server;
  protected String tdbdir;
  protected OntFactory fac;
  protected transient TransactionsAPI transaction;
  protected KBAPI ontKB;
  private Semaphore mutex;
  protected KBCache DISKOnt;

  protected Map<String, DataAdapter> dataAdapters;
  protected Map<String, MethodAdapter> methodAdapters;

  protected void setConfiguration() {
    if(Config.get() == null)
      return;
    PropertyListConfiguration props = Config.get().getProperties();
    this.server = props.getString("server");
    tdbdir = props.getString("storage.tdb");
    File tdbdirF = new File(tdbdir);
    if(!tdbdirF.exists() && !tdbdirF.mkdirs()) {
      System.err.println("Cannot create tdb directory : " + tdbdirF.getAbsolutePath());
    }
  }
  
  protected void initializeKB() {
    if (this.tdbdir == null)
      return;
    
    if (this.mutex == null)
        this.mutex = new Semaphore(1);
    
    this.fac = new OntFactory(OntFactory.JENA, tdbdir);
    this.transaction = new TransactionsJena(this.fac);
      
    try {
        this.ontKB = fac.getKB(KBConstants.DISKURI(), OntSpec.PELLET, false, true);
    } catch (Exception e) {
        e.printStackTrace();
        System.out.println("Error reading KB: " + KBConstants.DISKURI());
        return;
    }
    
    if (this.ontKB != null) {
        this.start_read();
        this.DISKOnt = new KBCache(ontKB);
        this.end();
    } else {
        return;
    }

  }

  public MethodAdapter getMethodAdapterByName (String source) {
      for (MethodAdapter adapter: this.methodAdapters.values()) {
          if (adapter.getName().equals(source))
              return adapter;
      }
      return null;
  }

  //TransactionsAPI functions
  private void acquire () {
    if (is_in_transaction()) {
      System.out.println("Waiting... " +  mutex.availablePermits());
      //If you get here, you are deadlocked... probably double open somewhere... 
      //throw new Exception("Deadlock"); This could help to solve errors...
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
    return true; //true??
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
