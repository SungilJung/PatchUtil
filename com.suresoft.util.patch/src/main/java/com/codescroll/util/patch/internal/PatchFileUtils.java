package com.codescroll.util.patch.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;

import com.codescroll.util.patch.Const;
import com.codescroll.util.patch.model.CSProduct;
import com.codescroll.util.patch.model.PatchInfo;

/**
 * 패치 유틸리티에서 사용되는 파일관련 유틸
 * @author Jung
 *
 */
public class PatchFileUtils{

	private static final String UTF8_BOM = "\uFEFF";
	private static final String INSTALL_PATH_SEPARATOR = "=";
	private static final String CODE_INSPECTOR = "CI";
	private static final String CONTROLLER_TESTER = "CT";
	private static final String SNIPER = "SNIPER";
	private static final String DEPENDENCY_ANALYZER = "DA";
	private static final String DATE_REGEX = 
			"^(19|20)\\d{2}"
					+ "(/|\\.|-)"
					+ "((0[1-9]|1[012])|([1-9]|1[012]))"
					+ "(/|\\.|-)"
					+ "([1-9]|[12][0-9]|3[0-1]|0[1-9]|[12][0-9]|3[0-1])$";

	/**
	 * 패치 유틸리티 jar 파일을 불러온다.<br>
	 * 
	 * @return  JarFile
	 */
	public static JarFile getJarFile(){
		//strip out only the JAR file
		JarFile jar = null;

		System.out.println("img path: " + PatchFileUtils.class.getResource("/" + Const.IMG_DIR_NAME)); //$NON-NLS-1$
		try {
			String imgPath = PatchFileUtils.class.getResource("/" + Const.IMG_DIR_NAME).getPath();
			String jarPath = getJarPath(imgPath);
			//XXX 파일 diff 시 맞지 않는경우 UTF-8의 문제일 확률이 높음
			jar = new JarFile(URLDecoder.decode(jarPath, Const.UTF_8));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		return jar;
	}

	private static String getJarPath(String imgPath){
		String jarPath = "";
		if(Const.DEBUG){
			jarPath = imgPath.substring(0,imgPath.lastIndexOf("/")) + "/" + Const.PLUGINS_DIR_NAME + "/test.jar"; //$NON-NLS-1$ //$NON-NLS-1$ //$NON-NLS-1$
		}else{
			jarPath = imgPath.substring(5, imgPath.indexOf("!")); //$NON-NLS-1$
		}

		return jarPath;
	}

	/**
	 *
	 * 패치할 파일들을 srcRootDir에서 복사 후, destRootDir 경로에 붙여넣기를 수행한다.
	 * 
	 * @see FileUtils#copyFile(File, File)
	 * @param srcRootDir 복사할 대상 최상위 디렉터리
	 * @param destRootDir 복사될 대상 최상위 디렉터리
	 * @param patchFileList 패치할 파일들의 목록 리스트
	 * @param progressMonitor 프로그래스 다이얼로그에 사용되는 progressMonitor <br> null인 경우에도 사용 가능
	 * 
	 * @exception IOException 복사에 실패할 경우 발생
	 */
	public static void copyFilesAndDirs(String srcRootDir, String destRootDir,
			Collection<String> patchFileList, IProgressMonitor progressMonitor) throws IOException{
		
		File destDir = new File(destRootDir);
		
		if(!destDir.exists()){
			destDir.mkdirs();
		}
		
		for(String patchFilePath : patchFileList){

			String srcFilePath = srcRootDir + File.separator + patchFilePath;
			String destFilePath = destRootDir + File.separator + patchFilePath; 

			File srcFile = new File(srcFilePath);
			File destFile = new File(destFilePath);

			if(!srcFile.exists() || srcFile.isDirectory()){
				continue;
			}

			FileUtils.copyFile(srcFile, destFile);

			if(progressMonitor != null){
				progressMonitor.subTask(destFilePath);
				progressMonitor.worked(1);
			}
		}
	}

	/**
	 * INI 파일에 제품 설치 경로를 저장한다.
	 * @param path 제품설치 경로
	 */
	public static void saveInstallPath(CSProduct product){
		String key = createInstallPathKey(product.getName(), product.getVersion());
		createINIFile();
		String contents = getInstallPaths(key, product.getPath());
		try {
			FileUtils.write(new File(Const.INI_FILE_PATH), contents, Const.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 비어있는 INI파일을 생성한다.
	 */
	private static void createINIFile(){
		createINIDir();
		File iniFile = new File(Const.INI_FILE_PATH);
		try {
			iniFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * INI 파일이 저장될 디렉터리를 생성한다.
	 */
	private static void createINIDir(){
		File iniDir = new File(Const.INI_FILE_DIR);
		iniDir.mkdirs();
	}


	/**
	 * 제품 경로를 INI 파일에 저장하기위해 key를 생성한다.
	 * @param productName
	 * @return
	 */
	private static String createInstallPathKey(String productName, String productVersion) {
		String key ="";
		
		if(productName.contains(Const.CSCI_NAME)){
			key = Const.CSCI_NAME + "_" + productVersion;
		}else if(productName.contains(Const.CSUT_NAME)){
			key = Const.CSUT_NAME + "_" + productVersion;
		}else if(productName.contains(Const.CSDA_NAME)){
			key = Const.CSDA_NAME + "_" + productVersion;
		}else if(productName.contains(Const.CSRTE_NAME)){
			key = Const.CSRTE_NAME + "_" + productVersion;
		}
		
		return key;
	}

	/**
	 * 신규 제품 경로를 포함한 설치경로들을 반환한다.
	 * @param key 제품 명
	 * @param path 제품 설치 경로
	 * @return 기존 설치경로가 없으면 신규 설치경로 정보만 반환하고,<br>
	 * 기존 설치경로가 있으면 기존 설치경로와 신규 설치 경로를 반환한다.
	 */
	private static String getInstallPaths(String key, String path) {
		Map<String, String> installPathMap = getInstallPathMap();
		installPathMap.put(key, path);
		StringBuilder contents = new StringBuilder();
		for(Map.Entry<String, String> element : installPathMap.entrySet()){
			contents.append(element.getKey() + INSTALL_PATH_SEPARATOR + element.getValue() + Const.NEW_LINE);
		}
		return contents.toString();
	}

	/**
	 * INI 파일에 저장되어 있는 설치경로를 가져온다.
	 * @return 저장되어 있는 설치경로가 있으면 Map<제품명_제품버전,설치경로>를 반환하고,<br>
	 * 없으면 비어있는 Map을 반환한다.
	 */
	public static Map<String, String> getInstallPathMap(){

		List<String> installPathList = new ArrayList<String>();
		Map<String, String> installPathMap = new HashMap<String, String>();

		try {
			installPathList = FileUtils.readLines(new File(Const.INI_FILE_PATH), Const.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
			return installPathMap;
		}

		for(String installPath : installPathList){
			if(installPath.isEmpty()) {
				continue;
			}
			String[] data = installPath.split(INSTALL_PATH_SEPARATOR);
			installPathMap.put(data[0], data[1]);
		}

		return installPathMap;
	}
	
	/**
	 * INI 파일에 저장되어 있는 외부 plugins 경로를 반환한다.<p>
	 * 
	 * INI 파일에 'plugins_path'으로 패치가 존재하는 디렉터리 경로 입력 시,<br> 
	 * 패치를 적용하는데 사용하는 외부 plugins 경로
	 * 
	 * @return 경로가 없으면 null 
	 */
	public static String getExternalPatchDirPath() {

		List<String> pathList = new ArrayList<String>();

		try {
			pathList = FileUtils.readLines(new File(Const.INI_FILE_PATH), Const.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		for(String path : pathList) {
			String[] data = path.split(INSTALL_PATH_SEPARATOR);
			if(data[0].equals(Const.EXTERNAL_PATCH_DIR)) {
				return data[1];
			}
		}

		return null;
	}

	/**
	 * 패치 파일들을 삭제한다.
	 * @param destDir 삭제할 패치 파일들이 존재하는 최상위 디렉터리
	 * @param patchFilePathList 상대경로 리스트
	 * @return 삭제 성공시 true, 실패시 false;
	 */
	public static boolean deletePatchFiles(String destDir, Set<String>patchFilePathList){
		boolean isSuccess = true;

		for(String path : patchFilePathList){
			String destFilePath = destDir + File.separator + path;
			isSuccess = new File(destFilePath).delete();
			if(!isSuccess){
				return isSuccess;
			}
		}

		return isSuccess;
	}


	/**
	 * patch.info 파일의 내용을 가져온다.
	 * @param patch.info의 InputStream
	 * @param latestPatchedVersion 적용된 패치의 마지막 버전
	 * 
	 */
	public static PatchInfo loadPatchInfo(InputStream inputStream, String latestPatchedVersion){

		List<String> patchInfoContents = new ArrayList<String>();
		String productName = "";
		String productVersion = "";
		int nameIndex = 0;
		int versionIndex = 1;

		if(inputStream == null){
			return null;
		}

		String inputLine = null;
		boolean isProductDataLine = true;
		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Const.UTF_8));
			try {
				while((inputLine = bufferedReader.readLine()) != null){

					String contents = inputLine.replace(UTF8_BOM, "").trim();
					

					if(isProductDataLine){
						String[] productData = createProductData(contents);
						productVersion = productData[versionIndex];
						productName = createProductName(productData[nameIndex]);
						isProductDataLine = !isProductDataLine;
						continue;
					}

					if(contents.isEmpty() || contents.matches(DATE_REGEX)){
						continue;
					}
					
					patchInfoContents.add(contents);
				}

				PatchInfo patchInfo = createPatchInfoData(latestPatchedVersion, productName,
						productVersion, patchInfoContents);

				return patchInfo;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static String[] createProductData(String contents) {
		return contents.split(Const.PACKAGE_NAME_DELIM);
	}

	/**
	 * patch.Info 파일에 입력되어있는 제품명을 사용자에게 제공할 형식으로 가공한다. 
	 * @param productName
	 */
	private static String createProductName(String productName){
		if(productName.equalsIgnoreCase(CODE_INSPECTOR)){
			return Const.CSCI_NAME;
		}else if(productName.equalsIgnoreCase(CONTROLLER_TESTER)){
			return Const.CSUT_NAME;
		}else if(productName.equalsIgnoreCase(DEPENDENCY_ANALYZER)){
			return Const.CSDA_NAME;
		}else if(productName.equalsIgnoreCase(SNIPER)){
			return Const.CSRTE_NAME;
		}else{
			return "";
		}
	}

	/**
	 * 적용할 패치 정보 갖고 있는 PatchInfo를 반환한다.
	 * @param latestPatchedVersion 적용된패치 중 가장 최신의 패치버전
	 * @return 적용된 최신의 패치 정보보다 상위의 패치 정보를 갖고 있는 PatchInfo
	 */
	private static PatchInfo createPatchInfoData(String latestPatchedVersion, String productName,
			String productVersion, List<String> patchInfoContents){
		List<String> contentsList = new ArrayList<String>();
		String patchVersion ="";
		String tempPatchVersion ="";

		int prePatchVersion;
		int currentPatchVersion; 
		boolean isLatestPatchVersion =  true;
		
		for(String lineContents : patchInfoContents){

			if(isPatchVersion(lineContents)){
				tempPatchVersion = createPatchVersion(productVersion, lineContents);
				if(isLatestPatchVersion){
					patchVersion = tempPatchVersion;
					isLatestPatchVersion = !isLatestPatchVersion;
				}

			}else{
				contentsList.add(lineContents);
				
			}
			
			if(!latestPatchedVersion.isEmpty()){
				prePatchVersion = Integer.parseInt(latestPatchedVersion.substring(latestPatchedVersion.lastIndexOf(".")+1));
			}else{
				prePatchVersion = 0;
			}
			
			currentPatchVersion = Integer.parseInt(tempPatchVersion.substring(tempPatchVersion.lastIndexOf(".")+1));
			
			if(currentPatchVersion <= prePatchVersion){
				break;
			}
		}
		return new PatchInfo(productName, productVersion, patchVersion, contentsList);
	}


	private static boolean isPatchVersion(String lineContents) {
		return lineContents.startsWith("v") || lineContents.startsWith("V"); //$NON-NLS-1$ //$NON-NLS-1$
	}

	/**
	 * patch.Info 파일에 입력되어있는 패치 버전을 사용자에게 제공할 형식으로 가공한다. 
	 * @param contents
	 * @return
	 */
	private static String createPatchVersion(String productVersion, String patchVersion) {
		return  productVersion + "." + patchVersion.substring(1); //$NON-NLS-1$
	}
	
	/**
	 * 제품의 버전(minor 까지)을 반환한다.
	 * @param productVersion
	 * @return 입력된 제품 버전의 minor 버전까지만 잘라서 반환한다.
	 */
	public static String getProductMinorVersion(String productVersion){
		return productVersion.substring(0, productVersion.lastIndexOf("."));
	}

}
