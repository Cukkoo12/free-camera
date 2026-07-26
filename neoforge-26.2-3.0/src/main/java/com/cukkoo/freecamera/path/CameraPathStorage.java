package com.cukkoo.freecamera.path;

import com.cukkoo.freecamera.recording.CameraRecordingStorage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import com.google.gson.stream.JsonReader;

public final class CameraPathStorage {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final long MAX_FILE_BYTES=16L*1024L*1024L;
    private final Path directory;
    public record Metadata(Path file,String name,int keyframes,double duration,PathInterpolation interpolation,boolean corrupt,String problem){}
    public CameraPathStorage(Path config){directory=config.resolve("free-camera/paths");}
    public Path directory(){return directory;}

    public List<Metadata> listMetadata(int limit)throws IOException{
        Files.createDirectories(directory);ArrayList<Metadata> out=new ArrayList<>();
        try(var files=Files.list(directory)){files.filter(p->p.getFileName().toString().endsWith(".json")).sorted().forEach(p->{
            try{out.add(scanMetadata(p,limit));}
            catch(IOException e){out.add(new Metadata(p,stem(p),0,0,PathInterpolation.LINEAR,true,e.getMessage()));}
        });}return List.copyOf(out);
    }
    private Metadata scanMetadata(Path file,int maximum)throws IOException{
        if(Files.size(file)>MAX_FILE_BYTES)throw new IOException("Path file exceeds safety limit");int schema=-1,count=0;double duration=0;String name=stem(file);PathInterpolation interpolation=null;boolean pathFound=false;
        try(JsonReader reader=new JsonReader(Files.newBufferedReader(file,StandardCharsets.UTF_8))){reader.beginObject();while(reader.hasNext()){String root=reader.nextName();if(root.equals("schemaVersion"))schema=reader.nextInt();else if(root.equals("path")){pathFound=true;reader.beginObject();while(reader.hasNext()){String field=reader.nextName();if(field.equals("name"))name=reader.nextString();else if(field.equals("interpolation"))interpolation=PathInterpolation.valueOf(reader.nextString());else if(field.equals("frames")){reader.beginArray();while(reader.hasNext()){if(++count>maximum)throw new IOException("Path keyframe limit exceeded");double zoom=Double.NaN,pitch=Double.NaN,segment=0,hold=0;PathEasing easing=null;int fields=0;reader.beginObject();while(reader.hasNext()){String n=reader.nextName();switch(n){case "x"->{if(!Double.isFinite(reader.nextDouble()))throw new IOException("Non-finite keyframe value");fields|=1;}case "y"->{if(!Double.isFinite(reader.nextDouble()))throw new IOException("Non-finite keyframe value");fields|=2;}case "z"->{if(!Double.isFinite(reader.nextDouble()))throw new IOException("Non-finite keyframe value");fields|=4;}case "yaw"->{if(!Double.isFinite(reader.nextDouble()))throw new IOException("Non-finite keyframe value");fields|=8;}case "pitch"->{pitch=reader.nextDouble();fields|=16;}case "roll"->{if(!Double.isFinite(reader.nextDouble()))throw new IOException("Non-finite keyframe value");fields|=32;}case "zoom"->{zoom=reader.nextDouble();fields|=64;}case "duration"->{segment=reader.nextDouble();fields|=128;}case "easing"->{easing=PathEasing.valueOf(reader.nextString());fields|=256;}case "hold"->{hold=reader.nextDouble();fields|=512;}default->reader.skipValue();}}reader.endObject();if(fields!=1023||!Double.isFinite(pitch)||pitch<-90||pitch>90||!Double.isFinite(zoom)||zoom<=0||!Double.isFinite(segment)||segment<0||!Double.isFinite(hold)||hold<0||easing==null)throw new IOException("Invalid or incomplete path keyframe");if(reader.hasNext())duration+=segment+hold;}reader.endArray();}else reader.skipValue();}reader.endObject();}else reader.skipValue();}reader.endObject();}catch(RuntimeException e){throw new IOException("Malformed path JSON",e);}
        if(schema!=CameraPath.SCHEMA_VERSION||!pathFound||interpolation==null)throw new IOException("Unsupported path schema");return new Metadata(file,name,count,duration,interpolation,false,"");
    }
    public Path create(String name)throws IOException{CameraPath path=new CameraPath();path.setName(name);return save(path,name,false);}
    public Path save(CameraPath path,String name)throws IOException{return save(path,name,false);}
    public Path save(CameraPath path,String name,boolean replace)throws IOException{
        validate(path,Integer.MAX_VALUE);Files.createDirectories(directory);String safe=CameraRecordingStorage.sanitize(name);Path target=replace?directory.resolve(safe+".json"):unique(safe);atomicWrite(target,GSON.toJson(new Stored(CameraPath.SCHEMA_VERSION,path)));return target;
    }
    public CameraPath load(Path file)throws IOException{return load(file,CameraPath.MAX_KEYFRAMES);}
    public CameraPath load(Path file,int maximum)throws IOException{
        requireOwned(file);if(maximum<1||Files.size(file)>MAX_FILE_BYTES)throw new IOException("Path file exceeds safety limit");final Stored s;
        scanMetadata(file,maximum);
        try{s=GSON.fromJson(Files.readString(file,StandardCharsets.UTF_8),Stored.class);}catch(RuntimeException e){throw new IOException("Malformed path JSON",e);}
        if(s==null||s.schemaVersion()!=CameraPath.SCHEMA_VERSION||s.path()==null)throw new IOException("Unsupported path schema");validate(s.path(),maximum);
        CameraPath clean=new CameraPath();clean.setName(s.path().name()==null||s.path().name().isBlank()?stem(file):s.path().name());clean.setInterpolation(s.path().interpolation());for(CameraKeyframe f:s.path().frames())clean.add(f);return clean;
    }
    public Path rename(Path source,String name)throws IOException{requireOwned(source);CameraPath path=load(source);String safe=CameraRecordingStorage.sanitize(name);path.setName(safe);Path target=save(path,safe,false);Files.delete(source);return target;}
    public Path duplicate(Path source,String name)throws IOException{requireOwned(source);CameraPath path=load(source);String safe=CameraRecordingStorage.sanitize(name);path.setName(safe);return save(path,safe,false);}
    public void delete(Path source)throws IOException{requireOwned(source);Files.delete(source);}
    private static void validate(CameraPath path,int maximum)throws IOException{
        if(path.frames().size()>maximum)throw new IOException("Path keyframe limit exceeded");if(path.interpolation()==null)throw new IOException("Invalid interpolation");
        for(CameraKeyframe f:path.frames()){if(f==null||!finite(f)||f.pitch()<-90||f.pitch()>90||f.duration()<0||f.hold()<0||f.easing()==null)throw new IOException("Invalid path keyframe");}
    }
    private void requireOwned(Path p)throws IOException{Path n=p.toAbsolutePath().normalize();if(!n.getParent().equals(directory.toAbsolutePath().normalize())||!n.getFileName().toString().endsWith(".json"))throw new IOException("Path is outside storage directory");}
    private void atomicWrite(Path target,String json)throws IOException{Path temp=Files.createTempFile(directory,".path-",".tmp");try{Files.writeString(temp,json,StandardCharsets.UTF_8,StandardOpenOption.TRUNCATE_EXISTING);try{Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}}
    private Path unique(String name){Path p=directory.resolve(name+".json");for(int i=2;Files.exists(p)&&i<10000;i++)p=directory.resolve(name+"-"+i+".json");return p;}
    private static String stem(Path p){String n=p.getFileName().toString();return n.endsWith(".json")?n.substring(0,n.length()-5):n;}
    private static boolean finite(CameraKeyframe f){return Double.isFinite(f.x())&&Double.isFinite(f.y())&&Double.isFinite(f.z())&&Float.isFinite(f.yaw())&&Float.isFinite(f.pitch())&&Double.isFinite(f.roll())&&Double.isFinite(f.zoom())&&f.zoom()>0&&Double.isFinite(f.duration())&&Double.isFinite(f.hold());}
    private record Stored(int schemaVersion,CameraPath path){}
}
