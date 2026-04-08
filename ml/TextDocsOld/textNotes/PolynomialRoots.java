

https://code.mikeyaworski.com/java/polynomial_roots


import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

 /**
 * Calculates the real roots of any polynomial using the rational roots theorem
 * (has yet to implement rational roots perfectly, since it will not work unless at least one factor is an integer).
 *
 * @author Michael Yaworski of http://mikeyaworski.com
 * @version July 2nd, 2015
 */
public class PolynomialRoots {

    // index 0 starts at leading coefficient and last index is the constant
    public static List<Double> getRoots(List<Double> coefficients) {
        return getRoots(coefficients, new ArrayList<Double>());
    }

    public static List<Double> getRoots(List<Double> coefficients, List<Double> roots) {
        if (coefficients.size() > 3) {
            Double k = findK(coefficients);
            if (k != null) {
                roots.add(k);
                coefficients = dividePolynomialByXMinusK(coefficients, k);
                return getRoots(coefficients, roots);
            }
        } else if (coefficients.size() == 3) {
            roots.addAll(getQuadraticRoots(coefficients.get(0), coefficients.get(1), coefficients.get(2)));
        } else if (coefficients.size() == 2) {
            roots.addAll(getQuadraticRoots(0, coefficients.get(0), coefficients.get(1)));
        }
        // sort the roots list
        Collections.sort(roots, new Comparator<Double>() {
                @Override
            public int compare(Double d1, Double d2){
                return d1.compareTo(d2);
            }
        });
        // remove duplicates from the list
        for (int i = 0; i < roots.size(); i++) {
            if (roots.get(i).doubleValue() == 0) roots.set(i, new Double("0"));
            for (int j = i + 1; j < roots.size(); j++) {
                if (roots.get(i).doubleValue() == roots.get(j).doubleValue()) {
                    roots.remove(j);
                    j--;
                }
            }
        }
        
        return roots;
    }

    public static Double findK(List<Double> coefficients) {
        if (coefficients.size() > 2) {
            if (coefficients.get(coefficients.size() - 1) == 0) return 0d;
            List<Double> trialKValues = new ArrayList<Double>();

            Double constant = coefficients.get(coefficients.size() - 1);
            Double leadingCoefficient = coefficients.get(0);

            for (int i = 1; i <= Math.abs(constant); i++) {
                if (Math.abs(constant) % i == 0) {
                    trialKValues.add((double)i);
                    trialKValues.add((double)i * -1);

                    for (int j = 1; j <= Math.abs(leadingCoefficient); j++) {
                        if (Math.abs(leadingCoefficient) % j == 0) {
                            if ((i/j) == ((double)i/j)) {
                                trialKValues.add(((double)i/j));
                                trialKValues.add(((double)i/j) * -1);
                            }
                        }
                    }
                }
            }

            for (double k : trialKValues) {
                double sumOfTerms = 0;
                for (int i = 0; i < coefficients.size(); i++) { // f(x=k)
                    sumOfTerms += coefficients.get(i) * Math.pow(k, coefficients.size() - 1 - i);
                }
                if (sumOfTerms == 0) { // if f(x=k) == 0
                    return k;
                }
            }
        }
        return null;
    }
    
    public static List<Double> dividePolynomialByXMinusK(List<Double> coefficients, Double k) {
        if (k != null && coefficients != null && coefficients.size() > 2) {
            List<Double> newCoefficients = new ArrayList<Double>();
            newCoefficients.add(coefficients.get(0));

            for (int i = 1; i < coefficients.size() - 1; i++) {
                newCoefficients.add(newCoefficients.get(i-1) * k + coefficients.get(i));
            }

            int lastIndex = coefficients.size() - 1;
            if (newCoefficients.get(lastIndex-1) * k + coefficients.get(lastIndex) != 0) return null;

            return newCoefficients;
        }
        return null;
    }

    public static List<Double> getQuadraticRoots(double a, double b, double c) {

        List<Double> roots = new ArrayList<Double>();

        if (a != 0) {
            int nRoots = 0;
            double discriminant = Math.pow(b, 2) - 4*a*c;
            if (discriminant > 0) nRoots = 2;
            else if (discriminant == 0) nRoots = 1;

            if (nRoots == 1) {
                roots.add(b * -1 / (2 * a));
            } else if (nRoots == 2) {
                roots.add(((b * -1 + Math.sqrt(discriminant)) / (2 * a)));
                roots.add(((b * -1 - Math.sqrt(discriminant)) / (2 * a)));
            }
        } else {
            roots.add(c * -1 / b);
        }
        return roots;
    }
}