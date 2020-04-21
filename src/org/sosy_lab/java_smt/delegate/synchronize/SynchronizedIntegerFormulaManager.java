/*
 *  JavaSMT is an API wrapper for a collection of SMT solvers.
 *  This file is part of JavaSMT.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.java_smt.delegate.synchronize;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.SolverContext;

class SynchronizedIntegerFormulaManager
    extends SynchronizedNumeralFormulaManager<IntegerFormula, IntegerFormula>
    implements IntegerFormulaManager {

  private final IntegerFormulaManager delegate;

  SynchronizedIntegerFormulaManager(IntegerFormulaManager pDelegate, SolverContext pSync) {
    super(pDelegate, pSync);
    delegate = checkNotNull(pDelegate);
  }

  @Override
  public BooleanFormula modularCongruence(
      IntegerFormula pNumber1, IntegerFormula pNumber2, BigInteger pN) {
    synchronized (sync) {
      return delegate.modularCongruence(pNumber1, pNumber2, pN);
    }
  }

  @Override
  public BooleanFormula modularCongruence(
      IntegerFormula pNumber1, IntegerFormula pNumber2, long pN) {
    synchronized (sync) {
      return delegate.modularCongruence(pNumber1, pNumber2, pN);
    }
  }

  @Override
  public IntegerFormula modulo(IntegerFormula pNumber1, IntegerFormula pNumber2) {
    synchronized (sync) {
      return delegate.modulo(pNumber1, pNumber2);
    }
  }
}