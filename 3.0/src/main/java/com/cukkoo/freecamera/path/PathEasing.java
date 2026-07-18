package com.cukkoo.freecamera.path;
public enum PathEasing { LINEAR, SMOOTH, CINEMATIC;
    public double apply(double t){t=Math.clamp(t,0,1);return switch(this){case LINEAR->t;case SMOOTH->t*t*(3-2*t);case CINEMATIC->t*t*t*(t*(t*6-15)+10);};}
}
