package org.apache.solr.search.function.funcvalues;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.solr.search.function.FuncValues;
import org.apache.solr.search.function.ValueSource;
import org.apache.solr.search.function.ValueSourceScorer;
import org.apache.solr.search.mutable.MutableValue;
import org.apache.solr.search.mutable.MutableValueLong;

/**
 * Abstract {@link org.apache.solr.search.function.FuncValues} implementation which supports retrieving long values.
 * Implementations can control how the long values are loaded through {@link #longVal(int)}}
 */
public abstract class LongDocValues extends FuncValues {
  protected final ValueSource vs;

  public LongDocValues(ValueSource vs) {
    this.vs = vs;
  }

  @Override
  public float floatVal(int doc) {
    return (float) longVal(doc);
  }

  @Override
  public int intVal(int doc) {
    return (int) longVal(doc);
  }

  @Override
  public abstract long longVal(int doc);

  @Override
  public double doubleVal(int doc) {
    return (double) longVal(doc);
  }

  @Override
  public boolean boolVal(int doc) {
    return longVal(doc) != 0;
  }

  @Override
  public String strVal(int doc) {
    return Long.toString(longVal(doc));
  }

  @Override
  public Object objectVal(int doc) {
    return exists(doc) ? longVal(doc) : null;
  }

  @Override
  public String toString(int doc) {
    return vs.description() + '=' + strVal(doc);
  }

  protected long externalToLong(String extVal) {
    return Long.parseLong(extVal);
  }

  @Override
  public ValueSourceScorer getRangeScorer(AtomicReaderContext readerContext, String lowerVal, String upperVal, boolean includeLower, boolean includeUpper, boolean matchMissing) {

    // TODO: can't use this because of externalToLong...
    // return getLongRangeScorer(this, readerContext, lowerVal, upperVal, includeLower, includeUpper);

    final LongDocValues vals = this;

    long lower, upper;

    // instead of using separate comparison functions, adjust the endpoints.

    if (lowerVal == null) {
      lower = Long.MIN_VALUE;
    } else {
      lower = externalToLong(lowerVal);
      if (!includeLower && lower < Long.MAX_VALUE) lower++;
    }

    if (upperVal == null) {
      upper = Long.MAX_VALUE;
    } else {
      upper = externalToLong(upperVal);
      if (!includeUpper && upper > Long.MIN_VALUE) upper--;
    }

    final long ll = lower;
    final long uu = upper;

    final long def = 0;
    boolean checkExists = matchMissing==false && (ll <= def && uu >= def);

    if (!checkExists) {
      return new ValueSourceScorer(readerContext, vals) {
        @Override
        public boolean matchesValue(int doc) {
          long val = vals.longVal(doc);
          return val >= ll && val <= uu;
        }
      };
    } else {
      return new ValueSourceScorer(readerContext, vals) {
        @Override
        public boolean matchesValue(int doc) {
          long val = vals.longVal(doc);
          return val >= ll && val <= uu && (val != def || vals.exists(doc));
        }
      };

    }


  }

  @Override
  public ValueFiller getValueFiller() {
    return new ValueFiller() {
      private final MutableValueLong mval = new MutableValueLong();

      @Override
      public MutableValue getValue() {
        return mval;
      }

      @Override
      public void fillValue(int doc) {
        mval.value = longVal(doc);
        mval.exists = exists(doc);
      }
    };
  }
}
