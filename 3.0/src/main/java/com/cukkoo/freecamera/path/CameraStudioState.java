package com.cukkoo.freecamera.path;
public final class CameraStudioState{
 private CameraPath path=new CameraPath();private int selected=-1;private java.nio.file.Path file;private boolean dirty;
 public CameraPath path(){return path;}public void newPath(){path=new CameraPath();selected=-1;file=null;dirty=true;}
 public void load(CameraPath value,java.nio.file.Path source){path=value;file=source;selected=value.size()==0?-1:0;dirty=false;}
 public void discardNew(){path=new CameraPath();file=null;selected=-1;dirty=false;}
 public int selected(){return selected;}public void select(int index){selected=index>=0&&index<path.size()?index:-1;}
 public java.nio.file.Path file(){return file;}public void setFile(java.nio.file.Path value){file=value;dirty=false;}public boolean dirty(){return dirty;}public void markDirty(){dirty=true;}
}
