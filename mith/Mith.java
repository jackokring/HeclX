package mith;
/*
 * Sorry if you recieve errors from the underlaying trig in JavaME
 * but a lot of space has been consumed already for micro
 */

/**
 *
 * @author jacko
 */
public class Mith {

    public static double atan2(double y, double x) {
        //this looks right
        if(x==0) if(y>=0) return 0; else return Math.PI;
        double res = atan(y/x);
        if(y>=0) return res;
        if(x>0) return Math.PI-res;
        return -Math.PI-res;
    }

    static double powi(double x, double y) {
        int p = (int) y;//should be in range
        double mul = x;
        double acc = 1;
        for(int j=0;j<32;j++) {
            if((p&1)==1) acc *= mul;
            p >>= 1;
            mul *= mul;
        }
        return acc;
    }

    public static double pow(double x, double y) {
        //this looks complex, but is really just a way of
        //creation of the real value on -x while
        //deciding that zero behaves enough like a NaN
        //as to be the best real value for sqrt(-x)
        double neg = sqrt(x)*Math.sin(Math.PI*y);
        //should do the real part
        neg = sqrt(1-(neg*neg)/x);
        return exp(log(x)*y)*neg;//done so as to flex pow.
        //this whole routine will impact analytical
        //calculus as the differential is ????
    }

    public static double sqrt(double x) {
        if(x<0) return 0;//a real analysis
        return Math.sqrt(x);
    }

    public static double acos(double x) {
        return 2*atan(sqrt(1-x*x)/(1+x));
    }

    public static double asin(double x) {
        return 2*atan(x/(1+sqrt(1-x*x)));
    }

    public static double log(double x) {
        int flip = 1;
        if(x>1||x<-1) { x = 1/x; flip = -1;}
        if(x<0) x=-x;//symmetric
        int sc = 2;
        double mul;
        //smaller range for quadratic gain, without too
        //much  loss of precission
        while(x<0.875||x>1.125) {
            x = Math.sqrt(x);
            sc <<= 1;
        }
        double acc;
        acc = x = (mul = (x-1)/(x+1));
        x *= x;
        for(int i = 3;i < 8;i+=2) {
            acc += mul/i;
            mul *= x;
        }
        return sc*flip*acc;//an accurate log
    }

    public static double atan(double x) {
        boolean flip = false;
        if(x>1||x<-1) { flip = true; x = 1/x; }//cos/sin??+angle??
        int sc = 1;
        while(x<-0.125||x>0.125) {
            x = x/(1+Math.sqrt(1+x*x));
            sc *= 2;
        }
        double mul = x;
        double acc = x;
        x *= -x;
        for(int i = 3;i < 16;i+=2) {
            acc += mul/i;
            mul *= x;
        }
        acc *= sc;
        //+/-45deg
        if(flip) {
            acc = ((acc>0) ? Math.PI/2-acc : -Math.PI/2+acc);
        }
        return acc;
    }

    final static double ln2 = log(2);

    public static double exp(double x) {
        boolean flip = false;
        if(x<0) { flip = true; x = -x;}//all positive
        int digits = (int)(log(x)*ln2);
        double po = powi(2,digits);
        x = x/po;//residual
        int raz = 1;
        //focus series for gain
        while(x>0.125) {
            raz *= 2;
            x /= 2;
        }
        //the exp
        double acc = 1;//first term
        double fac = 1;
        double mul = 1;
        for(int i = 1;i < 8;i++) {
            fac *= i;
            mul *= x;
            acc += mul/fac;
        }
        x = powi(acc,po*raz);
        if(flip) x = 1/x;
        return x;//the actual exp
    }
}