package com.cukkoo.freecamera.path;
import java.util.ArrayList;import java.util.Collections;import java.util.List;
public final class CameraPath{
 public static final int SCHEMA_VERSION=1,MAX_KEYFRAMES=4096;private String name="Path";private final ArrayList<CameraKeyframe> frames=new ArrayList<>();private PathInterpolation interpolation=PathInterpolation.SMOOTH;
 public boolean add(CameraKeyframe f){if(frames.size()>=MAX_KEYFRAMES)return false;frames.add(f);return true;}public void remove(int i){frames.remove(i);}public void duplicate(int i){if(frames.size()<MAX_KEYFRAMES)frames.add(i+1,frames.get(i));}
 public void move(int from,int to){CameraKeyframe f=frames.remove(from);frames.add(Math.clamp(to,0,frames.size()),f);}public void replace(int i,CameraKeyframe f){frames.set(i,f);}public List<CameraKeyframe> frames(){return Collections.unmodifiableList(frames);}public CameraKeyframe get(int i){return frames.get(i);}public int size(){return frames.size();}
 public String name(){return name;}public void setName(String v){name=v;}public PathInterpolation interpolation(){return interpolation;}public void setInterpolation(PathInterpolation v){interpolation=v;}
}
