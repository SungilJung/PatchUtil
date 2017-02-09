package com.codescroll.util.patch.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.eclipse.core.runtime.IProgressMonitor;

import com.codescroll.util.patch.Const;

/**
 * 패치 적용하는 유틸
 * @author Jung
 *
 */
public class PatchExecutor {

	/**
	 * 패치를 적용한다.
	 * @param destRootPath 패치파일들이 적용될 최상위 경로
	 * @param runnableJar Runnable jar 파일
	 * @param progressMonitor 프로그래스 다이얼로그에 사용되는 progressMonitor <br> null인 경우에도 사용 가능
	 * @return
	 */
	public static boolean applyPatch(String destRootPath, JarFile runnableJar, IProgressMonitor progressMonitor){
		boolean isSuccess = true;
		Enumeration<JarEntry> entries = runnableJar.entries();

		while(entries.hasMoreElements()) {

			JarEntry jarEntry = entries.nextElement();
			String patchPath = jarEntry.getName();

			if(patchPath.startsWith(Const.PLUGINS_DIR_NAME)){
				String destPatchPath = destRootPath+patchPath.replaceFirst(Const.PLUGINS_DIR_NAME, "").trim();

				try {
					if(jarEntry.isDirectory()){
						makeDir(new File(destPatchPath));
					}else{
						writeContents(runnableJar.getInputStream(jarEntry),destPatchPath);
					}
				}catch (IOException e) {
					isSuccess = false;
					e.printStackTrace();
					return isSuccess;
				}

				if(progressMonitor != null){
					progressMonitor.subTask(destPatchPath);
					progressMonitor.worked(1);
				}
				
			}
		}
		return isSuccess;
	}

	/**
	 * 디렉터리가 없을 경우 디렉터리를 생성한다.
	 * @see File#mkdirs()
	 */
	private static void makeDir(File dir){
		if(!dir.exists()){
			dir.mkdirs();
		}
	}

