/*
 *
 *  * Copyright 2014 Orient Technologies.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.orientechnologies.test;

//import com.orientechnologies.orient.core.OOrientListenerAbstract;
import com.orientechnologies.orient.core.OOrientListener;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.engine.local.OEngineLocalPaginated;
import com.orientechnologies.orient.core.engine.memory.OEngineMemory;
import com.orientechnologies.orient.core.storage.OStorage;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Enrico Risa (e.risa-at-orientechnologies.com) on 19/09/14.
 */
@Test
public abstract class BaseLuceneTest {

  private final ExecutorService pool = Executors.newFixedThreadPool(1);
  protected ODatabaseDocument   databaseDocumentTx;
  protected OServer             server;
  protected ODatabaseDocumentTx serverDatabase;
  protected String              buildDirectory;
  private String                url;
  private boolean               remote;
  private Process               process;

  public static final class RemoteDBRunner {
    public static void main(String[] args) throws Exception {

      if (args.length > 0) {
        OServer server = OServerMain.create();
        server.startup(ClassLoader.getSystemResourceAsStream("orientdb-server-config.xml"));
        server.activate();
        final ODatabaseDocumentTx db = Orient.instance().getDatabaseFactory()
            .createDatabase("graph", getStoragePath(args[0], "plocal"));

        if (db.exists()) {
          db.open("admin", "admin");
          db.drop();
        }
        db.create();

        Orient.instance().registerListener(new OOrientListener() {
            public void onStorageRegistered(OStorage oStorage) {

            }

            public void onStorageUnregistered(OStorage oStorage) {

            }

            public void onShutdown() {
                db.drop();
            }
        } );
        while (true)
          Thread.sleep(1000);
      }

    }
  }

  public BaseLuceneTest() {
    this(false);
  }

  public BaseLuceneTest(boolean remote) {
    this.remote = remote;

  }

  protected static String getStoragePath(final String databaseName, final String storageMode) {
    final String path;
    if (storageMode.equals(OEngineLocalPaginated.NAME)) {
      path = storageMode + ":${" + Orient.ORIENTDB_HOME + "}/databases/" + databaseName;
    } else if (storageMode.equals(OEngineMemory.NAME)) {
      path = storageMode + ":" + databaseName;
    } else {
      return null;
    }
    return path;
  }

  @Test(enabled = false)
  public void initDB() {

    buildDirectory = System.getProperty("buildDirectory", ".");
    if (buildDirectory == null)
      buildDirectory = ".";

    if (remote) {
      try {

        startServer();

        url = "remote:localhost/" + getDatabaseName();
        databaseDocumentTx = new ODatabaseDocumentTx(url);
        databaseDocumentTx.open("admin", "admin");
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      url = "plocal:" + buildDirectory + "/" + getDatabaseName();
      databaseDocumentTx = new ODatabaseDocumentTx(url);
      if (!databaseDocumentTx.exists()) {
        databaseDocumentTx = Orient.instance().getDatabaseFactory().createDatabase("graph", url);
        databaseDocumentTx.create();
      } else {
        databaseDocumentTx.open("admin", "admin");
      }
    }

  }

  @Test(enabled = false)
  public void deInitDB() {
    if (remote) {
      process.destroy();

    } else {
      databaseDocumentTx.drop();
    }
  }

  protected void startServer() throws IOException, InterruptedException {
    String javaExec = System.getProperty("java.home") + "/bin/java";
    System.setProperty("ORIENTDB_HOME", buildDirectory);

    ProcessBuilder processBuilder = new ProcessBuilder(javaExec, "-Xmx2048m", "-classpath", System.getProperty("java.class.path"), "-DORIENTDB_HOME=" + buildDirectory, "test", getDatabaseName()); //RemoteDBRunner.class.getName() //todo - what shoudl this return?
    processBuilder.inheritIO();

    process = processBuilder.start();
    Thread.sleep(5000);
  }

  protected void restart() {

    process.destroy();
    try {
      startServer();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }

  protected abstract String getDatabaseName();
}
