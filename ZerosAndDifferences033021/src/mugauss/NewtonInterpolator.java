package mugauss;

import LoopLists.GaussBean1;
import LoopLists.LoopList;
import LoopsLogic.ModuloList;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Newton divided difference interpolation with pure BigDecimal arithmetic.
 *
 * Replaces GaussMain.gauss() for recovering polynomial coefficients from
 * LoopList evaluation/difference tables. All computation is exact — no
 * floating-point until the final vmResult string formatting.
 *
 * For integer polynomials at equally-spaced integer nodes, k! always
 * divides the k-th forward difference, so BigDecimal.divide() is exact.
 */
public class NewtonInterpolator {

    private NewtonInterpolator() {}

    /**
     * Computes Newton coefficients from the LoopList values at the
     * interpolation nodes, converts to monomial form, and sets vmResult
     * on both the GaussBean1 and its associated LoopList.
     *
     * The Newton coefficients are cached on the LoopList for later use
     * by {@link #evaluateAt}.
     *
     * @param gBean            transport bean — vmResult is set here
     * @param loopList         contains BigDecimal polynomial/difference values
     * @param degree           polynomial degree (dimension - workNum)
     * @param halfIntegerRange integerRange / 2 (index offset into loopList)
     * @param moduloList       factorial table [1, 1!, 2!, ..., n!]
     */
    public static void interpolate(GaussBean1 gBean, LoopList loopList,
                                   int degree, int halfIntegerRange,
                                   ModuloList moduloList) {
        BigDecimal[] dd = computeNewtonCoeffs(loopList, degree, halfIntegerRange, moduloList);
        loopList.setNewtonCoeffs(dd);

        BigDecimal[] mono = newtonToMonomial(dd, degree);

        List<Object> buffer = new ArrayList<>();
        buffer.add(0.0);
        for (int j = degree; j >= 0; j--)
            buffer.add(mono[j].doubleValue());

        String vmResult = buffer.toString();
        gBean.setVmResult(vmResult);
        gBean.getLoopList().setVmResult(vmResult);
    }

    /**
     * Evaluates the interpolated polynomial at a given integer point.
     * Uses cached Newton coefficients from a prior {@link #interpolate} call.
     *
     * @param loopList         LoopList with cached newtonCoeffs
     * @param atX              integer point to evaluate at
     * @param degree           polynomial degree
     * @param halfIntegerRange integerRange / 2
     * @param moduloList       factorial table
     * @return exact evaluation result as int
     */
    public static int evaluateAt(LoopList loopList, int atX,
                                 int degree, int halfIntegerRange,
                                 ModuloList moduloList) {
        BigDecimal[] dd = loopList.getNewtonCoeffs();
        if (dd == null) {
            dd = computeNewtonCoeffs(loopList, degree, halfIntegerRange, moduloList);
        }

        BigDecimal x = BigDecimal.valueOf(atX);
        BigDecimal result = dd[degree];
        for (int j = degree - 1; j >= 0; j--) {
            BigDecimal node = BigDecimal.valueOf(j + 1);
            result = dd[j].add(x.subtract(node).multiply(result));
        }
        return result.intValue();
    }

    /**
     * Builds Newton divided-difference coefficients from equally-spaced
     * integer nodes 1, 2, ..., degree+1 sampled from the LoopList.
     *
     * Steps:
     *   1. Extract degree+1 values at indices halfIntegerRange+1 .. halfIntegerRange+1+degree
     *   2. In-place forward differences
     *   3. Divide by k! from ModuloList
     */
    static BigDecimal[] computeNewtonCoeffs(LoopList loopList, int degree,
                                            int halfIntegerRange,
                                            ModuloList moduloList) {
        BigDecimal[] dd = new BigDecimal[degree + 1];
        for (int j = 0; j <= degree; j++)
            dd[j] = (BigDecimal) loopList.get(halfIntegerRange + 1 + j);

        for (int level = 1; level <= degree; level++)
            for (int j = degree; j >= level; j--)
                dd[j] = dd[j].subtract(dd[j - 1]);

        for (int j = 0; j <= degree; j++)
            dd[j] = dd[j].divide((BigDecimal) moduloList.get(j));

        return dd;
    }

    /**
     * Converts Newton-form coefficients with nodes x_j = j+1 into
     * monomial-form coefficients (ascending power order).
     *
     * Uses synthetic expansion: starting from the highest-degree Newton
     * coefficient, repeatedly multiply by (x - node) and accumulate.
     */
    static BigDecimal[] newtonToMonomial(BigDecimal[] dd, int degree) {
        BigDecimal[] mono = new BigDecimal[degree + 1];
        Arrays.fill(mono, BigDecimal.ZERO);
        mono[0] = dd[degree];

        for (int j = degree - 1; j >= 0; j--) {
            BigDecimal node = BigDecimal.valueOf(j + 1);
            for (int i = mono.length - 1; i > 0; i--)
                mono[i] = mono[i - 1].subtract(node.multiply(mono[i]));
            mono[0] = dd[j].subtract(node.multiply(mono[0]));
        }
        return mono;
    }
}