	/**
	 * 파일경로의 파일에 데이터를 작성한다.
	 * @param inputStream
	 * @param filePath
	 */
	private static void writeContents(InputStream inputStream, String filePath) throws IOException{	

		BufferedOutputStream buffOutputStream = null;
		BufferedInputStream  buffInputStream = null;
		File file = new File(filePath);

		try{

			file.createNewFile();
			buffInputStream = new BufferedInputStream(inputStream);

			FileOutputStream outputStream = new FileOutputStream(file);
			buffOutputStream = new BufferedOutputStream(outputStream);

			byte[] byteArray = new byte[1024];
			int read;

			//While the input stream has bytes
			while ((read = buffInputStream.read(byteArray)) > 0) 
			{
				//Write the bytes to the output stream
				buffOutputStream.write(byteArray, 0, read);
			}
		}catch(IOException e){
			e.printStackTrace();
			throw e;
		}finally{
			try {
				if(buffInputStream != null){
					buffInputStream.close();
				}

				if(buffOutputStream != null){
					buffOutputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 패치를 적용한다.
	 * @see PatchFileUtils#copyFilesAndDirs(String, String)
	 * @param srcRootDir 적용할 패치 파일들이 있는 최상위 디렉터리 경로
	 * @param destRootDir 패치파일들이 적용될 최상위 경로
 	 * @param progressMonitor 프로그래스 다이얼로그에 사용되는 progressMonitor <br> null인 경우에도 사용 가능
	 * @return
	 */
	public static boolean applyPatch(String destRootDir, String srcRootDir, IProgressMonitor progressMonitor){
		boolean isApplayPatch = false;
		Set<String> pathFilePathList = getPatchFilePathList(srcRootDir);
		try {
			PatchFileUtils.copyFilesAndDirs(srcRootDir, destRootDir, pathFilePathList, progressMonitor);
			isApplayPatch = true;
		} catch (IOException e) {
			e.printStackTrace();
			return isApplayPatch;
		}
		return isApplayPatch;
	}

	/**
	 * 최상위 경로를 제외한 패치파일 목록을 가져온다.
	 * @param srcRootDir 적용할 패치 파일들이 있는 최상위 디렉터리 경로
	 */
	public static Set<String> getPatchFilePathList(String srcRootDir) {
		Set<String> patchFilePathList = new HashSet<String>();
		File rootDir = new File(srcRootDir);
		if(!rootDir.exists()) {
			return patchFilePathList;
		}
		Collection<File> patchFileList = FileUtils.listFiles(
				rootDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);

		int beginIndex = rootDir.getAbsolutePath().length()+1;

		for(File file : patchFileList){
			String path = file.getAbsolutePath();
			path = path.substring(beginIndex);
			patchFilePathList.add(path); 
		}
		
		return patchFilePathList;
	}
	
	/**
	 * 패치 유틸리티 Runnable jar 안의 plugins 디렉터리에 있는 
	 * 파일 및 디렉터리 경로 리스트를 불러온다.
	 * @return List<String>
	 */
	public static Set<String> getPatchFilePathList(JarFile jarFile){
		Set<String> pathFilePathList = new HashSet<String>();
		List<JarEntry> jarEntryList = loadPatchFileEntryList(jarFile);

		int beginIndex = Const.PLUGINS_DIR_NAME.length() + 1;

		for(JarEntry entry : jarEntryList){
			String entryFilePath = entry.getName();

			entryFilePath = entryFilePath.substring(beginIndex).trim();
			if(!entryFilePath.isEmpty()){
				pathFilePathList.add(entryFilePath);
			}
		}
		return pathFilePathList;
	}

	/**
	 * 패치 유틸리티 Runnable jar 파일 안의 plugins 디렉터리에 있는<br> 
	 * 파일과 디렉터리 목록을 추출하기 위해 JarEntry 리스트를 반환한다.
	 * 
	 * @see JarFile
	 * @see JarFile#entries()
	 * @see JarEntry#getName()
	 * @return an List of String with the matching files Path
	 */
	private static List<JarEntry> loadPatchFileEntryList(JarFile jarFile){

		List<JarEntry> entryList = new ArrayList<JarEntry>();

		//gives ALL entries in jar
		Enumeration<JarEntry> entries = jarFile.entries(); 

		while(entries.hasMoreElements()) {
			JarEntry jarEntry = entries.nextElement();
			String entryFilePath = jarEntry.getName();
			
			if(entryFilePath.startsWith(Const.PLUGINS_DIR_NAME)){
				if(!jarEntry.isDirectory()){
					entryList.add(jarEntry);
				}
			}
		}
		return entryList;
	}

	/**
	 * 패치 유틸리티 Runnable jar 파일의 plugins 디렉터리에 있는 파일 중
	 * fileName과 일치하는 파일의 inputStream을 반환한다.
	 * @param fileName
	 * @return fileName과 일치하는 파일이 있으면 InputStream을 반환하고,
	 * 없으면 null을 반환한다.
	 */
	public static InputStream getInputStream(String fileName, JarFile runnableJar){

		Enumeration<JarEntry> entries = runnableJar.entries();

		while(entries.hasMoreElements()) {

			JarEntry jarEntry = entries.nextElement();
			String patchPath = jarEntry.getName();

			if(patchPath.startsWith(Const.PLUGINS_DIR_NAME) && patchPath.endsWith(fileName)){

				try {
					return runnableJar.getInputStream(jarEntry);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	
	public static InputStream getInputStream(String fileName, String externalPath) {

		String extension = "info"; //$NON-NLS-1$
		Collection<File> listFiles = 
				FileUtils.listFiles(new File(externalPath), new String[] { extension }, false);

		for(File file : listFiles) {
			if(file.getName().endsWith(extension)) {
				try {
					return new FileInputStream(file);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	/**
	 * 지정된 디렉터리경로에 패치할 파일들이 있는 확인한다.
	 * @param directory
	 * @return 패치파일이 있으면 true, 없으면 false를 반환한다.
	 */
	public static boolean existPatchfiles(File directory){
		if(!directory.exists()) {
			return false;
		}
		return !FileUtils.listFiles(directory, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE).isEmpty();
	}

	/**
	 * jar파일에 패치할 파일들이 있는지 확인한다.
	 * @return 패치파일이 있으면 true, 없으면 false를 반환한다.
	 */
	public static boolean existPatchFiles(JarFile jarFile){
		Enumeration<JarEntry> jarEntryList = jarFile.entries();
		int beginIndex = Const.PLUGINS_DIR_NAME.length() + 1;

		while(jarEntryList.hasMoreElements()){

			JarEntry entry = jarEntryList.nextElement();
			String entryFilePath = entry.getName();

			if(entryFilePath.startsWith(Const.PLUGINS_DIR_NAME)){

				int endIndex = entryFilePath.length(); 
				entryFilePath = entryFilePath.substring(beginIndex, endIndex).trim();

				if(!entryFilePath.isEmpty()){
					return true;
				}
			}
		}
		return false;
	}

}
