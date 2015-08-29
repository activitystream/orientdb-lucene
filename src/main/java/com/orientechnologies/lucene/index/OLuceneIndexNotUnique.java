/*
 * Copyright 2014 Orient Technologies.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.orientechnologies.lucene.index;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.orientechnologies.common.listener.OProgressListener;
import com.orientechnologies.lucene.OLuceneIndex;
import com.orientechnologies.lucene.OLuceneIndexEngine;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.record.ridbag.sbtree.OIndexRIDContainer;
import com.orientechnologies.orient.core.index.ODefaultIndexFactory;
import com.orientechnologies.orient.core.index.OIndexDefinition;
import com.orientechnologies.orient.core.index.OIndexMultiValues;
import com.orientechnologies.orient.core.index.OIndexNotUnique;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.type.tree.OMVRBTreeRIDSet;

public class OLuceneIndexNotUnique extends OIndexNotUnique implements OLuceneIndex {

  public OLuceneIndexNotUnique(String name, String typeId, String algorithm, OLuceneIndexEngine engine, String valueContainerAlgorithm, ODocument metadata) {
    super(name, typeId, algorithm, engine, valueContainerAlgorithm, metadata);
  }

  @Override
  public OIndexMultiValues create(String name, OIndexDefinition indexDefinition, String clusterIndexName,
      Set<String> clustersToIndex, boolean rebuild, OProgressListener progressListener) {
    OLuceneIndexEngine engine = (OLuceneIndexEngine) indexEngine;
    engine.setManagedIndex(this);
    return super.create(name, indexDefinition, clusterIndexName, clustersToIndex, rebuild, progressListener);
  }

  @Override
  public OIndexMultiValues put(Object key, OIdentifiable iSingleValue) {
    checkForRebuild();

    key = getCollatingValue(key);

    modificationLock.requestModificationLock();
    try {
      acquireExclusiveLock();
      try {
        checkForKeyType(key);
        Set<OIdentifiable> values = new HashSet<OIdentifiable>();
        values.add(iSingleValue);
        indexEngine.put(key, values);
        return this;

      } finally {
        releaseExclusiveLock();
      }
    } finally {
      modificationLock.releaseModificationLock();
    }
  }

  @Override
  public Set<OIdentifiable> get(Object key) {
    checkForRebuild();

    key = getCollatingValue(key);

    acquireSharedLock();
    try {

      final Set<OIdentifiable> values = indexEngine.get(key);

      if (values == null)
        return Collections.emptySet();

      return values;

    } finally {
      releaseSharedLock();
    }
  }

  @Override
  public boolean remove(Object key, OIdentifiable value) {
    checkForRebuild();

    key = getCollatingValue(key);
    modificationLock.requestModificationLock();
    try {
      acquireExclusiveLock();
      try {

        if (indexEngine instanceof OLuceneIndexEngine) {
          return ((OLuceneIndexEngine) indexEngine).remove(key, value);
        } else {
          return false;
        }

      } finally {
        releaseExclusiveLock();
      }
    } finally {
      modificationLock.releaseModificationLock();
    }
  }

  @Override
  public long rebuild(OProgressListener iProgressListener) {

    long size = 0;
    OLuceneIndexEngine engine = (OLuceneIndexEngine) indexEngine;
    try {
      engine.setRebuilding(true);
      super.rebuild(iProgressListener);
    } finally {
      engine.setRebuilding(false);
    }

    return size;

  }
}
