package mugauss;

import LoopLists.GaussBean1;
import LoopLists.LoopList;
import LoopsLogic.ModuloList;
import java.math.BigDecimal;

/**
 * Manual validation that NewtonInterpolator produces correct monomial
 * coefficients and vmResult strings for known polynomials.
 *
 * Run with:  java mugauss.NewtonInterpolatorTest
 */
public class NewtonInterpolatorTest {

    public static void main(String[] args) {
        testQuadratic();
        testLinear();
        testConstant();
        testCubic();
        System.out.println("All tests passed.");
    }

    /**
     * p(x) = x^2 - x  →  pArray = [0, -1, 1]
     * dimension = 2, integerRange = 20, halfIntegerRange = 10
     * Monomial coefficients (descending): 1, -1, 0
     * Expected vmResult: "[0.0, 1.0, -1.0, 0.0]"
     */
    static void testQuadratic() {
        int dimension = 2;
        int integerRange = 20;
        int halfIntegerRange = integerRange / 2;

        LoopList loopList = buildLoopList(new int[]{0, -1, 1}, dimension, integerRange);
        ModuloList moduloList = new ModuloList(dimension + 1);

        BigDecimal[] dd = NewtonInterpolator.computeNewtonCoeffs(loopList, dimension, halfIntegerRange, moduloList);
        BigDecimal[] mono = NewtonInterpolator.newtonToMonomial(dd, dimension);

        assertCoeff("x^2-x mono[0] (constant)", 0, mono[0].intValue());
        assertCoeff("x^2-x mono[1] (linear)", -1, mono[1].intValue());
        assertCoeff("x^2-x mono[2] (quadratic)", 1, mono[2].intValue());

        GaussBean1 gBean = new GaussBean1("", loopList, 0);
        NewtonInterpolator.interpolate(gBean, loopList, dimension, halfIntegerRange, moduloList);
        String vmResult = gBean.getVmResult();
        assertString("x^2-x vmResult", "[0.0, 1.0, -1.0, 0.0]", vmResult);

        int eval5 = NewtonInterpolator.evaluateAt(loopList, 5, dimension, halfIntegerRange, moduloList);
        assertCoeff("x^2-x at x=5", 20, eval5);

        System.out.println("  PASS: testQuadratic");
    }

    /**
     * p(x) = 3x + 7  →  pArray = [7, 3]
     * dimension = 1, degree = 1
     * Monomial coefficients (descending): 3, 7
     * Expected vmResult: "[0.0, 3.0, 7.0]"
     */
    static void testLinear() {
        int dimension = 1;
        int integerRange = 20;
        int halfIntegerRange = integerRange / 2;

        LoopList loopList = buildLoopList(new int[]{7, 3}, dimension, integerRange);
        ModuloList moduloList = new ModuloList(dimension + 1);

        GaussBean1 gBean = new GaussBean1("", loopList, 0);
        NewtonInterpolator.interpolate(gBean, loopList, dimension, halfIntegerRange, moduloList);
        assertString("3x+7 vmResult", "[0.0, 3.0, 7.0]", gBean.getVmResult());

        int eval4 = NewtonInterpolator.evaluateAt(loopList, 4, dimension, halfIntegerRange, moduloList);
        assertCoeff("3x+7 at x=4", 19, eval4);

        System.out.println("  PASS: testLinear");
    }

    /**
     * p(x) = 42  →  pArray = [42]
     * dimension = 0, degree = 0
     * Expected vmResult: "[0.0, 42.0]"
     */
    static void testConstant() {
        int dimension = 0;
        int integerRange = 20;
        int halfIntegerRange = integerRange / 2;

        LoopList loopList = buildLoopList(new int[]{42}, dimension, integerRange);
        ModuloList moduloList = new ModuloList(dimension + 1);

        GaussBean1 gBean = new GaussBean1("", loopList, 0);
        NewtonInterpolator.interpolate(gBean, loopList, dimension, halfIntegerRange, moduloList);
        assertString("const 42 vmResult", "[0.0, 42.0]", gBean.getVmResult());

        System.out.println("  PASS: testConstant");
    }

    /**
     * p(x) = x^3  →  pArray = [0, 0, 0, 1]
     * dimension = 3
     * Monomial coefficients (descending): 1, 0, 0, 0
     * Expected vmResult: "[0.0, 1.0, 0.0, 0.0, 0.0]"
     */
    static void testCubic() {
        int dimension = 3;
        int integerRange = 20;
        int halfIntegerRange = integerRange / 2;

        LoopList loopList = buildLoopList(new int[]{0, 0, 0, 1}, dimension, integerRange);
        ModuloList moduloList = new ModuloList(dimension + 1);

        BigDecimal[] dd = NewtonInterpolator.computeNewtonCoeffs(loopList, dimension, halfIntegerRange, moduloList);
        BigDecimal[] mono = NewtonInterpolator.newtonToMonomial(dd, dimension);

        assertCoeff("x^3 mono[0]", 0, mono[0].intValue());
        assertCoeff("x^3 mono[1]", 0, mono[1].intValue());
        assertCoeff("x^3 mono[2]", 0, mono[2].intValue());
        assertCoeff("x^3 mono[3]", 1, mono[3].intValue());

        GaussBean1 gBean = new GaussBean1("", loopList, 0);
        NewtonInterpolator.interpolate(gBean, loopList, dimension, halfIntegerRange, moduloList);
        assertString("x^3 vmResult", "[0.0, 1.0, 0.0, 0.0, 0.0]", gBean.getVmResult());

        int eval3 = NewtonInterpolator.evaluateAt(loopList, 3, dimension, halfIntegerRange, moduloList);
        assertCoeff("x^3 at x=3", 27, eval3);

        System.out.println("  PASS: testCubic");
    }

    /**
     * Builds a LoopList by evaluating the polynomial with given coefficients
     * (ascending power: [a0, a1, ..., an]) over the integer range
     * [-halfIntegerRange, halfIntegerRange).
     */
    static LoopList buildLoopList(int[] coeffs, int dimension, int integerRange) {
        int halfIntegerRange = integerRange / 2;
        LoopList loopList = new LoopList();
        for (int x = -halfIntegerRange; x < halfIntegerRange; x++) {
            BigDecimal val = BigDecimal.ZERO;
            BigDecimal xBD = BigDecimal.valueOf(x);
            for (int k = 0; k < coeffs.length; k++) {
                val = val.add(BigDecimal.valueOf(coeffs[k]).multiply(xBD.pow(k)));
            }
            loopList.add(val);
        }
        return loopList;
    }

    static void assertCoeff(String label, int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        }
    }

    static void assertString(String label, String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected \"" + expected + "\" but got \"" + actual + "\"");
        }
    }
}
