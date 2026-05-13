package com.cukkoo.freecamera;
import net.minecraft.client.Camera;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
public class Dump {
    public static void main(String[] args) {
        System.out.println("--- FIELDS ---");
        for (Field f : Camera.class.getDeclaredFields()) {
            System.out.println(f.getName() + " : " + f.getType().getName());
        }
        System.out.println("--- METHODS ---");
        for (Method m : Camera.class.getDeclaredMethods()) {
            System.out.println(m.getName() + " : " + m.getReturnType().getName());
        }
    }
}
