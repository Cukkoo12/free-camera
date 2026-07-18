package com.cukkoo.freecamera.recording;

import com.cukkoo.freecamera.api.CameraMode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.google.gson.stream.JsonReader;

/** Bounded, atomic recording persistence. Listing never retains sample arrays. */
public final class CameraRecordingStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long MAX_FILE_BYTES = 128L * 1024L * 1024L;
    private final Path directory;

    public record Metadata(Path file, String name, double duration, int sampleCount,
                           double sampleRate, FileTime modified, boolean corrupt, String problem) {}

    public CameraRecordingStorage(Path configDirectory) { directory = configDirectory.resolve("free-camera/recordings"); }
    public Path directory() { return directory; }

    public List<Metadata> listMetadata(int maximumSamples) throws IOException {
        Files.createDirectories(directory);
        ArrayList<Metadata> result = new ArrayList<>();
        try (var files = Files.list(directory)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .forEach(p -> result.add(readMetadata(p, maximumSamples)));
        }
        return List.copyOf(result);
    }

    private Metadata readMetadata(Path file, int maximumSamples) {
        FileTime modified;
        try { modified = Files.getLastModifiedTime(file); } catch (IOException e) { modified = FileTime.fromMillis(0); }
        try { return scanMetadata(file,maximumSamples,modified); } catch (IOException e) {
            return new Metadata(file, stem(file), 0, 0, 0, modified, true, e.getMessage());
        }
    }

    private Metadata scanMetadata(Path file,int maximumSamples,FileTime modified)throws IOException{
        if(Files.size(file)>MAX_FILE_BYTES)throw new IOException("Recording file exceeds safety limit");int schema=-1,count=0;double previous=-1,duration=0;boolean recordingFound=false;
        try(JsonReader reader=new JsonReader(Files.newBufferedReader(file,StandardCharsets.UTF_8))){reader.beginObject();while(reader.hasNext()){String root=reader.nextName();if(root.equals("schemaVersion"))schema=reader.nextInt();else if(root.equals("recording")){recordingFound=true;reader.beginObject();while(reader.hasNext()){String field=reader.nextName();if(field.equals("frames")){reader.beginArray();while(reader.hasNext()){if(++count>maximumSamples)throw new IOException("Recording sample limit exceeded");double time=0,zoom=Double.NaN,pitch=Double.NaN;int fields=0;reader.beginObject();while(reader.hasNext()){String n=reader.nextName();switch(n){case "time"->{time=reader.nextDouble();fields|=1;}case "x"->{if(!Double.isFinite(reader.nextDouble()))throw new IOException("Non-finite recording value");fields|=2;}case "y"->{if(!Double.isFinite(reader.nextDouble()))throw new IOException("Non-finite recording value");fields|=4;}case "z"->{if(!Double.isFinite(reader.nextDouble()))throw new IOException("Non-finite recording value");fields|=8;}case "yaw"->{if(!Double.isFinite(reader.nextDouble()))throw new IOException("Non-finite recording value");fields|=16;}case "pitch"->{pitch=reader.nextDouble();fields|=32;}case "roll"->{if(!Double.isFinite(reader.nextDouble()))throw new IOException("Non-finite recording value");fields|=64;}case "zoom"->{zoom=reader.nextDouble();fields|=128;}case "mode"->{CameraMode.valueOf(reader.nextString());fields|=256;}default->reader.skipValue();}}reader.endObject();if(fields!=511||!Double.isFinite(time)||time<0||time>3600||(previous>=0&&time<=previous)||!Double.isFinite(pitch)||pitch<-90||pitch>90||!Double.isFinite(zoom)||zoom<=0||zoom>10)throw new IOException("Invalid or incomplete recording sample");previous=time;duration=time;}reader.endArray();}else reader.skipValue();}reader.endObject();}else reader.skipValue();}reader.endObject();}catch(RuntimeException e){throw new IOException("Malformed recording JSON",e);}
        if(schema!=CameraRecording.SCHEMA_VERSION||!recordingFound)throw new IOException("Unsupported recording schema");double rate=duration>0&&count>1?(count-1)/duration:0;return new Metadata(file,stem(file),duration,count,rate,modified,false,"");
    }

    public Path create(String name) throws IOException { return save(new CameraRecording(), name, false); }
    public Path save(CameraRecording recording, String requestedName) throws IOException { return save(recording, requestedName, false); }
    public Path save(CameraRecording recording, String requestedName, boolean replace) throws IOException {
        validateRecording(recording, Integer.MAX_VALUE);
        Files.createDirectories(directory);
        String safe = sanitize(requestedName);
        Path target = replace ? directory.resolve(safe + ".json") : unique(safe);
        atomicWrite(target, GSON.toJson(new Stored(CameraRecording.SCHEMA_VERSION, recording)));
        return target;
    }

    public CameraRecording load(Path file, int maximumSamples) throws IOException {
        requireOwnedJson(file);
        if (maximumSamples < 1 || Files.size(file) > MAX_FILE_BYTES) throw new IOException("Recording file exceeds safety limit");
        scanMetadata(file,maximumSamples,Files.getLastModifiedTime(file));
        final Stored stored;
        try { stored = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Stored.class); }
        catch (RuntimeException e) { throw new IOException("Malformed recording JSON", e); }
        if (stored == null || stored.schemaVersion() != CameraRecording.SCHEMA_VERSION || stored.recording() == null)
            throw new IOException("Unsupported recording schema");
        validateRecording(stored.recording(), maximumSamples);
        CameraRecording clean = new CameraRecording();
        clean.setName(nonBlank(stored.recording().name(), stem(file)));
        for (CameraRecordingFrame frame : stored.recording().frames()) clean.add(frame, maximumSamples);
        return clean;
    }

    public Path rename(Path source, String name) throws IOException {
        requireOwnedJson(source); CameraRecording recording=load(source,Integer.MAX_VALUE);String safe=sanitize(name);recording.setName(safe);Path target=unique(safe);atomicWrite(target,GSON.toJson(new Stored(CameraRecording.SCHEMA_VERSION,recording)));Files.delete(source);return target;
    }
    public Path duplicate(Path source, String name) throws IOException {
        requireOwnedJson(source);CameraRecording recording=load(source,Integer.MAX_VALUE);String safe=sanitize(name);recording.setName(safe);return save(recording,safe,false);
    }
    public void delete(Path source) throws IOException { requireOwnedJson(source); Files.delete(source); }

    private static void validateRecording(CameraRecording recording, int maximumSamples) throws IOException {
        if (recording.frames().size() > maximumSamples) throw new IOException("Recording sample limit exceeded");
        double previous = -1;
        for (CameraRecordingFrame f : recording.frames()) {
            if (f == null || !finite(f) || f.time() < 0 || f.time() <= previous && previous >= 0)
                throw new IOException("Invalid or duplicate recording timestamp/value");
            if (f.pitch() < -90 || f.pitch() > 90) throw new IOException("Invalid recording pitch");
            previous = f.time();
        }
    }
    private void requireOwnedJson(Path file) throws IOException {
        Path normalized = file.toAbsolutePath().normalize();
        if (!normalized.getParent().equals(directory.toAbsolutePath().normalize()) || !normalized.getFileName().toString().endsWith(".json"))
            throw new IOException("Recording path is outside storage directory");
    }
    private void atomicWrite(Path target, String json) throws IOException {
        Path temp = Files.createTempFile(directory, ".recording-", ".tmp");
        try {
            Files.writeString(temp, json, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            try { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
    private Path unique(String name) { Path p=directory.resolve(name+".json"); for(int i=2;Files.exists(p)&&i<10000;i++)p=directory.resolve(name+"-"+i+".json"); return p; }
    public static String sanitize(String name) { String s=name==null?"":name.trim().replaceAll("[^a-zA-Z0-9._-]","_"); return s.isBlank()?"recording":s.substring(0,Math.min(64,s.length())); }
    private static String stem(Path p) { String n=p.getFileName().toString(); return n.endsWith(".json")?n.substring(0,n.length()-5):n; }
    private static String nonBlank(String s,String fallback){return s==null||s.isBlank()?fallback:s;}
    private static boolean finite(CameraRecordingFrame f){return Double.isFinite(f.time())&&Double.isFinite(f.x())&&Double.isFinite(f.y())&&Double.isFinite(f.z())&&Float.isFinite(f.yaw())&&Float.isFinite(f.pitch())&&Double.isFinite(f.roll())&&Double.isFinite(f.zoom())&&f.zoom()>0;}
    private record Stored(int schemaVersion,CameraRecording recording){}
}
