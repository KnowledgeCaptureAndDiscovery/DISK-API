package org.diskproject.server.repository;

import java.util.concurrent.Semaphore;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.OntFactory;
import edu.isi.kcap.ontapi.jena.transactions.TransactionsJena;
import edu.isi.kcap.ontapi.transactions.TransactionsAPI;

public class DiskRDF {
    private Semaphore mutex;
    protected OntFactory fac;
    protected transient TransactionsAPI api;

    public DiskRDF (String tdbdir) {
        if (this.mutex == null)
            this.mutex = new Semaphore(1);
        this.fac = new OntFactory(OntFactory.JENA, tdbdir);
        this.api = new TransactionsJena(this.fac);
    }

    public OntFactory getFactory () {
        return fac;
    }

    private void acquire() {
        if (api.is_in_transaction()) {
            System.out.println("Waiting... " + mutex.availablePermits());
            // If you get here, you are deadlocked... probably double open somewhere...
            // throw new Exception("Deadlock"); This could help to solve errors...
        }
        try {
            mutex.acquire();
        } catch (InterruptedException ie) {
            System.out.println("InterruptedException");
        }
    }

    private void release() {
        try {
            mutex.release();
        } catch (Exception e) {
            System.out.println("Error on release");
        }
    }

    public boolean startRead() {
        acquire();
        return api.start_read();
    }

    public boolean startWrite() {
        acquire();
        return api.start_write();
    }

    public boolean end() {
        boolean b = api.end();
        release();
        return b;
    }

    public boolean save(KBAPI kb) {
        return api.save(kb);
    }

    public boolean saveAll() {
        return api.saveAll();
    }

    public boolean isInTransaction() {
        return api.is_in_transaction();
    }
}