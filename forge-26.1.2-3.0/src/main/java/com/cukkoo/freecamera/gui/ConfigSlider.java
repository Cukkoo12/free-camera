package com.cukkoo.freecamera.gui;
import net.minecraft.client.gui.components.AbstractSliderButton;import net.minecraft.client.gui.components.Tooltip;import net.minecraft.network.chat.Component;import java.util.function.DoubleConsumer;import java.util.function.DoubleSupplier;
final class ConfigSlider extends AbstractSliderButton{
 private final double minimum,maximum;private final DoubleConsumer setter;
 ConfigSlider(int x,int y,int width,Component label,Component tooltip,double min,double max,DoubleSupplier getter,DoubleConsumer setter){super(x,y,width,18,label,normalize(getter.getAsDouble(),min,max));this.minimum=min;this.maximum=max;this.setter=setter;setTooltip(Tooltip.create(tooltip));updateMessage();}
 @Override protected void updateMessage(){double actual=minimum+(maximum-minimum)*value;setMessage(Component.literal(format(actual)));}
 @Override protected void applyValue(){setter.accept(minimum+(maximum-minimum)*value);updateMessage();}
 private static double normalize(double v,double min,double max){return Math.clamp((v-min)/(max-min),0,1);}private static String format(double v){return Math.abs(v-Math.rint(v))<1e-6?Long.toString(Math.round(v)):String.format(java.util.Locale.ROOT,"%.2f",v);}
}
