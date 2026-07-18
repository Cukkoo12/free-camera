package com.cukkoo.freecamera.recording;

import com.cukkoo.freecamera.api.CameraMode;
import com.cukkoo.freecamera.path.*;
import java.nio.file.Files;

public final class StorageDevelopmentChecks {
 public static void main(String[] args)throws Exception{
  var root=Files.createTempDirectory("free-camera-storage-check");
  CameraRecordingStorage recordings=new CameraRecordingStorage(root);CameraRecording r=new CameraRecording();r.setName("Take");r.add(new CameraRecordingFrame(0,0,0,0,0,0,0,1,CameraMode.FREE_CAMERA),10);r.add(new CameraRecordingFrame(1,1,2,3,10,20,360,3,CameraMode.FREE_CAMERA),10);
  var first=recordings.save(r,"unsafe : name");var second=recordings.duplicate(first,"copy");req(Files.exists(first)&&Files.exists(second),"recording create/duplicate");req(recordings.listMetadata(10).size()==2,"recording metadata");var renamed=recordings.rename(second,"renamed");req(recordings.load(renamed,10).size()==2,"recording rename/lazy load");recordings.delete(renamed);req(!Files.exists(renamed),"recording delete");
  expectFailure(()->recordings.load(first,1),"recording count limit");
  var corrupt=recordings.directory().resolve("corrupt.json");Files.writeString(corrupt,"{");req(recordings.listMetadata(10).stream().anyMatch(CameraRecordingStorage.Metadata::corrupt),"corrupt recording reporting");req(Files.exists(corrupt),"corrupt recording preserved");
  var duplicateTimes=recordings.directory().resolve("duplicate-times.json");Files.writeString(duplicateTimes,"{\"schemaVersion\":1,\"recording\":{\"name\":\"bad\",\"frames\":[{\"time\":0,\"x\":0,\"y\":0,\"z\":0,\"yaw\":0,\"pitch\":0,\"roll\":0,\"zoom\":1,\"mode\":\"FREE_CAMERA\"},{\"time\":0,\"x\":0,\"y\":0,\"z\":0,\"yaw\":0,\"pitch\":0,\"roll\":0,\"zoom\":1,\"mode\":\"FREE_CAMERA\"}]}}");expectFailure(()->recordings.load(duplicateTimes,10),"duplicate timestamps");
  CameraPathStorage paths=new CameraPathStorage(root);CameraPath path=new CameraPath();path.setName("Path");path.add(new CameraKeyframe(0,0,0,0,0,0,1,1,PathEasing.LINEAR,0));path.add(new CameraKeyframe(1,2,3,20,10,360,2,1,PathEasing.SMOOTH,0));var pathFile=paths.save(path,"path");req(paths.listMetadata(10).getFirst().keyframes()==2,"path metadata");var pathCopy=paths.duplicate(pathFile,"copy");var pathRenamed=paths.rename(pathCopy,"renamed");req(paths.load(pathRenamed,10).size()==2,"path CRUD");paths.delete(pathRenamed);
  expectFailure(()->paths.load(pathFile,1),"path count limit");
  var invalidPath=paths.directory().resolve("invalid.json");Files.writeString(invalidPath,"{\"schemaVersion\":99}");req(paths.listMetadata(10).stream().anyMatch(CameraPathStorage.Metadata::corrupt),"invalid schema reporting");req(Files.exists(invalidPath),"corrupt path preserved");
  System.out.println("Storage metadata, lazy load, CRUD, bounds, validation, atomic save, and corrupt-file preservation checks passed.");
 }
 private static void expectFailure(IoAction action,String name)throws Exception{try{action.run();throw new AssertionError(name+" accepted");}catch(java.io.IOException expected){}}
 private static void req(boolean value,String message){if(!value)throw new AssertionError(message);}private interface IoAction{void run()throws Exception;}
}
