// This file is part of JavaSMT,
// an API wrapper for a collection of SMT solvers:
// https://github.com/sosy-lab/java-smt
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.java_smt.test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.truth.Truth.assert_;
import static com.google.common.truth.TruthJUnit.assume;
import static org.sosy_lab.java_smt.api.FormulaType.IntegerType;
import static org.sosy_lab.java_smt.test.ProverEnvironmentSubject.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BasicProverEnvironment;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.ArrayFormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.RationalFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

/** Test that values from models are appropriately parsed. */
@RunWith(Parameterized.class)
public class ModelTest extends SolverBasedTest0 {

  private static final ArrayFormulaType<IntegerFormula, IntegerFormula> ARRAY_TYPE_INT_INT =
      FormulaType.getArrayType(IntegerType, IntegerType);

  private static final ImmutableList<Solvers> SOLVERS_WITH_PARTIAL_MODEL =
      ImmutableList.of(Solvers.Z3, Solvers.PRINCESS);

  @Parameters(name = "{0}")
  public static Object[] getAllSolvers() {
    return Solvers.values();
  }

  @Parameter public Solvers solver;

  @Override
  protected Solvers solverToUse() {
    return solver;
  }

  @Before
  public void setup() {
    requireModel();
  }

  @Test
  public void testEmpty() throws SolverException, InterruptedException {
    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      assertThat(prover).isSatisfiable();

      try (Model m = prover.getModel()) {
        assertThat(m).isEmpty();
      }

      assertThat(prover.getModelAssignments()).isEmpty();
    }
  }

  @Test
  public void testOnlyTrue() throws SolverException, InterruptedException {
    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(bmgr.makeTrue());
      assertThat(prover).isSatisfiable();

      try (Model m = prover.getModel()) {
        assertThat(m).isEmpty();
      }

      assertThat(prover.getModelAssignments()).isEmpty();
    }
  }

  @Test
  public void testGetSmallIntegers() throws SolverException, InterruptedException {
    // Boolector only supports Bitvectors (bv arrays and ufs)
    assume().that(solverToUse()).isNotEqualTo(Solvers.BOOLECTOR);
    testModelGetters(
        imgr.equal(imgr.makeVariable("x"), imgr.makeNumber(10)),
        imgr.makeVariable("x"),
        BigInteger.valueOf(10),
        "x");
  }

  @Test
  public void testGetNegativeIntegers() throws SolverException, InterruptedException {
    // Boolector only supports Bitvectors (bv arrays and ufs)
    assume().that(solverToUse()).isNotEqualTo(Solvers.BOOLECTOR);
    testModelGetters(
        imgr.equal(imgr.makeVariable("x"), imgr.makeNumber(-10)),
        imgr.makeVariable("x"),
        BigInteger.valueOf(-10),
        "x");
  }

  @Test
  public void testGetLargeIntegers() throws SolverException, InterruptedException {
    // Boolector only supports Bitvectors (bv arrays and ufs)
    assume().that(solverToUse()).isNotEqualTo(Solvers.BOOLECTOR);
    BigInteger large = new BigInteger("1000000000000000000000000000000000000000");
    testModelGetters(
        imgr.equal(imgr.makeVariable("x"), imgr.makeNumber(large)),
        imgr.makeVariable("x"),
        large,
        "x");
  }

  @Test
  public void testGetSmallIntegralRationals() throws SolverException, InterruptedException {
    // Boolector only supports Bitvectors (bv arrays and ufs)
    assume().that(solverToUse()).isNotEqualTo(Solvers.BOOLECTOR);
    requireRationals();
    testModelGetters(
        rmgr.equal(rmgr.makeVariable("x"), rmgr.makeNumber(1)),
        rmgr.makeVariable("x"),
        Rational.ONE,
        "x");
  }

  @Test
  public void testGetLargeIntegralRationals() throws SolverException, InterruptedException {
    // Boolector only supports Bitvectors (bv arrays and ufs)
    assume().that(solverToUse()).isNotEqualTo(Solvers.BOOLECTOR);
    requireRationals();
    BigInteger large = new BigInteger("1000000000000000000000000000000000000000");
    testModelGetters(
        rmgr.equal(rmgr.makeVariable("x"), rmgr.makeNumber(large)),
        rmgr.makeVariable("x"),
        Rational.ofBigInteger(large),
        "x");
  }

  @Test
  public void testGetRationals() throws SolverException, InterruptedException {
    // Boolector only supports Bitvectors (bv arrays and ufs)
    assume().that(solverToUse()).isNotEqualTo(Solvers.BOOLECTOR);
    requireRationals();
    testModelGetters(
        rmgr.equal(rmgr.makeVariable("x"), rmgr.makeNumber(Rational.ofString("1/3"))),
        rmgr.makeVariable("x"),
        Rational.ofString("1/3"),
        "x");
  }

  @Test
  public void testGetBooleans() throws SolverException, InterruptedException {
    for (String name : new String[] {"x", "x-x", "x::x"}) {
      testModelGetters(bmgr.makeVariable(name), bmgr.makeBoolean(true), true, name);
    }
  }

  @Test
  public void testGetUFs() throws SolverException, InterruptedException {
    // Boolector does not support integers
    if (imgr != null) {
      IntegerFormula x =
          fmgr.declareAndCallUF("UF", IntegerType, ImmutableList.of(imgr.makeVariable("arg")));
      testModelGetters(imgr.equal(x, imgr.makeNumber(1)), x, BigInteger.ONE, "UF");
    } else {
      BitvectorFormula x =
          fmgr.declareAndCallUF(
              "UF",
              FormulaType.getBitvectorTypeWithSize(8),
              ImmutableList.of(bvmgr.makeVariable(8, "arg")));
      testModelGetters(bvmgr.equal(x, bvmgr.makeBitvector(8, 1)), x, BigInteger.ONE, "UF");
    }
  }

  @Test
  public void testGetUFwithMoreParams() throws Exception {
    // Boolector does not support integers
    if (imgr != null) {
      IntegerFormula x =
          fmgr.declareAndCallUF(
              "UF",
              IntegerType,
              ImmutableList.of(imgr.makeVariable("arg1"), imgr.makeVariable("arg2")));
      testModelGetters(imgr.equal(x, imgr.makeNumber(1)), x, BigInteger.ONE, "UF");
    } else {
      BitvectorFormula x =
          fmgr.declareAndCallUF(
              "UF",
              FormulaType.getBitvectorTypeWithSize(8),
              ImmutableList.of(bvmgr.makeVariable(8, "arg1"), bvmgr.makeVariable(8, "arg2")));
      testModelGetters(bvmgr.equal(x, bvmgr.makeBitvector(8, 1)), x, BigInteger.ONE, "UF");
    }
  }

  @Test
  public void testGetMultipleUFsWithInts() throws Exception {
    requireIntegers();
    IntegerFormula arg1 = imgr.makeVariable("arg1");
    IntegerFormula arg2 = imgr.makeVariable("arg2");
    FunctionDeclaration<IntegerFormula> declaration =
        fmgr.declareUF("UF", IntegerType, IntegerType);
    IntegerFormula app1 = fmgr.callUF(declaration, arg1);
    IntegerFormula app2 = fmgr.callUF(declaration, arg2);

    IntegerFormula one = imgr.makeNumber(1);
    IntegerFormula two = imgr.makeNumber(2);
    IntegerFormula three = imgr.makeNumber(3);
    IntegerFormula four = imgr.makeNumber(4);

    ImmutableList<ValueAssignment> expectedModel =
        ImmutableList.of(
            new ValueAssignment(
                arg1,
                three,
                imgr.equal(arg1, three),
                "arg1",
                BigInteger.valueOf(3),
                ImmutableList.of()),
            new ValueAssignment(
                arg2,
                four,
                imgr.equal(arg2, four),
                "arg2",
                BigInteger.valueOf(4),
                ImmutableList.of()),
            new ValueAssignment(
                fmgr.callUF(declaration, three),
                one,
                imgr.equal(fmgr.callUF(declaration, three), one),
                "UF",
                BigInteger.valueOf(1),
                ImmutableList.of(BigInteger.valueOf(3))),
            new ValueAssignment(
                fmgr.callUF(declaration, four),
                imgr.makeNumber(2),
                imgr.equal(fmgr.callUF(declaration, four), two),
                "UF",
                BigInteger.valueOf(2),
                ImmutableList.of(BigInteger.valueOf(4))));

    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(
          bmgr.and(imgr.equal(app1, imgr.makeNumber(1)), imgr.equal(app2, imgr.makeNumber(2))));
      prover.push(imgr.equal(arg1, imgr.makeNumber(3)));
      prover.push(imgr.equal(arg2, imgr.makeNumber(4)));

      assertThat(prover).isSatisfiable();

      try (Model m = prover.getModel()) {
        assertThat(m.evaluate(app1)).isEqualTo(BigInteger.ONE);
        assertThat(m.evaluate(app2)).isEqualTo(BigInteger.valueOf(2));
        assertThat(m).containsExactlyElementsIn(expectedModel);
      }
      assertThat(prover.getModelAssignments()).containsExactlyElementsIn(expectedModel);
    }
  }

  @Test
  public void testGetMultipleUFsWithBvs() throws Exception {
    requireBitvectors();
    BitvectorFormula arg1 = bvmgr.makeVariable(8, "arg1");
    BitvectorFormula arg2 = bvmgr.makeVariable(8, "arg2");
    FunctionDeclaration<BitvectorFormula> declaration =
        fmgr.declareUF(
            "UF", FormulaType.getBitvectorTypeWithSize(8), FormulaType.getBitvectorTypeWithSize(8));
    BitvectorFormula app1 = fmgr.callUF(declaration, arg1);
    BitvectorFormula app2 = fmgr.callUF(declaration, arg2);

    BitvectorFormula one = bvmgr.makeBitvector(8, 1);
    BitvectorFormula two = bvmgr.makeBitvector(8, 2);
    BitvectorFormula three = bvmgr.makeBitvector(8, 3);
    BitvectorFormula four = bvmgr.makeBitvector(8, 4);

    ImmutableList<ValueAssignment> expectedModel =
        ImmutableList.of(
            new ValueAssignment(
                arg1,
                three,
                bvmgr.equal(arg1, three),
                "arg1",
                BigInteger.valueOf(3),
                ImmutableList.of()),
            new ValueAssignment(
                arg2,
                four,
                bvmgr.equal(arg2, four),
                "arg2",
                BigInteger.valueOf(4),
                ImmutableList.of()),
            new ValueAssignment(
                fmgr.callUF(declaration, three),
                one,
                bvmgr.equal(fmgr.callUF(declaration, three), one),
                "UF",
                BigInteger.valueOf(1),
                ImmutableList.of(BigInteger.valueOf(3))),
            new ValueAssignment(
                fmgr.callUF(declaration, four),
                bvmgr.makeBitvector(8, 2),
                bvmgr.equal(fmgr.callUF(declaration, four), two),
                "UF",
                BigInteger.valueOf(2),
                ImmutableList.of(BigInteger.valueOf(4))));

    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(
          bmgr.and(
              bvmgr.equal(app1, bvmgr.makeBitvector(8, 1)),
              bvmgr.equal(app2, bvmgr.makeBitvector(8, 2))));
      prover.push(bvmgr.equal(arg1, bvmgr.makeBitvector(8, 3)));
      prover.push(bvmgr.equal(arg2, bvmgr.makeBitvector(8, 4)));

      assertThat(prover).isSatisfiable();

      try (Model m = prover.getModel()) {
        assertThat(m.evaluate(app1)).isEqualTo(BigInteger.ONE);
        assertThat(m.evaluate(app2)).isEqualTo(BigInteger.valueOf(2));
        assertThat(m).containsExactlyElementsIn(expectedModel);
      }
      assertThat(prover.getModelAssignments()).containsExactlyElementsIn(expectedModel);
    }
  }

  @Test
  public void testQuantifiedUF() throws SolverException, InterruptedException {
    requireQuantifiers();
    // Boolector only supports bitvector quantifier
    assume().that(solverToUse()).isNotEqualTo(Solvers.BOOLECTOR);

    IntegerFormula var = imgr.makeVariable("var");
    BooleanFormula varIsOne = imgr.equal(var, imgr.makeNumber(1));
    IntegerFormula boundVar = imgr.makeVariable("boundVar");
    BooleanFormula boundVarIsZero = imgr.equal(boundVar, imgr.makeNumber(0));

    String func = "func";
    IntegerFormula funcAtZero = fmgr.declareAndCallUF(func, IntegerType, imgr.makeNumber(0));
    IntegerFormula funcAtBoundVar = fmgr.declareAndCallUF(func, IntegerType, boundVar);

    BooleanFormula body = bmgr.and(boundVarIsZero, imgr.equal(var, funcAtBoundVar));
    BooleanFormula f = bmgr.and(varIsOne, qmgr.exists(ImmutableList.of(boundVar), body));
    IntegerFormula one = imgr.makeNumber(1);

    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(f);
      assertThat(prover).isSatisfiable();

      try (Model m = prover.getModel()) {
        for (@SuppressWarnings("unused") ValueAssignment assignment : m) {
          // Check that we can iterate through with no crashes.
        }
        assertThat(m)
            .contains(
                new ValueAssignment(
                    funcAtZero,
                    one,
                    imgr.equal(funcAtZero, one),
                    func,
                    BigInteger.ONE,
                    ImmutableList.of(BigInteger.ZERO)));
      }
    }
  }

  @Test
  public void testGetBitvectors() throws SolverException, InterruptedException {
    requireBitvectors();
    testModelGetters(
        bvmgr.equal(bvmgr.makeVariable(1, "x"), bvmgr.makeBitvector(1, BigInteger.ONE)),
        bvmgr.makeVariable(1, "x"),
        BigInteger.ONE,
        "x");
  }

  @Test
  public void testGetModelAssignments() throws SolverException, InterruptedException {
    if (imgr != null) {
      testModelIterator(
          bmgr.and(
              imgr.equal(imgr.makeVariable("x"), imgr.makeNumber(1)),
              imgr.equal(imgr.makeVariable("x"), imgr.makeVariable("y"))));
    } else {
      testModelIterator(
          bmgr.and(
              bvmgr.equal(bvmgr.makeVariable(8, "x"), bvmgr.makeBitvector(8, 1)),
              bvmgr.equal(bvmgr.makeVariable(8, "x"), bvmgr.makeVariable(8, "y"))));
    }
  }

  @Test
  public void testEmptyStackModel() throws SolverException, InterruptedException {
    if (imgr != null) {
      try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
        assertThat(prover).isSatisfiable();
        try (Model m = prover.getModel()) {
          assertThat(m.evaluate(imgr.makeNumber(123))).isEqualTo(BigInteger.valueOf(123));
          assertThat(m.evaluate(bmgr.makeBoolean(true))).isTrue();
          assertThat(m.evaluate(bmgr.makeBoolean(false))).isFalse();
          if (SOLVERS_WITH_PARTIAL_MODEL.contains(solver)) {
            // partial model should not return an evaluation
            assertThat(m.evaluate(imgr.makeVariable("y"))).isNull();
          } else {
            assertThat(m.evaluate(imgr.makeVariable("y"))).isNotNull();
          }
        }
      }
    } else {
      try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
        assertThat(prover).isSatisfiable();
        try (Model m = prover.getModel()) {
          assertThat(m.evaluate(bvmgr.makeBitvector(8, 123))).isEqualTo(BigInteger.valueOf(123));
          assertThat(m.evaluate(bmgr.makeBoolean(true))).isTrue();
          assertThat(m.evaluate(bmgr.makeBoolean(false))).isFalse();
          if (SOLVERS_WITH_PARTIAL_MODEL.contains(solver)) {
            // partial model should not return an evaluation
            assertThat(m.evaluate(bvmgr.makeVariable(8, "y"))).isNull();
          } else {
            assertThat(m.evaluate(bvmgr.makeVariable(8, "y"))).isNotNull();
          }
        }
      }
    }
  }

  @Test
  public void testNonExistantSymbol() throws SolverException, InterruptedException {
    if (imgr != null) {
      try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
        prover.push(bmgr.makeBoolean(true));
        assertThat(prover).isSatisfiable();

        try (Model m = prover.getModel()) {
          if (SOLVERS_WITH_PARTIAL_MODEL.contains(solver)) {
            // partial model should not return an evaluation
            assertThat(m.evaluate(imgr.makeVariable("y"))).isNull();
          } else {
            assertThat(m.evaluate(imgr.makeVariable("y"))).isNotNull();
          }
        }
      }
    } else {
      try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
        prover.push(bmgr.makeBoolean(true));
        assertThat(prover).isSatisfiable();

        try (Model m = prover.getModel()) {
          if (SOLVERS_WITH_PARTIAL_MODEL.contains(solver)) {
            // partial model should not return an evaluation
            assertThat(m.evaluate(bvmgr.makeVariable(8, "y"))).isNull();
          } else {
            assertThat(m.evaluate(bvmgr.makeVariable(8, "y"))).isNotNull();
          }
        }
      }
    }
  }

  @Test
  public void testPartialModels() throws SolverException, InterruptedException {
    assume()
        .withMessage("As of now, only Z3 and Princess support partial models")
        .that(solver)
        .isIn(SOLVERS_WITH_PARTIAL_MODEL);
    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      IntegerFormula x = imgr.makeVariable("x");
      prover.push(imgr.equal(x, x));
      assertThat(prover).isSatisfiable();
      try (Model m = prover.getModel()) {
        assertThat(m.evaluate(x)).isEqualTo(null);
        assertThat(m).isEmpty();
      }
    }
  }

  @Test
  public void testPartialModels2() throws SolverException, InterruptedException {
    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      if (imgr != null) {
        IntegerFormula x = imgr.makeVariable("x");
        prover.push(imgr.greaterThan(x, imgr.makeNumber(0)));
        assertThat(prover).isSatisfiable();
        try (Model m = prover.getModel()) {
          assertThat(m.evaluate(x)).isEqualTo(BigInteger.ONE);
          // it works now, but maybe the model "x=1" for the constraint "x>0" is not valid for new
          // solvers.
        }
      } else {
        BitvectorFormula x = bvmgr.makeVariable(8, "x");
        prover.push(bvmgr.greaterThan(x, bvmgr.makeBitvector(8, 0), true));
        assertThat(prover).isSatisfiable();
        try (Model m = prover.getModel()) {
          assertThat(m.evaluate(x)).isEqualTo(BigInteger.ONE);
          // it works now, but maybe the model "x=1" for the constraint "x>0" is not valid for new
          // solvers.
        }
      }
    }
  }

  @Test
  public void testPartialModelsUF() throws SolverException, InterruptedException {
    assume()
        .withMessage("As of now, only Z3 and Princess support partial model evaluation")
        .that(solver)
        .isIn(ImmutableList.of(Solvers.Z3, Solvers.PRINCESS));
    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      IntegerFormula x = imgr.makeVariable("x");
      IntegerFormula f = fmgr.declareAndCallUF("f", IntegerType, x);

      prover.push(imgr.equal(x, imgr.makeNumber(1)));
      assertThat(prover).isSatisfiable();

      try (Model m = prover.getModel()) {
        assertThat(m.evaluate(f)).isEqualTo(null);
      }
    }
  }

  @Test
  public void testEvaluatingConstants() throws SolverException, InterruptedException {
    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(bmgr.makeVariable("b"));
      assertThat(prover.isUnsat()).isFalse();
      try (Model m = prover.getModel()) {
        if (imgr != null) {
          assertThat(m.evaluate(imgr.makeNumber(0))).isEqualTo(BigInteger.ZERO);
          assertThat(m.evaluate(imgr.makeNumber(1))).isEqualTo(BigInteger.ONE);
          assertThat(m.evaluate(imgr.makeNumber(100))).isEqualTo(BigInteger.valueOf(100));
          assertThat(m.evaluate(bmgr.makeBoolean(true))).isTrue();
          assertThat(m.evaluate(bmgr.makeBoolean(false))).isFalse();
        }
        if (bvmgr != null) {
          if (solver == Solvers.BOOLECTOR) {
            for (int i : new int[] {2, 4, 8, 32, 64, 1000}) {
              assertThat(m.evaluate(bvmgr.makeBitvector(i, 0))).isEqualTo(BigInteger.ZERO);
              assertThat(m.evaluate(bvmgr.makeBitvector(i, 1))).isEqualTo(BigInteger.ONE);
            }
          } else {
            for (int i : new int[] {1, 2, 4, 8, 32, 64, 1000}) {
              assertThat(m.evaluate(bvmgr.makeBitvector(i, 0))).isEqualTo(BigInteger.ZERO);
              assertThat(m.evaluate(bvmgr.makeBitvector(i, 1))).isEqualTo(BigInteger.ONE);
            }
          }
        }
      }
    }
  }

  @Test
  public void testEvaluatingConstantsWithOperation() throws SolverException, InterruptedException {
    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(bmgr.makeVariable("b"));
      assertThat(prover.isUnsat()).isFalse();
      try (Model m = prover.getModel()) {
        if (imgr != null) {
          assertThat(m.evaluate(imgr.add(imgr.makeNumber(45), imgr.makeNumber(55))))
              .isEqualTo(BigInteger.valueOf(100));
          assertThat(m.evaluate(imgr.subtract(imgr.makeNumber(123), imgr.makeNumber(23))))
              .isEqualTo(BigInteger.valueOf(100));
          assertThat(m.evaluate(bmgr.and(bmgr.makeBoolean(true), bmgr.makeBoolean(true)))).isTrue();
        }
        if (bvmgr != null) {
          if (solver == Solvers.BOOLECTOR) {
            for (int i : new int[] {2, 4, 8, 32, 64, 1000}) {
              BitvectorFormula zero = bvmgr.makeBitvector(i, 0);
              BitvectorFormula one = bvmgr.makeBitvector(i, 1);
              assertThat(m.evaluate(bvmgr.add(zero, zero))).isEqualTo(BigInteger.ZERO);
              assertThat(m.evaluate(bvmgr.add(zero, one))).isEqualTo(BigInteger.ONE);
              assertThat(m.evaluate(bvmgr.subtract(one, one))).isEqualTo(BigInteger.ZERO);
              assertThat(m.evaluate(bvmgr.subtract(one, zero))).isEqualTo(BigInteger.ONE);
            }
          } else {
            for (int i : new int[] {1, 2, 4, 8, 32, 64, 1000}) {
              BitvectorFormula zero = bvmgr.makeBitvector(i, 0);
              BitvectorFormula one = bvmgr.makeBitvector(i, 1);
              assertThat(m.evaluate(bvmgr.add(zero, zero))).isEqualTo(BigInteger.ZERO);
              assertThat(m.evaluate(bvmgr.add(zero, one))).isEqualTo(BigInteger.ONE);
              assertThat(m.evaluate(bvmgr.subtract(one, one))).isEqualTo(BigInteger.ZERO);
              assertThat(m.evaluate(bvmgr.subtract(one, zero))).isEqualTo(BigInteger.ONE);
            }
          }
        }
      }
    }
  }

  @Test
  public void testNonVariableValues() throws SolverException, InterruptedException {
    requireArrays();
    requireIntegers();

    ArrayFormula<IntegerFormula, IntegerFormula> array1 =
        amgr.makeArray("array", IntegerType, IntegerType);

    IntegerFormula selected = amgr.select(array1, imgr.makeNumber(1));
    BooleanFormula selectEq0 = imgr.equal(selected, imgr.makeNumber(0));
    // Note that store is not an assignment that works beyond the section where you put it!
    IntegerFormula select1Store7in1 =
        amgr.select(amgr.store(array1, imgr.makeNumber(1), imgr.makeNumber(7)), imgr.makeNumber(1));
    BooleanFormula selectStoreEq1 = imgr.equal(select1Store7in1, imgr.makeNumber(1));

    IntegerFormula select1Store7in1store7in2 =
        amgr.select(
            amgr.store(
                amgr.store(array1, imgr.makeNumber(2), imgr.makeNumber(7)),
                imgr.makeNumber(1),
                imgr.makeNumber(7)),
            imgr.makeNumber(1));
    IntegerFormula select1Store1in1 =
        amgr.select(amgr.store(array1, imgr.makeNumber(1), imgr.makeNumber(1)), imgr.makeNumber(1));
    IntegerFormula arithIs7 =
        imgr.add(imgr.add(imgr.makeNumber(5), select1Store1in1), select1Store1in1);

    // (arr[1] = 7)[1] = 1 -> ...
    // false -> doesn't matter
    BooleanFormula assert1 = bmgr.implication(selectStoreEq1, selectEq0);
    // (arr[1] = 7)[1] != 1 -> ((arr[2] = 7)[1] = 7)[1] = 7 is true
    BooleanFormula assert2 =
        bmgr.implication(bmgr.not(selectStoreEq1), imgr.equal(select1Store7in1store7in2, arithIs7));

    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      // make the right part of the impl in assert1 fail such that the left is negated
      prover.push(bmgr.not(selectEq0));
      prover.push(assert1);
      prover.push(assert2);

      assertThat(prover).isSatisfiable();

      try (Model m = prover.getModel()) {
        for (@SuppressWarnings("unused") ValueAssignment assignment : m) {
          // Check that we can iterate through with no crashes.
        }
        assertThat(m.evaluate(select1Store7in1)).isEqualTo(BigInteger.valueOf(7));
        assertThat(m.evaluate(select1Store7in1store7in2)).isEqualTo(BigInteger.valueOf(7));
        assertThat(m.evaluate(selected)).isNotEqualTo(BigInteger.valueOf(0));
        assertThat(m.evaluate(arithIs7)).isEqualTo(BigInteger.valueOf(7));
      }
    }
  }

  @Test
  public void testNonVariableValues2() throws SolverException, InterruptedException {
    requireArrays();
    requireIntegers();

    ArrayFormula<IntegerFormula, IntegerFormula> array1 =
        amgr.makeArray("array", IntegerType, IntegerType);

    IntegerFormula selected = amgr.select(array1, imgr.makeNumber(1));
    BooleanFormula selectEq0 = imgr.equal(selected, imgr.makeNumber(0));
    // Note that store is not an assignment that works beyond the section where you put it!
    IntegerFormula select1Store7in1 =
        amgr.select(amgr.store(array1, imgr.makeNumber(1), imgr.makeNumber(7)), imgr.makeNumber(1));
    BooleanFormula selectStoreEq1 = imgr.equal(select1Store7in1, imgr.makeNumber(1));

    IntegerFormula select1Store7in1store3in1 =
        amgr.select(
            amgr.store(
                amgr.store(array1, imgr.makeNumber(1), imgr.makeNumber(3)),
                imgr.makeNumber(1),
                imgr.makeNumber(7)),
            imgr.makeNumber(1));
    IntegerFormula select1Store1in1 =
        amgr.select(amgr.store(array1, imgr.makeNumber(1), imgr.makeNumber(1)), imgr.makeNumber(1));
    IntegerFormula arithIs7 =
        imgr.add(imgr.add(imgr.makeNumber(5), select1Store1in1), select1Store1in1);

    // (arr[1] = 7)[1] = 1 -> ...
    // false -> doesn't matter
    BooleanFormula assert1 = bmgr.implication(selectStoreEq1, selectEq0);
    // (arr[1] = 7)[1] != 1 -> ((arr[1] = 3)[1] = 7)[1] = 7 is true
    BooleanFormula assert2 =
        bmgr.implication(bmgr.not(selectStoreEq1), imgr.equal(select1Store7in1store3in1, arithIs7));

    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      // make the right part of the impl in assert1 fail such that the left is negated
      prover.push(bmgr.not(selectEq0));
      prover.push(assert1);
      prover.push(assert2);

      assertThat(prover).isSatisfiable();

      try (Model m = prover.getModel()) {
        for (@SuppressWarnings("unused") ValueAssignment assignment : m) {
          // Check that we can iterate through with no crashes.
        }
        assertThat(m.evaluate(select1Store7in1)).isEqualTo(BigInteger.valueOf(7));
        assertThat(m.evaluate(select1Store7in1store3in1)).isEqualTo(BigInteger.valueOf(7));
        assertThat(m.evaluate(selected)).isNotEqualTo(BigInteger.valueOf(0));
        assertThat(m.evaluate(arithIs7)).isEqualTo(BigInteger.valueOf(7));
      }
    }
  }

  @Test
  public void testGetIntArrays() throws SolverException, InterruptedException {
    requireArrays();
    requireIntegers();
    ArrayFormula<IntegerFormula, IntegerFormula> array =
        amgr.makeArray("array", IntegerType, IntegerType);
    ArrayFormula<IntegerFormula, IntegerFormula> updated =
        amgr.store(array, imgr.makeNumber(1), imgr.makeNumber(1));
    IntegerFormula selected = amgr.select(updated, imgr.makeNumber(1));

    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(imgr.equal(selected, imgr.makeNumber(1)));

      assertThat(prover).isSatisfiable();

      try (Model m = prover.getModel()) {
        for (@SuppressWarnings("unused") ValueAssignment assignment : m) {
          // Check that we can iterate through with no crashes.
        }
        assertThat(m.evaluate(selected)).isEqualTo(BigInteger.ONE);

        // check that model evaluation applies formula simplification or constant propagation.
        ArrayFormula<IntegerFormula, IntegerFormula> stored = array;
        for (int i = 0; i < 10; i++) {
          stored = amgr.store(stored, imgr.makeNumber(i), imgr.makeNumber(i));
          // zero is the inner element of array
          assertThat(m.evaluate(amgr.select(stored, imgr.makeNumber(0))))
              .isEqualTo(BigInteger.ZERO);
          // i is the outer element of array
          assertThat(m.evaluate(amgr.select(stored, imgr.makeNumber(i))))
              .isEqualTo(BigInteger.valueOf(i));
        }
      }
    }
  }

  @Test
  public void testGetArrays2() throws SolverException, InterruptedException {
    requireParser();
    requireArrays();
    requireBitvectors();
    assume()
        .withMessage(
            "Solver %s does not support array theory with bitvectors as indices or elements",
            solverToUse())
        .that(solver)
        .isNotEqualTo(Solvers.PRINCESS);

    ArrayFormula<BitvectorFormula, BitvectorFormula> array =
        amgr.makeArray(
            "array",
            FormulaType.getBitvectorTypeWithSize(8),
            FormulaType.getBitvectorTypeWithSize(8));
    ArrayFormula<BitvectorFormula, BitvectorFormula> updated =
        amgr.store(array, bvmgr.makeBitvector(8, 1), bvmgr.makeBitvector(8, 1));

    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(
          bvmgr.equal(amgr.select(updated, bvmgr.makeBitvector(8, 1)), bvmgr.makeBitvector(8, 1)));

      assertThat(prover).isSatisfiable();

      try (Model m = prover.getModel()) {
        for (@SuppressWarnings("unused") ValueAssignment assignment : m) {
          // Check that we can iterate through with no crashes.
        }
        assertThat(m.evaluate(amgr.select(updated, bvmgr.makeBitvector(8, 1))))
            .isEqualTo(BigInteger.ONE);
      }
    }
  }

  @Test
  public void testGetArrays6() throws SolverException, InterruptedException {
    requireArrays();
    requireParser();
    BooleanFormula f =
        mgr.parse(
            "(declare-fun |pi@2| () Int)\n"
                + "(declare-fun *unsigned_int@1 () (Array Int Int))\n"
                + "(declare-fun |z2@2| () Int)\n"
                + "(declare-fun |z1@2| () Int)\n"
                + "(declare-fun |t@2| () Int)\n"
                + "(declare-fun |__ADDRESS_OF_t| () Int)\n"
                + "(declare-fun *char@1 () (Array Int Int))\n"
                + "(assert"
                + "    (and (= |t@2| 50)"
                + "        (not (<= |__ADDRESS_OF_t| 0))"
                + "        (= |z1@2| |__ADDRESS_OF_t|)"
                + "        (= (select *char@1 |__ADDRESS_OF_t|) |t@2|)"
                + "        (= |z2@2| |z1@2|)"
                + "        (= |pi@2| |z2@2|)"
                + "        (not (= (select *unsigned_int@1 |pi@2|) 50))))");

    testModelIterator(f);
  }

  @Test
  public void testGetArrays3() throws SolverException, InterruptedException {
    requireParser();
    requireArrays();
    assume()
        .withMessage("As of now, only Princess does not support multi-dimensional arrays")
        .that(solver)
        .isNotSameInstanceAs(Solvers.PRINCESS);

    // create formula for "arr[5][3][1]==x && x==123"
    BooleanFormula f =
        mgr.parse(
            "(declare-fun x () Int)\n"
                + "(declare-fun arr () (Array Int (Array Int (Array Int Int))))\n"
                + "(assert (and"
                + "    (= (select (select (select arr 5) 3) 1) x)"
                + "    (= x 123)"
                + "))");

    testModelIterator(f);
    testModelGetters(f, imgr.makeVariable("x"), BigInteger.valueOf(123), "x");
    ArrayFormulaType<
            IntegerFormula,
            ArrayFormula<IntegerFormula, ArrayFormula<IntegerFormula, IntegerFormula>>>
        arrType =
            FormulaType.getArrayType(
                IntegerType, FormulaType.getArrayType(IntegerType, ARRAY_TYPE_INT_INT));
    testModelGetters(
        f,
        amgr.select(
            amgr.select(
                amgr.select(amgr.makeArray("arr", arrType), imgr.makeNumber(5)),
                imgr.makeNumber(3)),
            imgr.makeNumber(1)),
        BigInteger.valueOf(123),
        "arr",
        true);
  }

  @Test
  public void testGetArrays4() throws SolverException, InterruptedException {
    requireParser();
    requireArrays();

    // create formula for "arr[5]==x && x==123"
    BooleanFormula f =
        mgr.parse(
            "(declare-fun x () Int)\n"
                + "(declare-fun arr () (Array Int Int))\n"
                + "(assert (and"
                + "    (= (select arr 5) x)"
                + "    (= x 123)"
                + "))");

    testModelIterator(f);
    testModelGetters(f, imgr.makeVariable("x"), BigInteger.valueOf(123), "x");
    testModelGetters(
        f,
        amgr.select(amgr.makeArray("arr", ARRAY_TYPE_INT_INT), imgr.makeNumber(5)),
        BigInteger.valueOf(123),
        "arr",
        true);
  }

  @Test(expected = IllegalArgumentException.class)
  @SuppressWarnings("CheckReturnValue")
  public void testGetArrays4invalid() throws SolverException, InterruptedException {
    requireParser();
    requireArrays();

    // create formula for "arr[5]==x && x==123"
    BooleanFormula f =
        mgr.parse(
            "(declare-fun x () Int)\n"
                + "(declare-fun arr () (Array Int Int))\n"
                + "(assert (and"
                + "    (= (select arr 5) x)"
                + "    (= x 123)"
                + "))");

    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(f);
      assertThat(prover).isSatisfiable();

      try (Model m = prover.getModel()) {
        m.evaluate(amgr.makeArray("arr", ARRAY_TYPE_INT_INT));
      }
    }
  }

  @Test
  public void testGetArrays5() throws SolverException, InterruptedException {
    requireParser();
    requireArrays();

    // create formula for "arr[5:6]==[x,x] && x==123"
    BooleanFormula f =
        mgr.parse(
            "(declare-fun x () Int)\n"
                + "(declare-fun arr () (Array Int Int))\n"
                + "(assert (and"
                + "    (= (select (store arr 6 x) 5) x)"
                + "    (= x 123)"
                + "))");

    testModelIterator(f);
    testModelGetters(f, imgr.makeVariable("x"), BigInteger.valueOf(123), "x");
    testModelGetters(
        f,
        amgr.select(amgr.makeArray("arr", ARRAY_TYPE_INT_INT), imgr.makeNumber(5)),
        BigInteger.valueOf(123),
        "arr",
        true);
  }

  @Test
  public void testGetArrays5b() throws SolverException, InterruptedException {
    requireParser();
    requireArrays();

    // create formula for "arr[5]==x && arr[6]==x && x==123"
    BooleanFormula f =
        mgr.parse(
            "(declare-fun x () Int)\n"
                + "(declare-fun arrgh () (Array Int Int))\n"
                + "(declare-fun ahoi () (Array Int Int))\n"
                + "(assert (and"
                + "    (= (select arrgh 5) x)"
                + "    (= (select arrgh 6) x)"
                + "    (= x 123)"
                + "    (= (select (store ahoi 66 x) 55) x)"
                + "))");

    testModelIterator(f);
    testModelGetters(f, imgr.makeVariable("x"), BigInteger.valueOf(123), "x");
    testModelGetters(
        f,
        amgr.select(amgr.makeArray("arrgh", ARRAY_TYPE_INT_INT), imgr.makeNumber(5)),
        BigInteger.valueOf(123),
        "arrgh",
        true);
    testModelGetters(
        f,
        amgr.select(amgr.makeArray("arrgh", ARRAY_TYPE_INT_INT), imgr.makeNumber(6)),
        BigInteger.valueOf(123),
        "arrgh",
        true);
    testModelGetters(
        f,
        amgr.select(amgr.makeArray("ahoi", ARRAY_TYPE_INT_INT), imgr.makeNumber(55)),
        BigInteger.valueOf(123),
        "ahoi",
        true);

    // The value for 'ahoi[66]' is not determined by the constraints from above,
    // because we only 'store' it in (a copy of) the array, but never read it.
    // Thus, the following test case depends on the solver and would be potentially wrong:
    //
    // testModelGetters(
    // f,
    // amgr.select(amgr.makeArray("ahoi", ARRAY_TYPE_INT_INT), imgr.makeNumber(66)),
    // BigInteger.valueOf(123),
    // "ahoi",
    // true);
  }

  @Test
  public void testGetArrays5c() throws SolverException, InterruptedException {
    requireParser();
    requireArrays();

    // create formula for "arr[5:6]==[x,x] && x==123"
    BooleanFormula f =
        mgr.parse(
            "(declare-fun x () Int)\n"
                + "(declare-fun arrgh () (Array Int Int))\n"
                + "(declare-fun ahoi () (Array Int Int))\n"
                + "(assert (and"
                + "    (= (select (store arrgh 6 x) 5) x)"
                + "    (= (select (store ahoi 6 x) 5) x)"
                + "    (= x 123)"
                + "))");

    testModelIterator(f);
    testModelGetters(f, imgr.makeVariable("x"), BigInteger.valueOf(123), "x");
    testModelGetters(
        f,
        amgr.select(amgr.makeArray("arrgh", ARRAY_TYPE_INT_INT), imgr.makeNumber(5)),
        BigInteger.valueOf(123),
        "arrgh",
        true);
    testModelGetters(
        f,
        amgr.select(amgr.makeArray("ahoi", ARRAY_TYPE_INT_INT), imgr.makeNumber(5)),
        BigInteger.valueOf(123),
        "ahoi",
        true);
  }

  @Test
  public void testGetArrays5d() throws SolverException, InterruptedException {
    requireParser();
    requireArrays();

    // create formula for "arr[5:6]==[x,x] && x==123"
    BooleanFormula f =
        mgr.parse(
            "(declare-fun x () Int)\n"
                + "(declare-fun arrgh () (Array Int Int))\n"
                + "(declare-fun ahoi () (Array Int Int))\n"
                + "(assert (and"
                + "    (= (select (store arrgh 6 x) 5) x)"
                + "    (= (select (store ahoi 6 x) 7) x)"
                + "    (= x 123)"
                + "))");

    testModelIterator(f);
    testModelGetters(f, imgr.makeVariable("x"), BigInteger.valueOf(123), "x");
    testModelGetters(
        f,
        amgr.select(amgr.makeArray("arrgh", ARRAY_TYPE_INT_INT), imgr.makeNumber(5)),
        BigInteger.valueOf(123),
        "arrgh",
        true);
    testModelGetters(
        f,
        amgr.select(amgr.makeArray("ahoi", ARRAY_TYPE_INT_INT), imgr.makeNumber(7)),
        BigInteger.valueOf(123),
        "ahoi",
        true);
  }

  @Test
  public void testGetArrays5e() throws SolverException, InterruptedException {
    requireParser();
    requireArrays();

    // create formula for "arrgh[5:6]==[x,x] && ahoi[5,7] == [x,x] && x==123"
    BooleanFormula f =
        mgr.parse(
            "(declare-fun x () Int)\n"
                + "(declare-fun arrgh () (Array Int Int))\n"
                + "(declare-fun ahoi () (Array Int Int))\n"
                + "(assert (and"
                + "    (= (select (store arrgh 6 x) 5) x)"
                + "    (= (select (store ahoi 7 x) 5) x)"
                + "    (= x 123)"
                + "))");

    testModelIterator(f);
    testModelGetters(f, imgr.makeVariable("x"), BigInteger.valueOf(123), "x");
    testModelGetters(
        f,
        amgr.select(amgr.makeArray("arrgh", ARRAY_TYPE_INT_INT), imgr.makeNumber(5)),
        BigInteger.valueOf(123),
        "arrgh",
        true);
    testModelGetters(
        f,
        amgr.select(amgr.makeArray("ahoi", ARRAY_TYPE_INT_INT), imgr.makeNumber(5)),
        BigInteger.valueOf(123),
        "ahoi",
        true);
  }

  @Test
  public void testGetArrays5f() throws SolverException, InterruptedException {
    requireParser();
    requireArrays();

    // create formula for "arrgh[5:6]==[x,x] && ahoi[5,6] == [x,y] && y = 125 && x==123"
    BooleanFormula f =
        mgr.parse(
            "(declare-fun x () Int)\n"
                + "(declare-fun y () Int)\n"
                + "(declare-fun arrgh () (Array Int Int))\n"
                + "(declare-fun ahoi () (Array Int Int))\n"
                + "(assert (and"
                + "    (= (select (store arrgh 6 x) 5) x)"
                + "    (= (select (store ahoi 6 y) 5) x)"
                + "    (= x 123)"
                + "    (= y 125)"
                + "))");

    testModelIterator(f);
    testModelGetters(f, imgr.makeVariable("x"), BigInteger.valueOf(123), "x");
    testModelGetters(
        f,
        amgr.select(amgr.makeArray("arrgh", ARRAY_TYPE_INT_INT), imgr.makeNumber(5)),
        BigInteger.valueOf(123),
        "arrgh",
        true);
    testModelGetters(
        f,
        amgr.select(amgr.makeArray("ahoi", ARRAY_TYPE_INT_INT), imgr.makeNumber(5)),
        BigInteger.valueOf(123),
        "ahoi",
        true);
  }

  @Test
  public void testGetArrays7() throws SolverException, InterruptedException {
    requireArrays();
    // Boolector only supports Bitvectors (bv arrays and ufs)
    assume().that(solverToUse()).isNotEqualTo(Solvers.BOOLECTOR);
    ArrayFormula<IntegerFormula, IntegerFormula> array1 =
        amgr.makeArray("array", IntegerType, IntegerType);

    IntegerFormula selected = amgr.select(array1, imgr.makeNumber(1));
    BooleanFormula selectEq0 = imgr.equal(selected, imgr.makeNumber(0));
    // Note that store is not an assignment! This is just so that the implication fails and arr[1] =
    // 0
    BooleanFormula selectStore =
        imgr.equal(
            amgr.select(
                amgr.store(array1, imgr.makeNumber(1), imgr.makeNumber(7)), imgr.makeNumber(1)),
            imgr.makeNumber(0));

    BooleanFormula assert1 = bmgr.implication(bmgr.not(selectEq0), selectStore);

    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(assert1);

      assertThat(prover).isSatisfiable();

      try (Model m = prover.getModel()) {
        for (@SuppressWarnings("unused") ValueAssignment assignment : m) {
          // Check that we can iterate through with no crashes.
        }
        assertThat(m.evaluate(selected)).isEqualTo(BigInteger.ZERO);
      }
    }
  }

  @Test
  public void testGetArrays8() throws SolverException, InterruptedException {
    requireArrays();
    // Boolector only supports Bitvectors (bv arrays and ufs)
    assume().that(solverToUse()).isNotEqualTo(Solvers.BOOLECTOR);
    ArrayFormula<IntegerFormula, IntegerFormula> array1 =
        amgr.makeArray("array", IntegerType, IntegerType);

    IntegerFormula selected = amgr.select(array1, imgr.makeNumber(1));
    BooleanFormula selectEq0 = imgr.equal(selected, imgr.makeNumber(0));
    IntegerFormula selectStore =
        amgr.select(amgr.store(array1, imgr.makeNumber(1), imgr.makeNumber(7)), imgr.makeNumber(1));
    // Note that store is not an assignment! This is just used to make the implication fail and
    // arr[1] =
    // 0
    BooleanFormula selectStoreEq0 = imgr.equal(selectStore, imgr.makeNumber(0));

    IntegerFormula arithEq7 =
        imgr.subtract(
            imgr.multiply(imgr.add(imgr.makeNumber(1), imgr.makeNumber(2)), imgr.makeNumber(3)),
            imgr.makeNumber(2));
    BooleanFormula selectStoreEq7 = imgr.equal(selectStore, arithEq7);

    // arr[1] = 0 -> (arr[1] = 7)[1] = 0
    // if the left is true, the right has to be, but its false => left false => overall TRUE
    BooleanFormula assert1 = bmgr.implication(selectEq0, selectStoreEq0);
    // arr[1] != 0 -> (arr[1] = 7)[1] = 7
    // left has to be true because of assert1 -> right has to be true as well
    BooleanFormula assert2 = bmgr.implication(bmgr.not(selectEq0), selectStoreEq7);

    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(bmgr.and(assert1, assert2));

      assertThat(prover).isSatisfiable();

      try (Model m = prover.getModel()) {
        for (@SuppressWarnings("unused") ValueAssignment assignment : m) {
          // Check that we can iterate through with no crashes.
        }
        assertThat(m.evaluate(selectStore)).isEqualTo(BigInteger.valueOf(7));
        assertThat(m.evaluate(arithEq7)).isEqualTo(BigInteger.valueOf(7));
        assertThat(m.evaluate(selected)).isNotEqualTo(BigInteger.valueOf(0));
      }
    }
  }

  @Test
  public void testGetArrays9() throws SolverException, InterruptedException {
    requireArrays();
    // Boolector only supports Bitvectors (bv arrays and ufs)
    assume().that(solverToUse()).isNotEqualTo(Solvers.BOOLECTOR);
    ArrayFormula<IntegerFormula, IntegerFormula> array1 =
        amgr.makeArray("array1", IntegerType, IntegerType);

    ArrayFormula<IntegerFormula, IntegerFormula> array2 =
        amgr.makeArray("array2", IntegerType, IntegerType);

    IntegerFormula selected1 = amgr.select(array1, imgr.makeNumber(1));
    BooleanFormula selectEq0 = imgr.equal(selected1, imgr.makeNumber(0));
    BooleanFormula selectGT0 = imgr.greaterThan(selected1, imgr.makeNumber(0));
    BooleanFormula selectGTEmin1 = imgr.greaterOrEquals(selected1, imgr.makeNumber(-1));

    IntegerFormula selected2 = amgr.select(array2, imgr.makeNumber(1));
    BooleanFormula arr2LT0 = imgr.lessOrEquals(selected2, imgr.makeNumber(0));
    BooleanFormula select2GTEmin1 = imgr.greaterOrEquals(selected2, imgr.makeNumber(-1));

    // arr1[1] > 0 -> arr1[1] = 0
    // obviously false => arr[1] <= 0
    BooleanFormula assert1 = bmgr.implication(selectGT0, selectEq0);
    // arr1[1] > 0 -> arr2[1] <= 1
    // left holds because of the first assertion => arr2[1] <= 0
    BooleanFormula assert2 = bmgr.implication(bmgr.not(selectGT0), arr2LT0);
    // if now arr2[1] >= -1 -> arr1[1] >= -1
    // holds
    BooleanFormula assert3 = bmgr.implication(select2GTEmin1, selectGTEmin1);
    BooleanFormula assert4 = imgr.greaterThan(selected2, imgr.makeNumber(-2));
    // basicly just says that: -1 <= arr[1] <= 0 & -1 <= arr2[1] <= 0 up to this point
    // make the 2 array[1] values unequal
    BooleanFormula assert5 = bmgr.not(imgr.equal(selected1, selected2));

    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(bmgr.and(assert1, assert2, assert3, assert4, assert5));

      assertThat(prover).isSatisfiable();

      try (Model m = prover.getModel()) {
        for (@SuppressWarnings("unused") ValueAssignment assignment : m) {
          // Check that we can iterate through with no crashes.
        }
        if (m.evaluate(selected1).equals(BigInteger.valueOf(-1))) {
          assertThat(m.evaluate(selected1)).isEqualTo(BigInteger.valueOf(-1));
          assertThat(m.evaluate(selected2)).isEqualTo(BigInteger.valueOf(0));
        } else {
          assertThat(m.evaluate(selected1)).isEqualTo(BigInteger.valueOf(0));
          assertThat(m.evaluate(selected2)).isEqualTo(BigInteger.valueOf(-1));
        }
      }
    }
  }

  private void testModelIterator(BooleanFormula f) throws SolverException, InterruptedException {
    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(f);

      assertThat(prover).isSatisfiable();

      try (Model m = prover.getModel()) {
        for (@SuppressWarnings("unused") ValueAssignment assignment : m) {
          // Check that we can iterate through with no crashes.
        }
        assertThat(prover.getModelAssignments()).containsExactlyElementsIn(m).inOrder();
      }
    }
  }

  private void testModelGetters(
      BooleanFormula constraint, Formula variable, Object expectedValue, String varName)
      throws SolverException, InterruptedException {
    testModelGetters(constraint, variable, expectedValue, varName, false);
  }

  private void testModelGetters(
      BooleanFormula constraint,
      Formula variable,
      Object expectedValue,
      String varName,
      boolean isArray)
      throws SolverException, InterruptedException {

    List<BooleanFormula> modelAssignments = new ArrayList<>();

    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(constraint);
      assertThat(prover).isSatisfiable();

      try (Model m = prover.getModel()) {
        assertThat(m.evaluate(variable)).isEqualTo(expectedValue);

        for (ValueAssignment va : m) {
          modelAssignments.add(va.getAssignmentAsFormula());
        }

        List<ValueAssignment> relevantAssignments =
            prover.getModelAssignments().stream()
                .filter(assignment -> assignment.getName().equals(varName))
                .collect(Collectors.toList());
        assertThat(relevantAssignments).isNotEmpty();

        if (isArray) {
          List<ValueAssignment> arrayAssignments =
              relevantAssignments.stream()
                  .filter(assignment -> expectedValue.equals(assignment.getValue()))
                  .collect(Collectors.toList());
          assertThat(arrayAssignments)
              .isNotEmpty(); // at least one assignment should have the wanted value

        } else {
          // normal variables or UFs have exactly one evaluation assigned to their name
          assertThat(relevantAssignments).hasSize(1);
          ValueAssignment assignment = Iterables.getOnlyElement(relevantAssignments);
          assertThat(assignment.getValue()).isEqualTo(expectedValue);
          assertThat(m.evaluate(assignment.getKey())).isEqualTo(expectedValue);
        }
      }
    }
    assertThatFormula(bmgr.and(modelAssignments)).implies(constraint);
  }

  @Test
  public void ufTest() throws SolverException, InterruptedException {
    requireQuantifiers();
    requireBitvectors();
    // only Z3 fulfills these requirements

    assume()
        .withMessage("solver does not implement optimisation")
        .that(solverToUse())
        .isEqualTo(Solvers.Z3);

    BitvectorType t32 = FormulaType.getBitvectorTypeWithSize(32);
    FunctionDeclaration<BitvectorFormula> si1 = fmgr.declareUF("*signed_int@1", t32, t32);
    FunctionDeclaration<BitvectorFormula> si2 = fmgr.declareUF("*signed_int@2", t32, t32);
    BitvectorFormula ctr = bvmgr.makeVariable(t32, "*signed_int@1@counter");
    BitvectorFormula adr = bvmgr.makeVariable(t32, "__ADDRESS_OF_test");
    BitvectorFormula num0 = bvmgr.makeBitvector(32, 0);
    BitvectorFormula num4 = bvmgr.makeBitvector(32, 4);
    BitvectorFormula num10 = bvmgr.makeBitvector(32, 10);

    BooleanFormula a11 =
        bmgr.implication(
            bmgr.and(
                bvmgr.lessOrEquals(adr, ctr, false),
                bvmgr.lessThan(ctr, bvmgr.add(adr, num10), false)),
            bvmgr.equal(fmgr.callUF(si2, ctr), num0));
    BooleanFormula a21 =
        bmgr.not(
            bmgr.and(
                bvmgr.lessOrEquals(adr, ctr, false),
                bvmgr.lessThan(ctr, bvmgr.add(adr, num10), false)));
    BooleanFormula body =
        bmgr.and(
            a11, bmgr.implication(a21, bvmgr.equal(fmgr.callUF(si2, ctr), fmgr.callUF(si1, ctr))));
    BooleanFormula a1 = qmgr.forall(ctr, body);
    BooleanFormula a2 =
        bvmgr.equal(fmgr.callUF(si1, bvmgr.add(adr, bvmgr.multiply(num4, num0))), num0);

    BooleanFormula f = bmgr.and(a1, bvmgr.lessThan(num0, adr, true), bmgr.not(a2));

    checkModelIteration(f, true);
    checkModelIteration(f, false);
  }

  @SuppressWarnings("resource")
  private void checkModelIteration(BooleanFormula f, boolean useOptProver)
      throws SolverException, InterruptedException {
    ImmutableList<ValueAssignment> assignments;
    try (BasicProverEnvironment<?> prover =
        useOptProver
            ? context.newOptimizationProverEnvironment(ProverOptions.GENERATE_MODELS)
            : context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(f);
      assertThat(prover.isUnsat()).isFalse();
      try (Model m = prover.getModel()) {
        for (@SuppressWarnings("unused") ValueAssignment assignment : m) {
          // Check that we can iterate through with no crashes.
        }
        assertThat(prover.getModelAssignments()).containsExactlyElementsIn(m).inOrder();

        assignments = prover.getModelAssignments();
      }
    }

    assertThat(assignments.size())
        .isEqualTo(assignments.stream().map(ValueAssignment::getKey).distinct().count());

    List<BooleanFormula> assignmentFormulas = new ArrayList<>();
    for (ValueAssignment va : assignments) {
      assignmentFormulas.add(va.getAssignmentAsFormula());
      assertThatFormula(va.getAssignmentAsFormula())
          .isEqualTo(makeAssignment(va.getKey(), va.getValueAsFormula()));
      assertThat(va.getValue().getClass())
          .isIn(ImmutableList.of(Boolean.class, BigInteger.class, Rational.class, Double.class));
    }

    // Check that model is not contradicting
    assertThatFormula(bmgr.and(assignmentFormulas)).isSatisfiable();

    // Check that model does not contradict formula.
    // Check for implication is not possible, because formula "x=y" does not imply "{x=0,y=0}" and
    // formula "A = (store EMPTY x y)" is not implied by "{x=0,y=0,(select A 0)=0}" (EMPTY != A).
    assertThatFormula(bmgr.and(f, bmgr.and(assignmentFormulas))).isSatisfiable();
  }

  /**
   * Short-cut in cases where the type of the formula is unknown. Delegates to the corresponding
   * [boolean, integer, bitvector, ...] formula manager.
   */
  @SuppressWarnings("unchecked")
  private BooleanFormula makeAssignment(Formula pFormula1, Formula pFormula2) {
    FormulaType<?> pType = mgr.getFormulaType(pFormula1);
    assertWithMessage(
            "Trying to equalize two formulas %s and %s of different types %s and %s",
            pFormula1, pFormula2, pType, mgr.getFormulaType(pFormula2))
        .that(mgr.getFormulaType(pFormula1).equals(mgr.getFormulaType(pFormula2)))
        .isTrue();
    if (pType.isBooleanType()) {
      return bmgr.equivalence((BooleanFormula) pFormula1, (BooleanFormula) pFormula2);
    } else if (pType.isIntegerType()) {
      return imgr.equal((IntegerFormula) pFormula1, (IntegerFormula) pFormula2);
    } else if (pType.isRationalType()) {
      return rmgr.equal((RationalFormula) pFormula1, (RationalFormula) pFormula2);
    } else if (pType.isBitvectorType()) {
      return bvmgr.equal((BitvectorFormula) pFormula1, (BitvectorFormula) pFormula2);
    } else if (pType.isFloatingPointType()) {
      return fpmgr.assignment((FloatingPointFormula) pFormula1, (FloatingPointFormula) pFormula2);
    } else if (pType.isArrayType()) {
      @SuppressWarnings("rawtypes")
      ArrayFormula f2 = (ArrayFormula) pFormula2;
      return amgr.equivalence((ArrayFormula<?, ?>) pFormula1, f2);
    }
    throw new IllegalArgumentException(
        "Cannot make equality of formulas with type " + pType + " in the Solver!");
  }

  @Test
  public void quantifierTestShort() throws SolverException, InterruptedException {
    requireQuantifiers();
    requireIntegers();

    IntegerFormula ctr = imgr.makeVariable("x");
    BooleanFormula body = imgr.equal(ctr, imgr.makeNumber(0));

    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {

      // exists x : x==0
      prover.push(qmgr.exists(ctr, body));
      assertThat(prover.isUnsat()).isFalse();
      try (Model m = prover.getModel()) {
        for (ValueAssignment v : m) {
          // a value-assignment might have a different name, but the value should be "0".
          assertThat(BigInteger.ZERO.equals(v.getValue())).isTrue();
        }
      }
      prover.pop();

      // x==0
      prover.push(body);
      assertThat(prover.isUnsat()).isFalse();
      try (Model m = prover.getModel()) {
        ValueAssignment v = m.iterator().next();
        assertThat("x".equals(v.getName())).isTrue();
        assertThat(BigInteger.ZERO.equals(v.getValue())).isTrue();
      }
    }
  }

  static final String SMALL_ARRAY_QUERY =
      "(declare-fun A1 () (Array Int Int))"
          + "(declare-fun A2 () (Array Int Int))"
          + "(declare-fun X () Int)"
          + "(declare-fun Y () Int)"
          + "(assert (= A1 (store A2 X Y)))";

  static final String BIG_ARRAY_QUERY =
      "(declare-fun |V#2@| () Int)"
          + "(declare-fun z3name!115 () Int)"
          + "(declare-fun P42 () Bool)"
          + "(declare-fun M@3 () Int)"
          + "(declare-fun P43 () Bool)"
          + "(declare-fun |V#1@| () Int)"
          + "(declare-fun z3name!114 () Int)"
          + "(declare-fun P44 () Bool)"
          + "(declare-fun M@2 () Int)"
          + "(declare-fun P45 () Bool)"
          + "(declare-fun |It15@5| () Int)"
          + "(declare-fun |It13@5| () Int)"
          + "(declare-fun |H@12| () (Array Int Int))"
          + "(declare-fun |H@13| () (Array Int Int))"
          + "(declare-fun |It14@5| () Int)"
          + "(declare-fun |IN@5| () Int)"
          + "(declare-fun |It12@5| () Int)"
          + "(declare-fun |It11@5| () Int)"
          + "(declare-fun |It9@5| () Int)"
          + "(declare-fun |H@11| () (Array Int Int))"
          + "(declare-fun |It10@5| () Int)"
          + "(declare-fun |It8@5| () Int)"
          + "(declare-fun |Anew@3| () Int)"
          + "(declare-fun |Aprev@3| () Int)"
          + "(declare-fun |H@10| () (Array Int Int))"
          + "(declare-fun |At7@5| () Int)"
          + "(declare-fun |H@9| () (Array Int Int))"
          + "(declare-fun |At6@5| () Int)"
          + "(declare-fun |Anext@3| () Int)"
          + "(declare-fun |H@8| () (Array Int Int))"
          + "(declare-fun |At5@5| () Int)"
          + "(declare-fun |H@7| () (Array Int Int))"
          + "(declare-fun |At4@5| () Int)"
          + "(declare-fun |at3@5| () Int)"
          + "(declare-fun |ahead@3| () Int)"
          + "(declare-fun |anew@3| () Int)"
          + "(declare-fun gl@ () Int)"
          + "(declare-fun |It7@5| () Int)"
          + "(declare-fun |It6@5| () Int)"
          + "(declare-fun |It5@5| () Int)"
          + "(declare-fun |Ivalue@3| () Int)"
          + "(declare-fun i@2 () (Array Int Int))"
          + "(declare-fun i@3 () (Array Int Int))"
          + "(declare-fun P46 () Bool)"
          + "(declare-fun |It4@5| () Int)"
          + "(declare-fun |It4@3| () Int)"
          + "(declare-fun |IT@5| () Int)"
          + "(declare-fun |gl_read::T@4| () Int)"
          + "(declare-fun __VERIFIER_nondet_int@4 () Int)"
          + "(declare-fun |It15@3| () Int)"
          + "(declare-fun |It13@3| () Int)"
          + "(declare-fun |H@6| () (Array Int Int))"
          + "(declare-fun |It14@3| () Int)"
          + "(declare-fun |IN@3| () Int)"
          + "(declare-fun |It12@3| () Int)"
          + "(declare-fun |It11@3| () Int)"
          + "(declare-fun |It9@3| () Int)"
          + "(declare-fun |H@5| () (Array Int Int))"
          + "(declare-fun |It10@3| () Int)"
          + "(declare-fun |It8@3| () Int)"
          + "(declare-fun |Anew@2| () Int)"
          + "(declare-fun |Aprev@2| () Int)"
          + "(declare-fun |H@4| () (Array Int Int))"
          + "(declare-fun |At7@3| () Int)"
          + "(declare-fun |H@3| () (Array Int Int))"
          + "(declare-fun |At6@3| () Int)"
          + "(declare-fun |Anext@2| () Int)"
          + "(declare-fun |H@2| () (Array Int Int))"
          + "(declare-fun |At5@3| () Int)"
          + "(declare-fun |H@1| () (Array Int Int))"
          + "(declare-fun |At4@3| () Int)"
          + "(declare-fun |at3@3| () Int)"
          + "(declare-fun |ahead@2| () Int)"
          + "(declare-fun |anew@2| () Int)"
          + "(declare-fun |It7@3| () Int)"
          + "(declare-fun |It6@3| () Int)"
          + "(declare-fun |It5@3| () Int)"
          + "(declare-fun |Ivalue@2| () Int)"
          + "(declare-fun i@1 () (Array Int Int))"
          + "(declare-fun P47 () Bool)"
          + "(declare-fun |IT@3| () Int)"
          + "(declare-fun |gl_read::T@3| () Int)"
          + "(declare-fun __VERIFIER_nondet_int@2 () Int)"
          + "(assert "
          + "  (and (not (<= gl@ 0))"
          + "       (not (<= gl@ (- 64)))"
          + "       (= |gl_read::T@3| __VERIFIER_nondet_int@2)"
          + "       (= |Ivalue@2| |gl_read::T@3|)"
          + "       (= |It4@3| 20)"
          + "       (= |IT@3| z3name!114)"
          + "       (not (<= |V#1@| 0))"
          + "       (= |IN@3| |IT@3|)"
          + "       (not (<= |V#1@| (+ 64 gl@)))"
          + "       (not (<= |V#1@| (- 160)))"
          + "       (not (<= (+ |V#1@| (* 8 |It4@3|)) 0))"
          + "       (or (and P47 (not (<= |IN@3| 0))) (and (not P47) (not (>= |IN@3| 0))))"
          + "       (= i@2 (store i@1 |IN@3| |Ivalue@2|))"
          + "       (= |It5@3| |IN@3|)"
          + "       (= |It6@3| (+ 4 |It5@3|))"
          + "       (= |It7@3| |It6@3|)"
          + "       (= |anew@2| |It7@3|)"
          + "       (= |ahead@2| gl@)"
          + "       (= |at3@3| (select |H@1| |ahead@2|))"
          + "       (= |Anew@2| |anew@2|)"
          + "       (= |Aprev@2| |ahead@2|)"
          + "       (= |Anext@2| |at3@3|)"
          + "       (= |At4@3| |Anext@2|)"
          + "       (= |At5@3| (+ 4 |At4@3|))"
          + "       (= |H@2| (store |H@1| |At5@3| |Anew@2|))"
          + "       (= |H@3| (store |H@2| |Anew@2| |Anext@2|))"
          + "       (= |At6@3| |Anew@2|)"
          + "       (= |At7@3| (+ 4 |At6@3|))"
          + "       (= |H@4| (store |H@3| |At7@3| |Aprev@2|))"
          + "       (= |H@5| (store |H@4| |Aprev@2| |Anew@2|))"
          + "       (= |It8@3| |IN@3|)"
          + "       (= |It9@3| (+ 12 |It8@3|))"
          + "       (= |It10@3| |IN@3|)"
          + "       (= |It11@3| (+ 12 |It10@3|))"
          + "       (= |H@6| (store |H@5| |It9@3| |It11@3|))"
          + "       (= |It12@3| |IN@3|)"
          + "       (= |It13@3| (+ 12 |It12@3|))"
          + "       (= |It14@3| |IN@3|)"
          + "       (= |It15@3| (+ 12 |It14@3|))"
          + "       (= |H@7| (store |H@6| |It13@3| |It15@3|))"
          + "       (= |gl_read::T@4| __VERIFIER_nondet_int@4)"
          + "       (= |Ivalue@3| |gl_read::T@4|)"
          + "       (= |It4@5| 20)"
          + "       (= |IT@5| z3name!115)"
          + "       (not (<= |V#2@| 0))"
          + "       (= |IN@5| |IT@5|)"
          + "       (not (<= |V#2@| (+ |V#1@| (* 8 |It4@3|))))"
          + "       (not (<= |V#2@| (+ 160 |V#1@|)))"
          + "       (not (<= |V#2@| (- 160)))"
          + "       (not (<= (+ |V#2@| (* 8 |It4@5|)) 0))"
          + "       (or (and P46 (not (<= |IN@5| 0))) (and (not P46) (not (>= |IN@5| 0))))"
          + "       (= i@3 (store i@2 |IN@5| |Ivalue@3|))"
          + "       (= |It5@5| |IN@5|)"
          + "       (= |It6@5| (+ 4 |It5@5|))"
          + "       (= |It7@5| |It6@5|)"
          + "       (= |anew@3| |It7@5|)"
          + "       (= |ahead@3| gl@)"
          + "       (= |at3@5| (select |H@7| |ahead@3|))"
          + "       (= |Anew@3| |anew@3|)"
          + "       (= |Aprev@3| |ahead@3|)"
          + "       (= |Anext@3| |at3@5|)"
          + "       (= |At4@5| |Anext@3|)"
          + "       (= |At5@5| (+ 4 |At4@5|))"
          + "       (= |H@8| (store |H@7| |At5@5| |Anew@3|))"
          + "       (= |H@9| (store |H@8| |Anew@3| |Anext@3|))"
          + "       (= |At6@5| |Anew@3|)"
          + "       (= |At7@5| (+ 4 |At6@5|))"
          + "       (= |H@10| (store |H@9| |At7@5| |Aprev@3|))"
          + "       (= |H@11| (store |H@10| |Aprev@3| |Anew@3|))"
          + "       (= |It8@5| |IN@5|)"
          + "       (= |It9@5| (+ 12 |It8@5|))"
          + "       (= |It10@5| |IN@5|)"
          + "       (= |It11@5| (+ 12 |It10@5|))"
          + "       (= |H@12| (store |H@11| |It9@5| |It11@5|))"
          + "       (= |It12@5| |IN@5|)"
          + "       (= |It13@5| (+ 12 |It12@5|))"
          + "       (= |It14@5| |IN@5|)"
          + "       (= |It15@5| (+ 12 |It14@5|))"
          + "       (= |H@13| (store |H@12| |It13@5| |It15@5|))"
          + "       (or (and P45 (not (= M@2 0)))  (and (not P45) (= z3name!114 0)))"
          + "       (or (and P44 (= M@2 0)) (and (not P44) (= z3name!114 |V#1@|)))"
          + "       (or (and P43 (not (= M@3 0))) (and (not P43) (= z3name!115 0)))"
          + "       (or (and P42 (= M@3 0)) (and (not P42) (= z3name!115 |V#2@|)))))";

  static final String MEDIUM_ARRAY_QUERY =
      "(declare-fun |H@1| () (Array Int Int))"
          + "(declare-fun |H@2| () (Array Int Int))"
          + "(declare-fun |H@3| () (Array Int Int))"
          + "(declare-fun |H@4| () (Array Int Int))"
          + "(declare-fun |H@5| () (Array Int Int))"
          + "(declare-fun |H@6| () (Array Int Int))"
          + "(declare-fun |H@7| () (Array Int Int))"
          + "(declare-fun |H@8| () (Array Int Int))"
          + "(declare-fun |H@9| () (Array Int Int))"
          + "(declare-fun |H@10| () (Array Int Int))"
          + "(declare-fun |H@11| () (Array Int Int))"
          + "(declare-fun |H@12| () (Array Int Int))"
          + "(declare-fun |H@13| () (Array Int Int))"
          + "(declare-fun I10 () Int)"
          + "(declare-fun I11 () Int)"
          + "(declare-fun I12 () Int)"
          + "(declare-fun I13 () Int)"
          + "(declare-fun I14 () Int)"
          + "(declare-fun I15 () Int)"
          + "(declare-fun |at3@5| () Int)"
          + "(declare-fun |at3@3| () Int)"
          + "(declare-fun |At5@3| () Int)"
          + "(declare-fun |At7@3| () Int)"
          + "(declare-fun |At7@5| () Int)"
          + "(declare-fun |ahead@3| () Int)"
          + "(declare-fun |ahead@2| () Int)"
          + "(declare-fun |At5@5| () Int)"
          + "(assert "
          + "  (and (not (<= |ahead@2| 0))"
          + "       (= |H@2| (store |H@1| |At5@3| 1))"
          + "       (= |H@3| (store |H@2| 3 1))"
          + "       (= |H@4| (store |H@3| 4 1))"
          + "       (= |H@5| (store |H@4| 5 1))"
          + "       (= |H@6| (store |H@5| 6 1))"
          + "       (= |H@7| (store |H@6| 7 1))"
          + "       (= |H@8| (store |H@7| 8 1))"
          + "       (= |at3@3| (select |H@1| |ahead@2|))"
          + "       (= |at3@5| (select |H@7| |ahead@3|))"
          + "       (= I11 (+ 12 I10))"
          + "       (= I13 (+ 12 I12))"
          + "       (= I15 (+ 12 I14))"
          + "       ))";

  static final String UGLY_ARRAY_QUERY =
      "(declare-fun V () Int)"
          + "(declare-fun W () Int)"
          + "(declare-fun A () Int)"
          + "(declare-fun B () Int)"
          + "(declare-fun U () Int)"
          + "(declare-fun G () Int)"
          + "(declare-fun ARR () (Array Int Int))"
          + "(declare-fun EMPTY () (Array Int Int))"
          + "(assert "
          + "  (and (> (+ V U) 0)"
          + "       (not (= B (- 4)))"
          + "       (= ARR (store (store (store EMPTY G B) B G) A W))"
          + "       ))";

  static final String UGLY_ARRAY_QUERY_2 =
      "(declare-fun A () Int)"
          + "(declare-fun B () Int)"
          + "(declare-fun ARR () (Array Int Int))"
          + "(declare-fun EMPTY () (Array Int Int))"
          + "(assert (and (= A 0) (= B 0) (= ARR (store (store EMPTY A 1) B 2))))";

  static final String SMALL_BV_FLOAT_QUERY =
      "(declare-fun |f@2| () (_ FloatingPoint 8 23))"
          + "(declare-fun |p@3| () (_ BitVec 32))"
          + "(declare-fun *float@1 () (Array (_ BitVec 32) (_ FloatingPoint 8 23)))"
          + "(declare-fun |i@33| () (_ BitVec 32))"
          + "(declare-fun |Ai@| () (_ BitVec 32))"
          + "(declare-fun *unsigned_int@1 () (Array (_ BitVec 32) (_ BitVec 32)))"
          + "(assert (and (bvslt #x00000000 |Ai@|)"
          + "     (bvslt #x00000000 (bvadd |Ai@| #x00000020))"
          + "     (= |i@33| #x00000000)"
          + "     (= |p@3| |Ai@|)"
          + "     (= (select *unsigned_int@1 |Ai@|) |i@33|)"
          + "     (= |f@2| (select *float@1 |p@3|))"
          + "     (not (fp.eq ((_ to_fp 11 52) roundNearestTiesToEven |f@2|)"
          + "                 (_ +zero 11 52)))))";

  static final String SMALL_BV_FLOAT_QUERY2 =
      "(declare-fun a () (_ FloatingPoint 8 23))"
          + "(declare-fun A () (Array (_ BitVec 32) (_ FloatingPoint 8 23)))"
          + "(assert (= a (select A #x00000000)))";

  @Test
  public void arrayTest1() throws SolverException, InterruptedException {
    requireParser();
    requireArrays();

    for (String query :
        ImmutableList.of(
            SMALL_ARRAY_QUERY, MEDIUM_ARRAY_QUERY, UGLY_ARRAY_QUERY, UGLY_ARRAY_QUERY_2)) {
      BooleanFormula formula = context.getFormulaManager().parse(query);
      checkModelIteration(formula, false);
    }
  }

  @Test
  public void arrayTest2() throws SolverException, InterruptedException {
    requireParser();
    requireArrays();
    requireOptimization();
    requireFloats();
    requireBitvectors();
    // only Z3 fulfills these requirements

    for (String query :
        ImmutableList.of(BIG_ARRAY_QUERY, SMALL_BV_FLOAT_QUERY, SMALL_BV_FLOAT_QUERY2)) {
      BooleanFormula formula = context.getFormulaManager().parse(query);
      checkModelIteration(formula, true);
      checkModelIteration(formula, false);
    }
  }

  static final String ARRAY_QUERY_INT =
      "(declare-fun i () Int)"
          + "(declare-fun X () (Array Int Int))"
          + "(declare-fun Y () (Array Int Int))"
          + "(declare-fun Z () (Array Int Int))"
          + "(assert (and "
          + "  (= Y (store X i 0))"
          + "  (= (select Y 5) 1)"
          + "  (= Z (store Y 5 2))"
          + "))";

  static final String ARRAY_QUERY_BV =
      "(declare-fun v () (_ BitVec 64))"
          + "(declare-fun A () (Array (_ BitVec 64) (_ BitVec 32)))"
          + "(declare-fun B () (Array (_ BitVec 64) (_ BitVec 32)))"
          + "(declare-fun C () (Array (_ BitVec 64) (_ BitVec 32)))"
          + "(assert (and "
          + "  (= B (store A v (_ bv0 32)))"
          + "  (= (select B (_ bv5 64)) (_ bv1 32))"
          + "  (= C (store B (_ bv5 64) (_ bv2 32)))"
          + "))";

  @Test
  public void arrayTest3() throws SolverException, InterruptedException {
    requireParser();
    requireArrays();

    BooleanFormula formula = context.getFormulaManager().parse(ARRAY_QUERY_INT);
    checkModelIteration(formula, false);
  }

  @Test
  public void arrayTest4() throws SolverException, InterruptedException {
    requireParser();
    requireArrays();
    requireBitvectors();
    assume()
        .withMessage("solver does not fully support arrays over bitvectors")
        .that(solverToUse())
        .isNotEqualTo(Solvers.PRINCESS);

    BooleanFormula formula = context.getFormulaManager().parse(ARRAY_QUERY_BV);
    checkModelIteration(formula, false);
  }

  @Test
  public void arrayTest5()
      throws SolverException, InterruptedException, IllegalArgumentException, IOException {
    requireParser();
    requireArrays();
    requireBitvectors();

    BooleanFormula formula =
        context
            .getFormulaManager()
            .parse(
                Files.readString(
                    Paths.get("src/org/sosy_lab/java_smt/test/SMT2_UF_and_Array.smt2")));

    checkModelIteration(formula, false);
  }

  @Test
  @SuppressWarnings("resource")
  public void multiCloseTest() throws SolverException, InterruptedException {
    Formula x;
    BooleanFormula eq;
    if (imgr != null) {
      x = imgr.makeVariable("x");
      eq = imgr.equal(imgr.makeVariable("x"), imgr.makeNumber(1));
    } else {
      // Boolector only has bitvectors
      x = bvmgr.makeVariable(8, "x");
      eq = bvmgr.equal(bvmgr.makeVariable(8, "x"), bvmgr.makeBitvector(8, 1));
    }
    ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS);
    try {
      prover.push(eq);
      assertThat(prover).isSatisfiable();
      Model m = prover.getModel();
      try {
        assertThat(m.evaluate(x)).isEqualTo(BigInteger.ONE);
        // close the model several times
      } finally {
        for (int i = 0; i < 10; i++) {
          m.close();
        }
      }
    } finally {
      // close the prover several times
      for (int i = 0; i < 10; i++) {
        prover.close();
      }
    }
  }

  @Test
  @SuppressWarnings("resource")
  public void modelAfterSolverCloseTest() throws SolverException, InterruptedException {
    ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS);
    if (imgr != null) {
      prover.push(imgr.equal(imgr.makeVariable("x"), imgr.makeNumber(1)));
    } else {
      prover.push(bvmgr.equal(bvmgr.makeVariable(8, "x"), bvmgr.makeBitvector(8, 1)));
    }
    assertThat(prover).isSatisfiable();
    Model m = prover.getModel();

    // close prover first
    prover.close();

    // try to access model, this should either fail fast or succeed
    try {
      if (imgr != null) {
        assertThat(m.evaluate(imgr.makeVariable("x"))).isEqualTo(BigInteger.ONE);
      } else {
        assertThat(m.evaluate(bvmgr.makeVariable(8, "x"))).isEqualTo(BigInteger.ONE);
      }
    } catch (IllegalStateException e) {
      // ignore
    } finally {
      m.close();
    }
  }

  @SuppressWarnings("resource")
  @Test(expected = IllegalStateException.class)
  public void testGenerateModelsOption() throws SolverException, InterruptedException {
    try (ProverEnvironment prover = context.newProverEnvironment()) { // no option
      assertThat(prover).isSatisfiable();
      prover.getModel();
      assert_().fail();
    }
  }

  @Test(expected = IllegalStateException.class)
  public void testGenerateModelsOption2() throws SolverException, InterruptedException {
    try (ProverEnvironment prover = context.newProverEnvironment()) { // no option
      assertThat(prover).isSatisfiable();
      prover.getModelAssignments();
      assert_().fail();
    }
  }

  @Test
  public void testGetSmallIntegers1() throws SolverException, InterruptedException {
    requireIntegers();
    evaluateInModel(
        imgr.equal(imgr.makeVariable("x"), imgr.makeNumber(10)),
        imgr.add(imgr.makeVariable("x"), imgr.makeVariable("x")),
        BigInteger.valueOf(20));
  }

  @Test
  public void testGetSmallIntegers2() throws SolverException, InterruptedException {
    requireIntegers();
    evaluateInModel(
        imgr.equal(imgr.makeVariable("x"), imgr.makeNumber(10)),
        imgr.add(imgr.makeVariable("x"), imgr.makeNumber(1)),
        BigInteger.valueOf(11));
  }

  @Test
  public void testGetNegativeIntegers1() throws SolverException, InterruptedException {
    requireIntegers();
    evaluateInModel(
        imgr.equal(imgr.makeVariable("x"), imgr.makeNumber(-10)),
        imgr.add(imgr.makeVariable("x"), imgr.makeNumber(1)),
        BigInteger.valueOf(-9));
  }

  @Test
  public void testGetSmallIntegralRationals1() throws SolverException, InterruptedException {
    requireRationals();
    evaluateInModel(
        rmgr.equal(rmgr.makeVariable("x"), rmgr.makeNumber(1)),
        rmgr.add(rmgr.makeVariable("x"), rmgr.makeVariable("x")),
        Rational.of(2));
  }

  @Test
  public void testGetRationals1() throws SolverException, InterruptedException {
    requireRationals();
    evaluateInModel(
        rmgr.equal(rmgr.makeVariable("x"), rmgr.makeNumber(Rational.ofString("1/3"))),
        rmgr.divide(rmgr.makeVariable("x"), rmgr.makeNumber(2)),
        Rational.ofString("1/6"));
  }

  @Test
  public void testGetBooleans1() throws SolverException, InterruptedException {
    evaluateInModel(bmgr.makeVariable("x"), bmgr.makeBoolean(true), true);
    evaluateInModel(bmgr.makeVariable("x"), bmgr.makeBoolean(false), false);
    evaluateInModel(
        bmgr.makeVariable("x"),
        bmgr.or(bmgr.makeVariable("x"), bmgr.not(bmgr.makeVariable("x"))),
        true);
    evaluateInModel(
        bmgr.makeVariable("x"),
        bmgr.and(bmgr.makeVariable("x"), bmgr.not(bmgr.makeVariable("x"))),
        false);
  }

  private void evaluateInModel(BooleanFormula constraint, Formula variable, Object expectedValue)
      throws SolverException, InterruptedException {

    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(constraint);
      assertThat(prover).isSatisfiable();

      try (Model m = prover.getModel()) {
        assertThat(m.evaluate(variable)).isEqualTo(expectedValue);
      }
    }
  }
}
